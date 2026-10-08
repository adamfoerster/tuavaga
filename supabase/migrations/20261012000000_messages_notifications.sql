-- Fase 5 · Chat da reserva e notificações em tempo real (pranchas 26 e 29).
-- Mensagens: só locatário e locador da reserva leem e escrevem (insert direto, sempre como si mesmo).
-- Notificações e mensagens de sistema nascem de um gatilho em bookings e de settle_bookings()
-- (lembrete de check-in e atraso). As duas tabelas entram na publicação do Realtime; o Realtime
-- respeita a RLS, então cada usuário só recebe o que pode ler.

-- ---------------------------------------------------------------------------------------------
-- Tipos
-- ---------------------------------------------------------------------------------------------
do $$ begin
    create type public.message_kind as enum ('text', 'system');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.notification_kind as enum
        ('request', 'booked', 'approved', 'rejected', 'cancelled', 'expired', 'reminder', 'late');
exception when duplicate_object then null; end $$;

-- ---------------------------------------------------------------------------------------------
-- Tabelas
-- ---------------------------------------------------------------------------------------------
create table if not exists public.messages (
    id          uuid primary key default gen_random_uuid(),
    booking_id  uuid not null references public.bookings (id) on delete cascade,
    -- null = mensagem de sistema ("CHECK-IN · 08:02").
    sender_id   uuid references auth.users (id) on delete set null,
    kind        public.message_kind not null default 'text',
    body        text not null check (char_length(btrim(body)) between 1 and 1000),
    created_at  timestamptz not null default now(),
    -- Lida pela outra parte (só mensagens de texto contam como não lidas).
    read_at     timestamptz,
    check ((kind = 'system') = (sender_id is null))
);
create index if not exists messages_booking_idx on public.messages (booking_id, created_at);

create table if not exists public.notifications (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references auth.users (id) on delete cascade,
    condo_id    uuid not null references public.condominiums (id) on delete cascade,
    booking_id  uuid references public.bookings (id) on delete cascade,
    kind        public.notification_kind not null,
    title       text not null,
    body        text not null,
    created_at  timestamptz not null default now(),
    read_at     timestamptz
);
create index if not exists notifications_user_idx on public.notifications (user_id, created_at desc);
-- Lembrete e atraso saem uma vez por reserva.
create unique index if not exists notifications_once_idx
    on public.notifications (booking_id, kind) where kind in ('reminder', 'late');

-- ---------------------------------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------------------------------
alter table public.messages enable row level security;
alter table public.notifications enable row level security;

drop policy if exists "Partes da reserva leem as mensagens" on public.messages;
create policy "Partes da reserva leem as mensagens"
    on public.messages for select to authenticated
    using (exists (
        select 1 from public.bookings b
        where b.id = messages.booking_id and (select auth.uid()) in (b.renter_id, b.owner_id)
    ));

drop policy if exists "Partes da reserva escrevem como si mesmas" on public.messages;
create policy "Partes da reserva escrevem como si mesmas"
    on public.messages for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and kind = 'text'
        and read_at is null
        and exists (
            select 1 from public.bookings b
            where b.id = messages.booking_id and (select auth.uid()) in (b.renter_id, b.owner_id)
        )
    );

drop policy if exists "Usuário lê as próprias notificações" on public.notifications;
create policy "Usuário lê as próprias notificações"
    on public.notifications for select to authenticated
    using (user_id = (select auth.uid()));

-- ---------------------------------------------------------------------------------------------
-- Textos (iguais aos do app: "B2-27", "Marina R.", "Sex 16/10 · 19:00")
-- ---------------------------------------------------------------------------------------------
create or replace function public.spot_label(p_spot uuid)
returns text
language sql
stable
security definer
set search_path = ''
as $$
    select upper(coalesce(btrim(sec.name), ''))
        || coalesce(nullif(regexp_replace(l.name, '\D', '', 'g'), ''), upper(left(btrim(l.name), 1)))
        || '-'
        || case when btrim(s.number) ~ '^\d$' then '0' || btrim(s.number) else upper(btrim(s.number)) end
    from public.spots s
    join public.condo_levels l on l.id = s.level_id
    left join public.condo_sectors sec on sec.id = s.sector_id
    where s.id = p_spot;
