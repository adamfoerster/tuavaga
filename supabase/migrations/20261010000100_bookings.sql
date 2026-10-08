-- Fase 3 · Explorar vagas livres num período e pedir reserva (pranchas 04 a 09).
-- Regras: a reserva só nasce por request_booking, que confere vínculo, veículo, período mínimo,
-- disponibilidade (janela semanal / exceções no fuso do condomínio) e sobreposição. Reservas
-- confirmadas ou em curso não podem se sobrepor na mesma vaga (exclusion constraint).

create extension if not exists btree_gist with schema extensions;

-- ---------------------------------------------------------------------------------------------
-- Tipos
-- ---------------------------------------------------------------------------------------------
do $$ begin
    create type public.billing_unit as enum ('hour', 'day', 'week');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.booking_status as enum
        ('pending', 'confirmed', 'rejected', 'cancelled', 'expired', 'in_progress', 'completed');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.reject_reason as enum ('visita', 'uso', 'veiculo', 'outro');
exception when duplicate_object then null; end $$;

-- ---------------------------------------------------------------------------------------------
-- Tabela
-- ---------------------------------------------------------------------------------------------
create table if not exists public.bookings (
    id                uuid primary key default gen_random_uuid(),
    -- Número curto mostrado ao usuário ("Reserva 4821").
    code              bigint generated always as identity (start with 1001) unique,
    spot_id           uuid not null references public.spots (id) on delete restrict,
    condo_id          uuid not null references public.condominiums (id) on delete cascade,
    owner_id          uuid not null references auth.users (id) on delete cascade,
    renter_id         uuid not null references auth.users (id) on delete cascade,
    vehicle_id        uuid references public.vehicles (id) on delete set null,
    starts_at         timestamptz not null,
    ends_at           timestamptz not null,
    period            tstzrange generated always as (tstzrange(starts_at, ends_at, '[)')) stored,
    billing_unit      public.billing_unit not null,
    units             int not null check (units > 0),
    unit_price_cents  int not null check (unit_price_cents > 0),
    total_cents       int not null check (total_cents > 0),
    note              text check (char_length(note) <= 280),
    status            public.booking_status not null default 'pending',
    reject_reason     public.reject_reason,
    reject_message    text check (char_length(reject_message) <= 280),
    -- Prazo para o locador responder um pedido pendente.
    respond_by        timestamptz,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now(),
    check (starts_at < ends_at),
    check (owner_id <> renter_id)
);
create index if not exists bookings_renter_idx on public.bookings (renter_id, starts_at);
create index if not exists bookings_owner_idx on public.bookings (owner_id, starts_at);

do $$ begin
    alter table public.bookings add constraint bookings_no_overlap
        exclude using gist (spot_id with =, period with &&) where (status in ('confirmed', 'in_progress'));
-- The constraint is backed by an index, so a second run raises duplicate_table.
exception when duplicate_object or duplicate_table then null; end $$;

drop trigger if exists bookings_touch_updated_at on public.bookings;
create trigger bookings_touch_updated_at
    before update on public.bookings
    for each row execute function public.touch_updated_at();

-- ---------------------------------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------------------------------
alter table public.bookings enable row level security;

drop policy if exists "Locatário e locador leem a reserva" on public.bookings;
create policy "Locatário e locador leem a reserva"
    on public.bookings for select to authenticated
    using ((select auth.uid()) in (renter_id, owner_id));

-- O locador vê o veículo de quem pediu a vaga dele (prancha 16).
drop policy if exists "Locador vê o veículo da reserva" on public.vehicles;
create policy "Locador vê o veículo da reserva"
    on public.vehicles for select to authenticated
    using (exists (
        select 1 from public.bookings b
        where b.vehicle_id = vehicles.id and b.owner_id = (select auth.uid())
    ));

-- ---------------------------------------------------------------------------------------------
-- Regras de disponibilidade e preço (internas)
-- ---------------------------------------------------------------------------------------------

-- O período [p_start, p_end) cabe na disponibilidade da vaga? Cada dia tocado (no fuso do
-- condomínio) precisa estar aberto — regra semanal ou exceção "open" — e o trecho daquele dia
-- precisa caber na janela; um dia bloqueado reprova.
create or replace function public.spot_is_open(p_spot uuid, p_start timestamptz, p_end timestamptz)
returns boolean
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    tz text;
    local_start timestamp;
    local_end timestamp;
    cur_day date;
    seg_start timestamp;
    seg_end timestamp;
    w_start time;
    w_end time;
    o record;
