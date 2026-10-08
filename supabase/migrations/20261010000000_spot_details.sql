-- Fase 3 · Detalhes da vaga usados pelo Explorar (pranchas 04 e 07): características (filtros e tags),
-- pé-direito e "como chegar". save_spot ganha os parâmetros novos.

alter table public.spots add column if not exists features text[] not null default '{}';
alter table public.spots add column if not exists height_cm int;
alter table public.spots add column if not exists directions text;

do $$ begin
    alter table public.spots add constraint spots_features_check
        check (features <@ array['coberta', 'larga', 'eletrica', 'elevador', 'moto']::text[]);
exception when duplicate_object then null; end $$;

do $$ begin
    alter table public.spots add constraint spots_height_check check (height_cm between 150 and 500);
exception when duplicate_object then null; end $$;

do $$ begin
    alter table public.spots add constraint spots_directions_check check (char_length(directions) <= 200);
exception when duplicate_object then null; end $$;

-- A assinatura muda: remove a versão da fase 2 para não deixar uma sobrecarga antiga.
drop function if exists public.save_spot(uuid, uuid, uuid, uuid, text, text, text, int, int, int, int, int, public.approval_mode, text[], jsonb, jsonb);

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
    p_features            text[],
    p_height_cm           int,
    p_directions          text,
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
            condo_id, owner_id, level_id, sector_id, number, size_label, description, features, height_cm,
            directions, price_hour_cents, price_day_cents, price_week_cents,
            min_period_minutes, cancel_notice_hours, approval, rules
        ) values (
            p_condo, uid, p_level, p_sector, btrim(p_number), nullif(btrim(p_size_label), ''),
            nullif(btrim(p_description), ''), coalesce(p_features, '{}'), p_height_cm,
            nullif(btrim(p_directions), ''), p_price_hour_cents, p_price_day_cents, p_price_week_cents,
            p_min_period_minutes, p_cancel_notice_hours, p_approval, coalesce(p_rules, '{}')
        )
        returning id into saved_id;
    else
        update public.spots set
            condo_id = p_condo, level_id = p_level, sector_id = p_sector, number = btrim(p_number),
            size_label = nullif(btrim(p_size_label), ''), description = nullif(btrim(p_description), ''),
            features = coalesce(p_features, '{}'), height_cm = p_height_cm,
            directions = nullif(btrim(p_directions), ''),
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

revoke execute on function public.save_spot(uuid, uuid, uuid, uuid, text, text, text, text[], int, text, int, int, int, int, int, public.approval_mode, text[], jsonb, jsonb) from public, anon;
grant execute on function public.save_spot(uuid, uuid, uuid, uuid, text, text, text, text[], int, text, int, int, int, int, int, public.approval_mode, text[], jsonb, jsonb) to authenticated;