$$;

create or replace function public.short_name(p_user uuid)
returns text
language sql
stable
security definer
set search_path = ''
as $$
    select case
        when parts is null or cardinality(parts) = 0 then 'Um vizinho'
        when cardinality(parts) = 1 then parts[1]
        else parts[1] || ' ' || upper(left(parts[cardinality(parts)], 1)) || '.'
    end
    from (
        select regexp_split_to_array(nullif(btrim(p.full_name), ''), '\s+') as parts
        from (select 1) one
        left join public.profiles p on p.id = p_user
    ) n;
$$;

-- "Sex 16/10 · 19:00" no fuso do condomínio.
create or replace function public.local_label(p_at timestamptz, p_tz text)
returns text
language sql
stable
set search_path = ''
as $$
    select (array['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'])[extract(dow from p_at at time zone p_tz)::int + 1]
        || ' ' || to_char(p_at at time zone p_tz, 'DD/MM') || ' · ' || to_char(p_at at time zone p_tz, 'HH24:MI');
$$;

create or replace function public.local_time(p_at timestamptz, p_tz text)
returns text
language sql
stable
set search_path = ''
as $$
    select to_char(p_at at time zone p_tz, 'HH24:MI');
$$;

-- ---------------------------------------------------------------------------------------------
-- Eventos da reserva → notificações e mensagens de sistema
-- ---------------------------------------------------------------------------------------------
create or replace function public.booking_events()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    tz text;
    spot text := public.spot_label(new.spot_id);
    renter text := public.short_name(new.renter_id);
    owner text := public.short_name(new.owner_id);
    period text;
    reason text;
begin
    select c.timezone into tz from public.condominiums c where c.id = new.condo_id;
    tz := coalesce(tz, 'America/Sao_Paulo');
    period := public.local_label(new.starts_at, tz) || ' → ' || public.local_label(new.ends_at, tz);

    if tg_op = 'INSERT' then
        if new.status = 'pending' then
            insert into public.notifications (user_id, condo_id, booking_id, kind, title, body)
            values (new.owner_id, new.condo_id, new.id, 'request', renter || ' pediu a vaga ' || spot,
                    period || '. Responda até ' || coalesce(public.local_label(new.respond_by, tz), 'a entrada') || '.');
        else
            insert into public.notifications (user_id, condo_id, booking_id, kind, title, body)
            values (new.owner_id, new.condo_id, new.id, 'booked', renter || ' reservou a vaga ' || spot,
                    period || '. Confirmada na hora.');
        end if;
        -- The note of the request opens the conversation.
        if new.note is not null then
            insert into public.messages (booking_id, sender_id, kind, body)
            values (new.id, new.renter_id, 'text', new.note);
        end if;
        return new;
    end if;

    if new.ends_at is distinct from old.ends_at and new.status = old.status then
        insert into public.messages (booking_id, kind, body)
        values (new.id, 'system', 'SAÍDA AJUSTADA PARA ' || public.local_label(new.ends_at, tz));
        return new;
    end if;

    if new.status = old.status then
        return new;
    end if;

    case new.status
        when 'confirmed' then
            insert into public.notifications (user_id, condo_id, booking_id, kind, title, body)
            values (new.renter_id, new.condo_id, new.id, 'approved', 'Sua reserva da vaga ' || spot || ' foi aprovada',
                    period || '. O check-in libera no dia da reserva.');
            insert into public.messages (booking_id, kind, body) values (new.id, 'system', 'RESERVA CONFIRMADA');
        when 'rejected' then
            reason := case new.reject_reason
                when 'visita' then 'vaga reservada para visita'
                when 'uso' then 'o locador vai usar a vaga'
                when 'veiculo' then 'o veículo não cabe'
                else 'outro motivo' end;
            insert into public.notifications (user_id, condo_id, booking_id, kind, title, body)
            values (new.renter_id, new.condo_id, new.id, 'rejected', owner || ' recusou a vaga ' || spot,
                    'Motivo: ' || reason || '. Veja outras vagas livres.');
            insert into public.messages (booking_id, kind, body) values (new.id, 'system', 'PEDIDO RECUSADO');
        when 'cancelled' then
            insert into public.notifications (user_id, condo_id, booking_id, kind, title, body)
            values (
                case when new.cancelled_by = new.owner_id then new.renter_id else new.owner_id end,
                new.condo_id, new.id, 'cancelled',
                case when new.cancelled_by = new.owner_id then owner else renter end
                    || ' cancelou a reserva da vaga ' || spot,
                period || '.');
            insert into public.messages (booking_id, kind, body) values (new.id, 'system', 'RESERVA CANCELADA');
        when 'expired' then
            insert into public.notifications (user_id, condo_id, booking_id, kind, title, body)
            values (new.renter_id, new.condo_id, new.id, 'expired', 'Pedido da vaga ' || spot || ' sem resposta',
                    'O pedido de ' || period || ' venceu. Veja outras vagas livres.');
        when 'in_progress' then
            insert into public.messages (booking_id, kind, body)
            values (new.id, 'system', 'CHECK-IN · ' || public.local_time(coalesce(new.checked_in_at, now()), tz));
        when 'completed' then
            if new.checked_out_at is not null and old.status = 'in_progress' then
                insert into public.messages (booking_id, kind, body)
                values (new.id, 'system', 'CHECK-OUT · ' || public.local_time(new.checked_out_at, tz));
            end if;
        else
            null;
    end case;
    return new;
