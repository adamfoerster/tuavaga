-- Fase 1 · Condomínios, garagem, vínculo do morador e veículos.
-- Regras: só membros enxergam o condomínio e a garagem; o vínculo (membership) só é criado pelas
-- funções join_condominium / create_condominium, que também atualizam nome e telefone do perfil.

-- ---------------------------------------------------------------------------------------------
-- Perfil
-- ---------------------------------------------------------------------------------------------
alter table public.profiles add column if not exists phone text;

-- ---------------------------------------------------------------------------------------------
-- Tipos
-- ---------------------------------------------------------------------------------------------
do $$ begin
    create type public.membership_kind as enum ('morador', 'trabalho');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.vehicle_type as enum ('carro', 'moto', 'grande');
exception when duplicate_object then null; end $$;

-- ---------------------------------------------------------------------------------------------
-- Tabelas
-- ---------------------------------------------------------------------------------------------
create table if not exists public.condominiums (
    id           uuid primary key default gen_random_uuid(),
    name         text not null check (char_length(btrim(name)) between 2 and 120),
    address      text not null check (char_length(btrim(address)) between 3 and 200),
    cep          text check (cep ~ '^[0-9]{5}-[0-9]{3}$'),
    -- Blocos / torres oferecidos no cadastro do morador; vazio = condomínio sem blocos.
    blocks       text[] not null default '{}',
    timezone     text not null default 'America/Sao_Paulo',
    invite_code  text not null unique check (invite_code ~ '^[A-Z]{2}-[A-Z0-9]{4}$'),
    created_by   uuid references auth.users (id) on delete set null,
    created_at   timestamptz not null default now()
);

create table if not exists public.condo_levels (
    id         uuid primary key default gen_random_uuid(),
    condo_id   uuid not null references public.condominiums (id) on delete cascade,
    name       text not null check (char_length(btrim(name)) between 1 and 40),
    position   int  not null,
    unique (condo_id, position)
);

create table if not exists public.condo_sectors (
    id        uuid primary key default gen_random_uuid(),
    level_id  uuid not null references public.condo_levels (id) on delete cascade,
    name      text not null check (char_length(btrim(name)) between 1 and 20),
    position  int  not null,
    unique (level_id, position)
);

create table if not exists public.memberships (
    user_id     uuid not null references auth.users (id) on delete cascade,
    condo_id    uuid not null references public.condominiums (id) on delete cascade,
    block       text,
    unit        text not null check (char_length(btrim(unit)) between 1 and 20),
    kind        public.membership_kind not null default 'morador',
    created_at  timestamptz not null default now(),
    primary key (user_id, condo_id)
);
create index if not exists memberships_condo_idx on public.memberships (condo_id);

create table if not exists public.vehicles (
    id          uuid primary key default gen_random_uuid(),
    owner_id    uuid not null default auth.uid() references auth.users (id) on delete cascade,
    -- Placa antiga (ABC1234) ou Mercosul (ABC1D23), sem hífen.
    plate       text not null check (plate ~ '^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$'),
    model       text not null check (char_length(btrim(model)) between 1 and 60),
    color       text not null check (char_length(btrim(color)) between 1 and 30),
    type        public.vehicle_type not null default 'carro',
    created_at  timestamptz not null default now(),
    unique (owner_id, plate)
);

-- ---------------------------------------------------------------------------------------------
-- Helpers
-- ---------------------------------------------------------------------------------------------

-- security definer: usado dentro das policies de memberships sem recursão de RLS.
create or replace function public.is_condo_member(p_condo uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1 from public.memberships m
        where m.condo_id = p_condo and m.user_id = (select auth.uid())
    );
$$;

-- "Residencial Alameda Verde" -> "AV-4K7Q". Ignora palavras genéricas; sem 0/O/1/I para não confundir.
create or replace function public.generate_invite_code(p_name text)
returns text
language plpgsql
volatile
set search_path = ''
as $$
declare
    words        text[];
    significant  text[] := '{}';
    prefix       text;
    w            text;
    code         text;
    alphabet constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
