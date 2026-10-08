-- Fase 4 · Ciclo da reserva (pranchas 10, 12, 17, 18, 24, 25 e 28).
-- Toda mudança de estado passa por uma RPC que confere o papel (locatário ou locador) e a transição.
-- Pedidos vencidos e reservas encerradas são acertados por settle_bookings(), chamada no início de
-- cada RPC daqui (e, opcionalmente, pelo pg_cron — ver supabase/README.md).

-- ---------------------------------------------------------------------------------------------
-- Colunas
-- ---------------------------------------------------------------------------------------------
alter table public.bookings add column if not exists cancelled_by   uuid references auth.users (id) on delete set null;
alter table public.bookings add column if not exists cancelled_at   timestamptz;
alter table public.bookings add column if not exists checked_in_at  timestamptz;
alter table public.bookings add column if not exists checked_out_at timestamptz;

-- ---------------------------------------------------------------------------------------------
-- Acerto automático
-- ---------------------------------------------------------------------------------------------

-- Pendentes sem resposta no prazo (ou cuja entrada já passou) expiram; confirmadas cuja saída
-- passou sem check-in terminam; em curso terminam 12 h depois da saída sem check-out.
create or replace function public.settle_bookings()
returns void
language sql
security definer
set search_path = ''
as $$
    update public.bookings set status = 'expired'
    where status = 'pending' and (respond_by < now() or starts_at < now());

    update public.bookings set status = 'completed'
    where status = 'confirmed' and ends_at < now();

    update public.bookings set status = 'completed', checked_out_at = ends_at
    where status = 'in_progress' and ends_at + interval '12 hours' < now();
$$;

-- ---------------------------------------------------------------------------------------------
-- Leitura
-- ---------------------------------------------------------------------------------------------