begin
    select c.timezone into tz
    from public.spots s join public.condominiums c on c.id = s.condo_id
    where s.id = p_spot;
    if tz is null or p_start >= p_end then
        return false;
    end if;

    local_start := p_start at time zone tz;
    local_end := p_end at time zone tz;
    cur_day := local_start::date;
    while cur_day::timestamp < local_end loop
        seg_start := greatest(local_start, cur_day::timestamp);
        seg_end := least(local_end, cur_day::timestamp + interval '1 day');

        select d.kind, d.start_time, d.end_time into o
        from public.spot_date_overrides d where d.spot_id = p_spot and d.day = cur_day;
        if found then
            if o.kind = 'blocked' then
                return false;
            end if;
            w_start := o.start_time;
            w_end := o.end_time;
        else
            select w.start_time, w.end_time into w_start, w_end
            from public.spot_weekly_availability w
            where w.spot_id = p_spot and w.weekday = extract(isodow from cur_day)::int;
            if not found then
                return false;
            end if;
        end if;

        -- "24:00" is stored as time '24:00:00'; compare as offsets from midnight.
        if seg_start - cur_day::timestamp < w_start - time '00:00'
           or seg_end - cur_day::timestamp > w_end - time '00:00' then
            return false;
        end if;
        cur_day := cur_day + 1;
    end loop;
    return true;
end;
$$;

-- Já existe reserva confirmada ou em curso no período?
create or replace function public.spot_is_busy(p_spot uuid, p_start timestamptz, p_end timestamptz)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1 from public.bookings b
        where b.spot_id = p_spot
          and b.status in ('confirmed', 'in_progress')
          and b.period && tstzrange(p_start, p_end, '[)')
    );
$$;

-- Unidades cobradas: arredonda para cima (34 h em diárias = 2).
create or replace function public.billing_units(p_start timestamptz, p_end timestamptz, p_unit public.billing_unit)
returns int
language sql
immutable
set search_path = ''
as $$
    select ceil(extract(epoch from (p_end - p_start)) / case p_unit
        when 'hour' then 3600 when 'day' then 86400 else 604800 end)::int;
$$;

-- ---------------------------------------------------------------------------------------------
-- RPCs
-- ---------------------------------------------------------------------------------------------

-- Vagas ativas do condomínio com o estado no período (lista e mapa da garagem).
-- available = aberta, sem reserva confirmada e respeitando o período mínimo; is_mine = do usuário.
create or replace function public.search_spots(p_condo uuid, p_start timestamptz, p_end timestamptz)
returns table (
    id                  uuid,
    level_id            uuid,
    level_name          text,
    level_position      int,
    sector_name         text,
    number              text,
    size_label          text,
    description         text,
    features            text[],
    height_cm           int,
    directions          text,
    price_hour_cents    int,
    price_day_cents     int,
    price_week_cents    int,
    min_period_minutes  int,
    cancel_notice_hours int,
    approval            public.approval_mode,
    rules               text[],
    owner_name          text,
    owner_block         text,
    owner_unit          text,
    is_mine             boolean,
    available           boolean,
    weekly              jsonb
)
language sql
stable
security definer
set search_path = ''
as $$
    select s.id, s.level_id, l.name, l.position, sec.name, s.number, s.size_label, s.description,
           s.features, s.height_cm, s.directions, s.price_hour_cents, s.price_day_cents,
           s.price_week_cents, s.min_period_minutes, s.cancel_notice_hours, s.approval, s.rules,
           p.full_name, m.block, m.unit,
           s.owner_id = (select auth.uid()),
           extract(epoch from (p_end - p_start)) / 60 >= s.min_period_minutes
               and public.spot_is_open(s.id, p_start, p_end)
               and not public.spot_is_busy(s.id, p_start, p_end),
           coalesce((
               select jsonb_agg(jsonb_build_object(
                          'weekday', w.weekday,
                          'start', to_char(w.start_time, 'HH24:MI'),
                          'end', to_char(w.end_time, 'HH24:MI')) order by w.weekday)
               from public.spot_weekly_availability w where w.spot_id = s.id
           ), '[]'::jsonb)
    from public.spots s
    join public.condo_levels l on l.id = s.level_id
    left join public.condo_sectors sec on sec.id = s.sector_id
    left join public.profiles p on p.id = s.owner_id
    left join public.memberships m on m.user_id = s.owner_id and m.condo_id = s.condo_id
    where s.condo_id = p_condo
      and s.status = 'active'
      and public.is_condo_member(p_condo)
    order by l.position, sec.position nulls first, lpad(s.number, 6, '0');
