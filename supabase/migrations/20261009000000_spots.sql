-- Fase 2 · Vagas anunciadas pelos moradores e sua disponibilidade.
-- Regras: membros do condomínio leem as vagas ativas; o dono lê todas as suas. Escrita só pelas
-- funções save_spot / set_spot_status, que validam vínculo, andar e setor.

-- ---------------------------------------------------------------------------------------------
-- Tipos
-- ---------------------------------------------------------------------------------------------
do $$ begin
    create type public.spot_status as enum ('active', 'paused');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.approval_mode as enum ('manual', 'auto');
exception when duplicate_object then null; end $$;

-- ---------------------------------------------------------------------------------------------
-- Tabelas
-- ---------------------------------------------------------------------------------------------
create table if not exists public.spots (
    id                   uuid primary key default gen_random_uuid(),
    condo_id             uuid not null references public.condominiums (id) on delete cascade,
    owner_id             uuid not null default auth.uid() references auth.users (id) on delete cascade,
    level_id             uuid not null references public.condo_levels (id) on delete restrict,
    sector_id            uuid references public.condo_sectors (id) on delete restrict,
    -- Como está pintado no chão ("14", "B2-14").
    number               text not null check (char_length(btrim(number)) between 1 and 6),
    size_label           text check (char_length(size_label) <= 20),
    description          text check (char_length(description) <= 40),
    -- Preços em centavos; nulo = não oferece esse período. Pelo menos um é obrigatório.
    price_hour_cents     int check (price_hour_cents > 0),
    price_day_cents      int check (price_day_cents > 0),
    price_week_cents     int check (price_week_cents > 0),
    min_period_minutes   int not null default 120 check (min_period_minutes in (60, 120, 240, 1440)),
    cancel_notice_hours  int not null default 24 check (cancel_notice_hours in (2, 24, 48)),
    approval             public.approval_mode not null default 'manual',
    rules                text[] not null default '{}' check (cardinality(rules) <= 10),
    status               public.spot_status not null default 'active',
    created_at           timestamptz not null default now(),
    updated_at           timestamptz not null default now(),
    check (coalesce(price_hour_cents, price_day_cents, price_week_cents) is not null)
);
create index if not exists spots_condo_idx on public.spots (condo_id, status);
create index if not exists spots_owner_idx on public.spots (owner_id);
-- Um número por andar/setor (sem setor conta como um setor próprio).
create unique index if not exists spots_place_unique on public.spots
    (level_id, coalesce(sector_id, '00000000-0000-0000-0000-000000000000'::uuid), upper(btrim(number)));

-- Janela semanal: o mesmo horário para cada dia da semana marcado (1 = segunda … 7 = domingo).
create table if not exists public.spot_weekly_availability (
    spot_id     uuid not null references public.spots (id) on delete cascade,
    weekday     smallint not null check (weekday between 1 and 7),
    start_time  time not null,
    end_time    time not null,
    primary key (spot_id, weekday),
    check (start_time < end_time)
);

-- Exceções por data: liberar um dia fora da regra semanal ou bloquear um dia dela.
create table if not exists public.spot_date_overrides (
    spot_id     uuid not null references public.spots (id) on delete cascade,
    day         date not null,
    kind        text not null check (kind in ('open', 'blocked')),
    start_time  time,
    end_time    time,
    primary key (spot_id, day),
    check (kind = 'blocked' or (start_time is not null and end_time is not null and start_time < end_time))
);

drop trigger if exists spots_touch_updated_at on public.spots;
create trigger spots_touch_updated_at
    before update on public.spots
    for each row execute function public.touch_updated_at();

-- ---------------------------------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------------------------------
alter table public.spots enable row level security;
alter table public.spot_weekly_availability enable row level security;
alter table public.spot_date_overrides enable row level security;

drop policy if exists "Dono e membros leem vagas" on public.spots;
create policy "Dono e membros leem vagas"
    on public.spots for select to authenticated
    using (
        (select auth.uid()) = owner_id
        or (status = 'active' and public.is_condo_member(condo_id))
    );

-- security definer: evita recursão de RLS ao consultar spots dentro das policies abaixo.
create or replace function public.can_read_spot(p_spot uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1 from public.spots s
        where s.id = p_spot
          and (s.owner_id = (select auth.uid()) or (s.status = 'active' and public.is_condo_member(s.condo_id)))
    );
$$;

drop policy if exists "Quem lê a vaga lê a disponibilidade" on public.spot_weekly_availability;
create policy "Quem lê a vaga lê a disponibilidade"
    on public.spot_weekly_availability for select to authenticated
    using (public.can_read_spot(spot_id));

drop policy if exists "Quem lê a vaga lê as exceções" on public.spot_date_overrides;
create policy "Quem lê a vaga lê as exceções"
    on public.spot_date_overrides for select to authenticated
    using (public.can_read_spot(spot_id));

-- ---------------------------------------------------------------------------------------------
-- RPCs
-- ---------------------------------------------------------------------------------------------