-- Reservas do usuário, como locatário (aba Reservas) e como locador (solicitações, agenda, ganhos),
-- com o que as telas mostram. A contraparte aparece só pelo nome, bloco e unidade.
-- conflict_*: para pedidos pendentes, a primeira reserva confirmada que se sobrepõe (prancha 17).
drop function if exists public.my_bookings();
create function public.my_bookings()
returns table (
    id                  uuid,
    code                bigint,
    role                text,
    status              public.booking_status,
    condo_id            uuid,
    condo_name          text,
    spot_id             uuid,
    level_name          text,
    sector_name         text,
    spot_number         text,
    directions          text,
    rules               text[],
    cancel_notice_hours int,
    starts_at           timestamptz,
    ends_at             timestamptz,
    billing_unit        public.billing_unit,
    units               int,
    unit_price_cents    int,
    total_cents         int,
    note                text,
    reject_reason       public.reject_reason,
    reject_message      text,
    respond_by          timestamptz,
    cancelled_by_owner  boolean,
    checked_in_at       timestamptz,
    checked_out_at      timestamptz,
    counterpart_name    text,
    counterpart_block   text,
    counterpart_unit    text,
    vehicle_plate       text,
    vehicle_model       text,
    vehicle_color       text,
    vehicle_type        text,
    conflict_starts_at  timestamptz,
    conflict_ends_at    timestamptz
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    uid uuid := (select auth.uid());
begin
    if uid is null then
        raise exception 'not authenticated' using errcode = '28000';
    end if;
    perform public.settle_bookings();
    return query
    select b.id, b.code,
           case when b.renter_id = uid then 'renter' else 'owner' end,
           b.status, b.condo_id, c.name, b.spot_id, l.name, sec.name, s.number, s.directions, s.rules,
           s.cancel_notice_hours, b.starts_at, b.ends_at, b.billing_unit, b.units, b.unit_price_cents,
           b.total_cents, b.note, b.reject_reason, b.reject_message, b.respond_by,
           case when b.cancelled_by is null then null else b.cancelled_by = b.owner_id end,
           b.checked_in_at, b.checked_out_at,
           p.full_name, m.block, m.unit,
           v.plate, v.model, v.color, v.type::text,
           conflict.starts_at, conflict.ends_at
    from public.bookings b
    join public.condominiums c on c.id = b.condo_id
    join public.spots s on s.id = b.spot_id
    join public.condo_levels l on l.id = s.level_id
    left join public.condo_sectors sec on sec.id = s.sector_id
    left join public.profiles p
        on p.id = case when b.renter_id = uid then b.owner_id else b.renter_id end
    left join public.memberships m
        on m.condo_id = b.condo_id and m.user_id = case when b.renter_id = uid then b.owner_id else b.renter_id end
    left join public.vehicles v on v.id = b.vehicle_id
    left join lateral (
        select o.starts_at, o.ends_at
        from public.bookings o
        where b.status = 'pending'
          and o.spot_id = b.spot_id
          and o.id <> b.id
          and o.status in ('confirmed', 'in_progress')
          and o.period && b.period
        order by o.starts_at
        limit 1
    ) conflict on true
    where uid in (b.renter_id, b.owner_id)
    order by b.starts_at;
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- Transições
-- ---------------------------------------------------------------------------------------------

-- Locks the booking and checks who is acting. Errors: booking_not_found (also when the user is
-- not the expected party).
create or replace function public.booking_for_action(p_booking uuid, p_role text)
returns public.bookings
language plpgsql
security definer
set search_path = ''
as $$
declare
    uid uuid := (select auth.uid());
    b public.bookings%rowtype;
begin
    if uid is null then
        raise exception 'not authenticated' using errcode = '28000';
    end if;
    perform public.settle_bookings();
    select * into b from public.bookings where id = p_booking for update;
    if not found
       or (p_role = 'renter' and b.renter_id <> uid)
       or (p_role = 'owner' and b.owner_id <> uid)
       or (p_role = 'any' and uid not in (b.renter_id, b.owner_id)) then
        raise exception 'booking_not_found' using errcode = 'P0001';
    end if;
    return b;
end;
$$;

-- Locador aceita um pedido. Erros: invalid_state (não está pendente ou venceu), conflict (já há
-- reserva confirmada no período).
create or replace function public.approve_booking(p_booking uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    b public.bookings%rowtype := public.booking_for_action(p_booking, 'owner');
begin
    if b.status <> 'pending' then
        raise exception 'invalid_state' using errcode = 'P0001';
    end if;
    begin
        update public.bookings set status = 'confirmed', respond_by = null where id = b.id;
    exception when exclusion_violation then
        raise exception 'conflict' using errcode = 'P0001';
    end;
end;
$$;

-- Locador recusa um pedido, com motivo e mensagem opcional.
create or replace function public.reject_booking(p_booking uuid, p_reason public.reject_reason, p_message text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    b public.bookings%rowtype := public.booking_for_action(p_booking, 'owner');
begin
    if b.status <> 'pending' then
        raise exception 'invalid_state' using errcode = 'P0001';
    end if;
    if char_length(p_message) > 280 then
        raise exception 'invalid_message' using errcode = '22023';
    end if;
    update public.bookings
    set status = 'rejected', reject_reason = p_reason, reject_message = nullif(btrim(p_message), ''), respond_by = null
    where id = b.id;
end;
$$;

-- Cancelamento. Locatário: pedido pendente a qualquer momento; reserva confirmada só até
-- cancel_notice_hours antes da entrada (depois, cancel_window_closed — combine pelo chat).
-- Locador: reserva confirmada antes da entrada (pedido pendente se recusa, não se cancela).
create or replace function public.cancel_booking(p_booking uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    uid uuid := (select auth.uid());
    b public.bookings%rowtype := public.booking_for_action(p_booking, 'any');
    notice int;
begin
    if b.renter_id = uid then
        if b.status = 'confirmed' then
            select s.cancel_notice_hours into notice from public.spots s where s.id = b.spot_id;
            if now() > b.starts_at - make_interval(hours => notice) then
                raise exception 'cancel_window_closed' using errcode = 'P0001';
            end if;
        elsif b.status <> 'pending' then
            raise exception 'invalid_state' using errcode = 'P0001';
        end if;
    else
        if b.status <> 'confirmed' or now() >= b.starts_at then
            raise exception 'invalid_state' using errcode = 'P0001';
        end if;
    end if;
    update public.bookings
    set status = 'cancelled', cancelled_by = uid, cancelled_at = now(), respond_by = null
    where id = b.id;
end;
$$;

-- Check-in do locatário: reserva confirmada, de 30 min antes da entrada até a saída.
create or replace function public.check_in(p_booking uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    b public.bookings%rowtype := public.booking_for_action(p_booking, 'renter');
begin
    if b.status <> 'confirmed' then
        raise exception 'invalid_state' using errcode = 'P0001';
    end if;
    if now() < b.starts_at - interval '30 minutes' or now() >= b.ends_at then
        raise exception 'check_in_closed' using errcode = 'P0001';
    end if;
    update public.bookings set status = 'in_progress', checked_in_at = now() where id = b.id;
end;
$$;

-- Check-out do locatário.
create or replace function public.check_out(p_booking uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    b public.bookings%rowtype := public.booking_for_action(p_booking, 'renter');
begin
    if b.status <> 'in_progress' then
        raise exception 'invalid_state' using errcode = 'P0001';
    end if;
    update public.bookings set status = 'completed', checked_out_at = now() where id = b.id;
end;
$$;

-- "Preciso de mais tempo" / "Ajustar horário": o locatário estende a saída de uma reserva confirmada
-- ou em curso, se o trecho a mais estiver dentro da disponibilidade e livre. O valor é recalculado
-- na mesma forma de cobrança. Erros: invalid_state, invalid_period, spot_unavailable.
create or replace function public.extend_booking(p_booking uuid, p_new_end timestamptz)
returns table (ends_at timestamptz, units int, total_cents int)
language plpgsql
security definer
set search_path = ''
as $$
declare
    b public.bookings%rowtype := public.booking_for_action(p_booking, 'renter');
    n int;
begin
    if b.status not in ('confirmed', 'in_progress') then
        raise exception 'invalid_state' using errcode = 'P0001';
    end if;
    if p_new_end <= b.ends_at or p_new_end > b.ends_at + interval '7 days' then
        raise exception 'invalid_period' using errcode = 'P0001';
    end if;
    if not public.spot_is_open(b.spot_id, b.ends_at, p_new_end)
       or public.spot_is_busy(b.spot_id, b.ends_at, p_new_end) then
        raise exception 'spot_unavailable' using errcode = 'P0001';
    end if;
    n := public.billing_units(b.starts_at, p_new_end, b.billing_unit);
    begin
        return query
        update public.bookings as u
        set ends_at = p_new_end, units = n, total_cents = n * b.unit_price_cents
        where u.id = b.id
        returning u.ends_at, u.units, u.total_cents;
    exception when exclusion_violation then
        raise exception 'spot_unavailable' using errcode = 'P0001';
    end;
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- Permissões
-- ---------------------------------------------------------------------------------------------
revoke execute on function public.settle_bookings() from public, anon, authenticated;
revoke execute on function public.booking_for_action(uuid, text) from public, anon, authenticated;
revoke execute on function public.my_bookings() from public, anon;
revoke execute on function public.approve_booking(uuid) from public, anon;
revoke execute on function public.reject_booking(uuid, public.reject_reason, text) from public, anon;
revoke execute on function public.cancel_booking(uuid) from public, anon;
revoke execute on function public.check_in(uuid) from public, anon;
revoke execute on function public.check_out(uuid) from public, anon;
revoke execute on function public.extend_booking(uuid, timestamptz) from public, anon;
grant execute on function public.my_bookings() to authenticated;
grant execute on function public.approve_booking(uuid) to authenticated;
grant execute on function public.reject_booking(uuid, public.reject_reason, text) to authenticated;
grant execute on function public.cancel_booking(uuid) to authenticated;
grant execute on function public.check_in(uuid) to authenticated;
grant execute on function public.check_out(uuid) to authenticated;
grant execute on function public.extend_booking(uuid, timestamptz) to authenticated;
