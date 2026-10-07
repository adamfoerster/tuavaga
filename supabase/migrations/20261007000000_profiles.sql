-- Perfil público de cada usuário do Supabase Auth.
-- Criado automaticamente no cadastro a partir de raw_user_meta_data (o app envia full_name).

create table if not exists public.profiles (
    id          uuid primary key references auth.users (id) on delete cascade,
    email       text not null,
    full_name   text,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

alter table public.profiles enable row level security;

create policy "Usuário lê o próprio perfil"
    on public.profiles for select
    to authenticated
    using ((select auth.uid()) = id);

create policy "Usuário atualiza o próprio perfil"
    on public.profiles for update
    to authenticated
    using ((select auth.uid()) = id)
    with check ((select auth.uid()) = id);

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.profiles (id, email, full_name)
    values (new.id, new.email, new.raw_user_meta_data ->> 'full_name');
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

create or replace function public.touch_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists profiles_touch_updated_at on public.profiles;
create trigger profiles_touch_updated_at
    before update on public.profiles
    for each row execute function public.touch_updated_at();
