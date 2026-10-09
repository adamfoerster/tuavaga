-- Fase 6 · Perfil (prancha 24): veículos, condomínios e exclusão da conta.
-- Regras: um veículo usado por reserva ativa não pode ser removido; sair de um condomínio com reserva
-- ativa (como locatário ou locador) é bloqueado e pausa as vagas do usuário ali; excluir a conta
-- cancela as reservas futuras (a outra parte é avisada) e apaga tudo do usuário.

-- Avisos sobrevivem à reserva apagada junto com a conta da outra parte.
do $$ begin
    alter table public.notifications drop constraint if exists notifications_booking_id_fkey;
    alter table public.notifications add constraint notifications_booking_id_fkey
        foreign key (booking_id) references public.bookings (id) on delete set null;
end $$;

-- ---------------------------------------------------------------------------------------------
-- Veículos
-- ---------------------------------------------------------------------------------------------
create or replace function public.vehicle_guard()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if exists (
        select 1 from public.bookings b
        where b.vehicle_id = old.id and b.status in ('pending', 'confirmed', 'in_progress')
    ) then
        raise exception 'vehicle_in_use' using errcode = 'P0001';
    end if;
    return old;
end;
$$;

drop trigger if exists vehicles_guard on public.vehicles;
create trigger vehicles_guard
    before delete on public.vehicles
    for each row execute function public.vehicle_guard();

-- ---------------------------------------------------------------------------------------------
-- Sair do condomínio
-- ---------------------------------------------------------------------------------------------
create or replace function public.leave_condominium(p_condo uuid)
returns void
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
    if exists (
        select 1 from public.bookings b
        where b.condo_id = p_condo and uid in (b.renter_id, b.owner_id)
          and b.status in ('pending', 'confirmed', 'in_progress')
    ) then
        raise exception 'active_bookings' using errcode = 'P0001';
    end if;
    -- Spots stay (history), but nobody can book them any more.
    update public.spots set status = 'paused' where condo_id = p_condo and owner_id = uid;
    delete from public.memberships where condo_id = p_condo and user_id = uid;
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- Excluir a conta
-- ---------------------------------------------------------------------------------------------
create or replace function public.delete_own_account()
returns void
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
    -- Someone is parked: finish it first (check-out), the account cannot vanish under them.
    if exists (
        select 1 from public.bookings b
        where uid in (b.renter_id, b.owner_id) and b.status = 'in_progress'
    ) then
        raise exception 'booking_in_progress' using errcode = 'P0001';
    end if;
    -- Future bookings are cancelled first, so the other party gets the notification.
    update public.bookings
    set status = 'cancelled', cancelled_by = uid, cancelled_at = now(), respond_by = null
    where uid in (renter_id, owner_id) and status in ('pending', 'confirmed');

    delete from public.bookings where uid in (renter_id, owner_id);
    delete from auth.users where id = uid;
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- Permissões
-- ---------------------------------------------------------------------------------------------
revoke execute on function public.vehicle_guard() from public, anon, authenticated;
revoke execute on function public.leave_condominium(uuid) from public, anon;
revoke execute on function public.delete_own_account() from public, anon;
grant execute on function public.leave_condominium(uuid) to authenticated;
grant execute on function public.delete_own_account() to authenticated;