-- Cria (p_spot_id nulo) ou atualiza uma vaga do usuário, substituindo a disponibilidade.
-- p_weekly:    [{"weekday": 1, "start": "08:00", "end": "18:00"}, ...]
-- p_overrides: [{"day": "2026-10-12", "kind": "blocked"},
--               {"day": "2026-10-17", "kind": "open", "start": "08:00", "end": "18:00"}, ...]
create or replace function public.save_spot(
    p_spot_id             uuid,
    p_condo               uuid,
    p_level               uuid,
    p_sector              uuid,
    p_number              text,
    p_size_label          text,
    p_description         text,
    p_price_hour_cents    int,
    p_price_day_cents     int,
    p_price_week_cents    int,
    p_min_period_minutes  int,
    p_cancel_notice_hours int,
    p_approval            public.approval_mode,
    p_rules               text[],
    p_weekly              jsonb,
    p_overrides           jsonb
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    uid uuid := (select auth.uid());
    saved_id uuid;
begin
    if uid is null then
        raise exception 'not authenticated' using errcode = '28000';
    end if;
    if not public.is_condo_member(p_condo) then
        raise exception 'not a member of this condominium' using errcode = '42501';
    end if;
    if not exists (select 1 from public.condo_levels l where l.id = p_level and l.condo_id = p_condo) then
        raise exception 'invalid level' using errcode = '22023';
    end if;
    if p_sector is not null
       and not exists (select 1 from public.condo_sectors s where s.id = p_sector and s.level_id = p_level) then
        raise exception 'invalid sector' using errcode = '22023';
    end if;

    if p_spot_id is null then
        insert into public.spots (
            condo_id, owner_id, level_id, sector_id, number, size_label, description,
            price_hour_cents, price_day_cents, price_week_cents,
            min_period_minutes, cancel_notice_hours, approval, rules
        ) values (
            p_condo, uid, p_level, p_sector, btrim(p_number), nullif(btrim(p_size_label), ''),
            nullif(btrim(p_description), ''), p_price_hour_cents, p_price_day_cents, p_price_week_cents,
            p_min_period_minutes, p_cancel_notice_hours, p_approval, coalesce(p_rules, '{}')
        )
        returning id into saved_id;
    else
        update public.spots set
            condo_id = p_condo, level_id = p_level, sector_id = p_sector, number = btrim(p_number),
            size_label = nullif(btrim(p_size_label), ''), description = nullif(btrim(p_description), ''),
            price_hour_cents = p_price_hour_cents, price_day_cents = p_price_day_cents,
            price_week_cents = p_price_week_cents, min_period_minutes = p_min_period_minutes,
            cancel_notice_hours = p_cancel_notice_hours, approval = p_approval, rules = coalesce(p_rules, '{}')
        where id = p_spot_id and owner_id = uid
        returning id into saved_id;
        if saved_id is null then
            raise exception 'spot not found' using errcode = 'P0002';
        end if;
        delete from public.spot_weekly_availability where spot_id = saved_id;
        delete from public.spot_date_overrides where spot_id = saved_id;
    end if;

    insert into public.spot_weekly_availability (spot_id, weekday, start_time, end_time)
    select saved_id, (w ->> 'weekday')::smallint, (w ->> 'start')::time, (w ->> 'end')::time
    from jsonb_array_elements(coalesce(p_weekly, '[]'::jsonb)) w;

    insert into public.spot_date_overrides (spot_id, day, kind, start_time, end_time)
    select saved_id, (o ->> 'day')::date, o ->> 'kind', (o ->> 'start')::time, (o ->> 'end')::time
    from jsonb_array_elements(coalesce(p_overrides, '[]'::jsonb)) o;

    return saved_id;
end;
$$;

-- Pausa ou reativa uma vaga do usuário.
create or replace function public.set_spot_status(p_spot uuid, p_status public.spot_status)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    update public.spots set status = p_status
    where id = p_spot and owner_id = (select auth.uid());
    if not found then
        raise exception 'spot not found' using errcode = 'P0002';
    end if;
end;
$$;

-- A busca e o convite (fase 1) passam a contar as vagas ativas anunciadas.
create or replace function public.search_condominiums(p_query text)
returns table (
    id            uuid,
    name          text,
    address       text,
    blocks_count  int,
    listed_spots  int,
    is_member     boolean
)
language sql
stable
security definer
set search_path = ''
as $$
    select c.id, c.name, c.address, coalesce(array_length(c.blocks, 1), 0),
           (select count(*)::int from public.spots s where s.condo_id = c.id and s.status = 'active'),
           public.is_condo_member(c.id)
    from public.condominiums c
    where char_length(btrim(p_query)) >= 2
      and (c.name ilike '%' || btrim(p_query) || '%' or c.address ilike '%' || btrim(p_query) || '%')
    order by c.name
    limit 20;
$$;

create or replace function public.find_condominium_by_invite(p_code text)
returns table (
    id            uuid,
    name          text,
    address       text,
    blocks        text[],
    blocks_count  int,
    listed_spots  int,
    is_member     boolean
)
language sql
stable
security definer
set search_path = ''
as $$
    select c.id, c.name, c.address, c.blocks, coalesce(array_length(c.blocks, 1), 0),
           (select count(*)::int from public.spots s where s.condo_id = c.id and s.status = 'active'),
           public.is_condo_member(c.id)
    from public.condominiums c
    where c.invite_code = upper(btrim(p_code));
$$;

-- Funções são executáveis por PUBLIC por padrão: só usuários logados chamam as RPCs.
revoke execute on function public.can_read_spot(uuid) from public, anon;
revoke execute on function public.save_spot(uuid, uuid, uuid, uuid, text, text, text, int, int, int, int, int, public.approval_mode, text[], jsonb, jsonb) from public, anon;
revoke execute on function public.set_spot_status(uuid, public.spot_status) from public, anon;
revoke execute on function public.search_condominiums(text) from public, anon;
revoke execute on function public.find_condominium_by_invite(text) from public, anon;
grant execute on function public.can_read_spot(uuid) to authenticated;
grant execute on function public.save_spot(uuid, uuid, uuid, uuid, text, text, text, int, int, int, int, int, public.approval_mode, text[], jsonb, jsonb) to authenticated;
grant execute on function public.set_spot_status(uuid, public.spot_status) to authenticated;
grant execute on function public.search_condominiums(text) to authenticated;
grant execute on function public.find_condominium_by_invite(text) to authenticated;