begin
    words := array_remove(regexp_split_to_array(
        upper(translate(p_name, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ',
                                'aaaaaeeeeiiiiooooouuuucAAAAAEEEEIIIIOOOOOUUUUC')),
        '[^A-Z]+'), '');
    foreach w in array words loop
        continue when w in ('RESIDENCIAL', 'EDIFICIO', 'CONDOMINIO', 'COND', 'ED',
                            'DE', 'DA', 'DO', 'DAS', 'DOS', 'E');
        significant := significant || w;
    end loop;
    -- Two words: initials ("Alameda Verde" -> AV); one word: its first two letters ("Árvores" -> AR);
    -- only generic words: the first word ("Residencial" -> RE).
    prefix := case
        when cardinality(significant) >= 2 then left(significant[1], 1) || left(significant[2], 1)
        when cardinality(significant) = 1 then left(significant[1], 2)
        else left(coalesce(words[1], ''), 2)
    end;
    prefix := rpad(prefix, 2, 'X');

    loop
        code := prefix || '-';
        for i in 1..4 loop
            code := code || substr(alphabet, 1 + floor(random() * char_length(alphabet))::int, 1);
        end loop;
        exit when not exists (select 1 from public.condominiums c where c.invite_code = code);
    end loop;
    return code;
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------------------------------
alter table public.condominiums enable row level security;
alter table public.condo_levels enable row level security;
alter table public.condo_sectors enable row level security;
alter table public.memberships enable row level security;
alter table public.vehicles enable row level security;

drop policy if exists "Membro lê o condomínio" on public.condominiums;
create policy "Membro lê o condomínio"
    on public.condominiums for select to authenticated
    using (public.is_condo_member(id));

drop policy if exists "Membro lê os andares" on public.condo_levels;
create policy "Membro lê os andares"
    on public.condo_levels for select to authenticated
    using (public.is_condo_member(condo_id));

drop policy if exists "Membro lê os setores" on public.condo_sectors;
create policy "Membro lê os setores"
    on public.condo_sectors for select to authenticated
    using (exists (
        select 1 from public.condo_levels l
        where l.id = level_id and public.is_condo_member(l.condo_id)
    ));

-- Vínculos: cada um vê os próprios; insert só pelas funções abaixo.
drop policy if exists "Usuário lê os próprios vínculos" on public.memberships;
create policy "Usuário lê os próprios vínculos"
    on public.memberships for select to authenticated
    using ((select auth.uid()) = user_id);

drop policy if exists "Usuário atualiza os próprios vínculos" on public.memberships;
create policy "Usuário atualiza os próprios vínculos"
    on public.memberships for update to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

drop policy if exists "Usuário sai do condomínio" on public.memberships;
create policy "Usuário sai do condomínio"
    on public.memberships for delete to authenticated
    using ((select auth.uid()) = user_id);

drop policy if exists "Dono gerencia os veículos" on public.vehicles;
create policy "Dono gerencia os veículos"
    on public.vehicles for all to authenticated
    using ((select auth.uid()) = owner_id)
    with check ((select auth.uid()) = owner_id);

-- ---------------------------------------------------------------------------------------------
-- RPCs
-- ---------------------------------------------------------------------------------------------

-- Busca por nome ou endereço (mín. 2 letras). Não expõe membros nem o código de convite.
-- listed_spots é 0 até a fase 2 (tabela spots) redefinir esta função.
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
    select c.id, c.name, c.address, coalesce(array_length(c.blocks, 1), 0), 0,
           public.is_condo_member(c.id)
    from public.condominiums c
    where char_length(btrim(p_query)) >= 2
      and (c.name ilike '%' || btrim(p_query) || '%' or c.address ilike '%' || btrim(p_query) || '%')
    order by c.name
    limit 20;
$$;

-- Prévia do condomínio de um código de convite (nenhuma linha = código não encontrado).
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
    select c.id, c.name, c.address, c.blocks, coalesce(array_length(c.blocks, 1), 0), 0,
           public.is_condo_member(c.id)
    from public.condominiums c
    where c.invite_code = upper(btrim(p_code));
$$;

-- Blocos de um condomínio ainda não acessível pelo RLS (passo "Seus dados" após a busca).
create or replace function public.condominium_blocks(p_condo uuid)
returns text[]
language sql
stable
security definer
set search_path = ''
as $$
    select c.blocks from public.condominiums c where c.id = p_condo;
$$;

create or replace function public.update_own_profile(p_full_name text, p_phone text)
returns void
language sql
security definer
set search_path = ''
as $$
    update public.profiles
    set full_name = coalesce(nullif(btrim(p_full_name), ''), full_name),
        phone = coalesce(nullif(btrim(p_phone), ''), phone)
    where id = (select auth.uid());