end;
$$;

drop trigger if exists bookings_events on public.bookings;
create trigger bookings_events
    after insert or update of status, ends_at on public.bookings
    for each row execute function public.booking_events();

-- Lembrete (véspera do check-in) e atraso (passou 15 min da saída), uma vez por reserva.
create or replace function public.notify_due_bookings()
returns void
language sql
security definer
set search_path = ''
as $$
    insert into public.notifications (user_id, condo_id, booking_id, kind, title, body)
    select b.renter_id, b.condo_id, b.id, 'reminder',
           'Check-in libera ' || public.local_label(b.starts_at - interval '30 minutes', coalesce(c.timezone, 'America/Sao_Paulo')),
           'Vaga ' || public.spot_label(b.spot_id) || ' · reserva ' || b.code || '. Entrada combinada às '
               || public.local_time(b.starts_at, coalesce(c.timezone, 'America/Sao_Paulo')) || '.'
    from public.bookings b
    join public.condominiums c on c.id = b.condo_id
    where b.status = 'confirmed' and b.starts_at > now() and b.starts_at <= now() + interval '24 hours'
    on conflict (booking_id, kind) where kind in ('reminder', 'late') do nothing;

    insert into public.notifications (user_id, condo_id, booking_id, kind, title, body)
    select b.owner_id, b.condo_id, b.id, 'late',
           public.short_name(b.renter_id) || ' passou do horário na vaga ' || public.spot_label(b.spot_id),
           'Saída combinada às ' || public.local_time(b.ends_at, coalesce(c.timezone, 'America/Sao_Paulo'))
               || '. Fale com o vizinho pelo chat.'
    from public.bookings b
    join public.condominiums c on c.id = b.condo_id
    where b.status = 'in_progress' and b.ends_at + interval '15 minutes' < now()
    on conflict (booking_id, kind) where kind in ('reminder', 'late') do nothing;
$$;

-- settle_bookings (fase 4) passa a gerar também os lembretes e avisos de atraso.
create or replace function public.settle_bookings()
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    update public.bookings set status = 'expired'
    where status = 'pending' and (respond_by < now() or starts_at < now());

    update public.bookings set status = 'completed'
    where status = 'confirmed' and ends_at < now();

    update public.bookings set status = 'completed', checked_out_at = ends_at
    where status = 'in_progress' and ends_at + interval '12 hours' < now();

    perform public.notify_due_bookings();
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- RPCs
-- ---------------------------------------------------------------------------------------------