$$;

-- Períodos ocupados de uma vaga (calendário do detalhe), sem expor quem reservou.
create or replace function public.spot_busy_ranges(p_spot uuid, p_from timestamptz, p_to timestamptz)
returns table (starts_at timestamptz, ends_at timestamptz)
language sql
stable
security definer
set search_path = ''
as $$
    select b.starts_at, b.ends_at
    from public.bookings b
    where b.spot_id = p_spot
      and b.status in ('confirmed', 'in_progress')
      and b.period && tstzrange(p_from, p_to, '[)')
      and public.can_read_spot(p_spot)
    order by b.starts_at;
$$;

-- Pede (ou, com aprovação automática, confirma) a reserva. Erros têm mensagem estável que o app
-- traduz: spot_unavailable, below_minimum, unit_not_offered, own_spot, invalid_vehicle, invalid_period.
create or replace function public.request_booking(
    p_spot     uuid,
    p_start    timestamptz,
    p_end      timestamptz,
    p_unit     public.billing_unit,
    p_vehicle  uuid,
    p_note     text
)
returns table (id uuid, code bigint, status public.booking_status, total_cents int)
language plpgsql
security definer
set search_path = ''
as $$
declare
    uid uuid := (select auth.uid());
    s public.spots%rowtype;
    unit_price int;
    n int;
    new_status public.booking_status;
begin
    if uid is null then
        raise exception 'not authenticated' using errcode = '28000';
    end if;
    select * into s from public.spots sp where sp.id = p_spot;
    if not found or s.status <> 'active' or not public.is_condo_member(s.condo_id) then
        raise exception 'spot_unavailable' using errcode = 'P0001';
    end if;
    if s.owner_id = uid then
        raise exception 'own_spot' using errcode = 'P0001';
    end if;
    if p_end <= p_start or p_start < now() - interval '15 minutes' then
        raise exception 'invalid_period' using errcode = 'P0001';
    end if;
    if p_vehicle is not null
       and not exists (select 1 from public.vehicles v where v.id = p_vehicle and v.owner_id = uid) then
        raise exception 'invalid_vehicle' using errcode = 'P0001';
    end if;
    if extract(epoch from (p_end - p_start)) / 60 < s.min_period_minutes then
        raise exception 'below_minimum' using errcode = 'P0001';
    end if;
    unit_price := case p_unit
        when 'hour' then s.price_hour_cents when 'day' then s.price_day_cents else s.price_week_cents end;
    if unit_price is null then
        raise exception 'unit_not_offered' using errcode = 'P0001';
    end if;
    if not public.spot_is_open(p_spot, p_start, p_end) or public.spot_is_busy(p_spot, p_start, p_end) then
        raise exception 'spot_unavailable' using errcode = 'P0001';
    end if;

    n := public.billing_units(p_start, p_end, p_unit);
    new_status := case s.approval when 'auto' then 'confirmed' else 'pending' end;

    begin
        return query
        insert into public.bookings as b (
            spot_id, condo_id, owner_id, renter_id, vehicle_id, starts_at, ends_at, billing_unit,
            units, unit_price_cents, total_cents, note, status, respond_by
        ) values (
            p_spot, s.condo_id, s.owner_id, uid, p_vehicle, p_start, p_end, p_unit,
            n, unit_price, n * unit_price, nullif(btrim(p_note), ''), new_status,
            case when new_status = 'pending' then now() + interval '12 hours' end
        )
        returning b.id, b.code, b.status, b.total_cents;
    exception when exclusion_violation then
        -- Another confirmation for the same period landed first.
        raise exception 'spot_unavailable' using errcode = 'P0001';
    end;
end;
$$;

revoke execute on function public.spot_is_open(uuid, timestamptz, timestamptz) from public, anon, authenticated;
revoke execute on function public.spot_is_busy(uuid, timestamptz, timestamptz) from public, anon, authenticated;
revoke execute on function public.search_spots(uuid, timestamptz, timestamptz) from public, anon;
revoke execute on function public.spot_busy_ranges(uuid, timestamptz, timestamptz) from public, anon;
revoke execute on function public.request_booking(uuid, timestamptz, timestamptz, public.billing_unit, uuid, text) from public, anon;
grant execute on function public.search_spots(uuid, timestamptz, timestamptz) to authenticated;
grant execute on function public.spot_busy_ranges(uuid, timestamptz, timestamptz) to authenticated;
grant execute on function public.request_booking(uuid, timestamptz, timestamptz, public.billing_unit, uuid, text) to authenticated;