$$;

-- Entra (ou atualiza o vínculo) no condomínio. Idempotente.
create or replace function public.join_condominium(
    p_condo      uuid,
    p_block      text,
    p_unit       text,
    p_kind       public.membership_kind,
    p_full_name  text,
    p_phone      text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    uid uuid := (select auth.uid());
    condo_blocks text[];
begin
    if uid is null then
        raise exception 'not authenticated' using errcode = '28000';
    end if;
    select c.blocks into condo_blocks from public.condominiums c where c.id = p_condo;
    if not found then
        raise exception 'condominium not found' using errcode = 'P0002';
    end if;
    if cardinality(condo_blocks) > 0 and not coalesce(p_block = any (condo_blocks), false) then
        raise exception 'invalid block' using errcode = '22023';
    end if;

    insert into public.memberships (user_id, condo_id, block, unit, kind)
    values (uid, p_condo, nullif(btrim(p_block), ''), btrim(p_unit), p_kind)
    on conflict (user_id, condo_id) do update
        set block = excluded.block, unit = excluded.unit, kind = excluded.kind;

    perform public.update_own_profile(p_full_name, p_phone);
end;
$$;

-- Cria o condomínio com a garagem e já vincula quem criou.
-- p_levels: [{"name": "Subsolo 1", "sectors": ["A", "B"]}, ...] na ordem de exibição.
create or replace function public.create_condominium(
    p_name       text,
    p_address    text,
    p_cep        text,
    p_blocks     text[],
    p_levels     jsonb,
    p_block      text,
    p_unit       text,
    p_kind       public.membership_kind,
    p_full_name  text,
    p_phone      text
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    uid uuid := (select auth.uid());
    new_id uuid;
    level jsonb;
    level_id uuid;
    level_pos int := 0;
    sector_pos int;
    sector text;
begin
    if uid is null then
        raise exception 'not authenticated' using errcode = '28000';
    end if;
    if jsonb_typeof(p_levels) <> 'array' or jsonb_array_length(p_levels) = 0 then
        raise exception 'at least one level is required' using errcode = '22023';
    end if;

    insert into public.condominiums (name, address, cep, blocks, invite_code, created_by)
    values (btrim(p_name), btrim(p_address), nullif(btrim(p_cep), ''),
            coalesce((select array_agg(btrim(b)) from unnest(p_blocks) b where btrim(b) <> ''), '{}'),
            public.generate_invite_code(p_name), uid)
    returning id into new_id;

    for level in select * from jsonb_array_elements(p_levels) loop
        level_pos := level_pos + 1;
        insert into public.condo_levels (condo_id, name, position)
        values (new_id, btrim(level ->> 'name'), level_pos)
        returning id into level_id;

        sector_pos := 0;
        for sector in select jsonb_array_elements_text(coalesce(level -> 'sectors', '[]'::jsonb)) loop
            sector_pos := sector_pos + 1;
            insert into public.condo_sectors (level_id, name, position)
            values (level_id, btrim(sector), sector_pos);
        end loop;
    end loop;

    perform public.join_condominium(new_id, p_block, p_unit, p_kind, p_full_name, p_phone);
    return new_id;
end;
$$;

-- Funções são executáveis por PUBLIC por padrão: só usuários logados chamam as RPCs.
revoke execute on function public.generate_invite_code(text) from public, anon, authenticated;
revoke execute on function public.update_own_profile(text, text) from public, anon, authenticated;
revoke execute on function public.search_condominiums(text) from public, anon;
revoke execute on function public.find_condominium_by_invite(text) from public, anon;
revoke execute on function public.condominium_blocks(uuid) from public, anon;
revoke execute on function public.join_condominium(uuid, text, text, public.membership_kind, text, text) from public, anon;
revoke execute on function public.create_condominium(text, text, text, text[], jsonb, text, text, public.membership_kind, text, text) from public, anon;
grant execute on function public.search_condominiums(text) to authenticated;
grant execute on function public.find_condominium_by_invite(text) to authenticated;
grant execute on function public.condominium_blocks(uuid) to authenticated;
grant execute on function public.join_condominium(uuid, text, text, public.membership_kind, text, text) to authenticated;
grant execute on function public.create_condominium(text, text, text, text[], jsonb, text, text, public.membership_kind, text, text) to authenticated;