-- Aba Mensagens: uma conversa por reserva em que o usuário é parte, com a última mensagem e quantas
-- mensagens da outra parte ainda não foram lidas. Reservas sem mensagens aparecem enquanto ativas.
create or replace function public.my_conversations()
returns table (
    booking_id        uuid,
    code              bigint,
    role              text,
    status            public.booking_status,
    condo_id          uuid,
    condo_name        text,
    spot_label        text,
    starts_at         timestamptz,
    ends_at           timestamptz,
    counterpart_name  text,
    last_body         text,
    last_kind         public.message_kind,
    last_mine         boolean,
    last_at           timestamptz,
    unread            int
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
           b.status, b.condo_id, c.name, public.spot_label(b.spot_id), b.starts_at, b.ends_at,
           p.full_name,
           last.body, last.kind, last.sender_id = uid, last.created_at,
           (select count(*)::int from public.messages m
            where m.booking_id = b.id and m.kind = 'text' and m.sender_id <> uid and m.read_at is null)
    from public.bookings b
    join public.condominiums c on c.id = b.condo_id
    left join public.profiles p on p.id = case when b.renter_id = uid then b.owner_id else b.renter_id end
    left join lateral (
        select m.body, m.kind, m.sender_id, m.created_at
        from public.messages m where m.booking_id = b.id
        order by m.created_at desc limit 1
    ) last on true
    where uid in (b.renter_id, b.owner_id)
      and (last.created_at is not null or b.status in ('pending', 'confirmed', 'in_progress'))
    order by coalesce(last.created_at, b.created_at) desc;
end;
$$;

-- Marca como lidas as mensagens da outra parte numa conversa.
create or replace function public.mark_messages_read(p_booking uuid)
returns void
language sql
security definer
set search_path = ''
as $$
    update public.messages m set read_at = now()
    from public.bookings b
    where m.booking_id = p_booking and b.id = p_booking
      and (select auth.uid()) in (b.renter_id, b.owner_id)
      and m.kind = 'text' and m.sender_id <> (select auth.uid()) and m.read_at is null;
$$;

-- "Marcar como lidas" (prancha 29): todas, ou só as de um condomínio.
create or replace function public.mark_notifications_read(p_condo uuid default null)
returns void
language sql
security definer
set search_path = ''
as $$
    update public.notifications set read_at = now()
    where user_id = (select auth.uid()) and read_at is null and (p_condo is null or condo_id = p_condo);
$$;

-- ---------------------------------------------------------------------------------------------
-- Realtime
-- ---------------------------------------------------------------------------------------------
do $$ begin
    if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
        begin
            alter publication supabase_realtime add table public.messages;
        exception when duplicate_object then null; end;
        begin
            alter publication supabase_realtime add table public.notifications;
        exception when duplicate_object then null; end;
    end if;
end $$;

-- ---------------------------------------------------------------------------------------------
-- Permissões
-- ---------------------------------------------------------------------------------------------
revoke execute on function public.spot_label(uuid) from public, anon, authenticated;
revoke execute on function public.short_name(uuid) from public, anon, authenticated;
revoke execute on function public.local_label(timestamptz, text) from public, anon;
revoke execute on function public.local_time(timestamptz, text) from public, anon;
revoke execute on function public.booking_events() from public, anon, authenticated;
revoke execute on function public.notify_due_bookings() from public, anon, authenticated;
revoke execute on function public.settle_bookings() from public, anon, authenticated;
revoke execute on function public.my_conversations() from public, anon;
revoke execute on function public.mark_messages_read(uuid) from public, anon;
revoke execute on function public.mark_notifications_read(uuid) from public, anon;
grant execute on function public.my_conversations() to authenticated;
grant execute on function public.mark_messages_read(uuid) to authenticated;
grant execute on function public.mark_notifications_read(uuid) to authenticated;
