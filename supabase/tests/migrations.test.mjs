// Applies every migration to an in-memory Postgres (PGlite) with stand-ins for Supabase's `auth`
// schema and roles, then checks RLS and the RPCs as two different users.
// Run: npm --prefix supabase/tests ci && npm --prefix supabase/tests test
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { PGlite } from '@electric-sql/pglite';
import { btree_gist } from '@electric-sql/pglite/contrib/btree_gist';

const migrationsDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'migrations');
const db = new PGlite({ extensions: { btree_gist } });

let failures = 0;
const ok = (name) => console.log('  ok  ' + name);
const fail = (name, detail) => { failures++; console.log('  FAIL ' + name + ' :: ' + detail); };
async function expectOk(name, sql, params = [], check) {
  try {
    const r = await db.query(sql, params);
    if (check) { const msg = check(r.rows); if (msg) return fail(name, msg); }
    ok(name);
    return r.rows;
  } catch (e) { fail(name, e.message); }
}
async function expectError(name, sql, params = [], codeOrText) {
  try { await db.query(sql, params); fail(name, 'no error'); }
  catch (e) {
    // Only database errors count; a JS error here means the test itself is wrong.
    if (!e.code && !/permission denied|row-level security/.test(e.message)) return fail(name, 'not a database error: ' + e.message);
    const got = (e.code || '') + ' ' + e.message;
    if (codeOrText && !got.includes(codeOrText)) fail(name, 'unexpected error: ' + got); else ok(name + ' -> ' + (e.code || e.message));
  }
}
async function as(user) {
  await db.exec('reset role');
  if (user === 'anon') { await db.exec(`set role anon; select set_config('request.jwt.claim.sub', '', false);`); return; }
  await db.exec(`set role authenticated; select set_config('request.jwt.claim.sub', '${user}', false);`);
}

// --- Supabase stand-ins ----------------------------------------------------------------------
await db.exec(`
  create role anon nologin; create role authenticated nologin;
  create schema extensions;
  create schema auth;
  create table auth.users (id uuid primary key, email text, raw_user_meta_data jsonb default '{}'::jsonb);
  create function auth.uid() returns uuid language sql stable as
    $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
  grant usage on schema public, auth to anon, authenticated;
  grant execute on function auth.uid() to anon, authenticated;
  alter default privileges in schema public grant all on tables to anon, authenticated;
  alter default privileges in schema public grant usage on types to anon, authenticated;
`);
// Supabase's Realtime publication (the messages/notifications migration adds its tables to it).
let hasPublication = true;
try { await db.exec('create publication supabase_realtime'); } catch { hasPublication = false; }

// --- Migrations --------------------------------------------------------------------------------
const files = readdirSync(migrationsDir).filter((f) => f.endsWith('.sql')).sort();
for (const f of files) {
  try { await db.exec(readFileSync(join(migrationsDir, f), 'utf8')); ok('apply ' + f); }
  catch (e) { fail('apply ' + f, e.message); process.exit(1); }
}
// Every migration after the first (profiles predates the idempotency rule) must survive being
// applied again; re-apply them in order so later ones keep overriding earlier function versions.
for (const f of files.slice(1)) {
  try { await db.exec(readFileSync(join(migrationsDir, f), 'utf8')); ok('re-apply ' + f + ' (idempotent)'); }
  catch (e) { fail('re-apply ' + f, e.message); }
}

const U1 = '11111111-1111-1111-1111-111111111111';
const U2 = '22222222-2222-2222-2222-222222222222';
await db.exec(`insert into auth.users (id, email, raw_user_meta_data) values
  ('${U1}', 'adam@x.com', '{"full_name":"Adam"}'), ('${U2}', 'marina@x.com', '{"full_name":"Marina"}');`);

console.log('\n# create (u1)');
await as(U1);
const levels = JSON.stringify([{ name: 'Subsolo 1', sectors: ['A', 'B'] }, { name: 'Subsolo 2', sectors: ['A', 'B', 'C'] }, { name: 'Térreo', sectors: [] }]);
const created = await expectOk('create_condominium',
  `select public.create_condominium($1, $2, $3, $4::text[], $5::jsonb, $6, $7, 'morador', $8, $9) as id`,
  ['Residencial Alameda Verde', 'Rua das Figueiras, 410', '01310-100', '{A,B,C}', levels, 'B', '142', 'Adam Foerster', '(11) 91234-5678']);
const condoId = created?.[0]?.id;
const code = (await expectOk('u1 reads the condominium', 'select invite_code from public.condominiums', [], (r) => r.length === 1 ? null : 'rows=' + r.length))?.[0]?.invite_code;
console.log('     invite code: ' + code);
if (!/^AV-[A-Z0-9]{4}$/.test(code ?? '')) fail('invite code prefix', code); else ok('invite code prefix AV-');
await expectOk('levels in order', 'select name from public.condo_levels order by position', [], (r) => r.map((x) => x.name).join('|') === 'Subsolo 1|Subsolo 2|Térreo' ? null : JSON.stringify(r));
await expectOk('5 sectors', 'select count(*)::int n from public.condo_sectors', [], (r) => r[0].n === 5 ? null : 'n=' + r[0].n);
await expectOk('profile updated', 'select full_name, phone from public.profiles where id = $1', [U1], (r) => r[0].full_name === 'Adam Foerster' && r[0].phone === '(11) 91234-5678' ? null : JSON.stringify(r));
await expectOk('membership embedded like the app reads it',
  `select m.block, m.unit, m.kind, c.name from public.memberships m join public.condominiums c on c.id = m.condo_id where m.user_id = $1`, [U1],
  (r) => r.length === 1 && r[0].block === 'B' && r[0].kind === 'morador' ? null : JSON.stringify(r));
await expectError('create without levels', `select public.create_condominium('X Y', 'Rua 1', null, '{}', '[]'::jsonb, null, '1', 'morador', null, null)`, [], '22023');
await expectError('direct insert into memberships is blocked', `insert into public.memberships (user_id, condo_id, unit) values ($1, $2, '1')`, [U1, condoId], 'row-level security');

console.log('\n# other user (u2)');
await as(U2);
await expectOk('u2 cannot read the condominium yet', 'select * from public.condominiums', [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectOk('u2 cannot read the garage yet', 'select * from public.condo_levels', [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectOk('search finds it', `select * from public.search_condominiums('alameda')`, [], (r) => r.length === 1 && r[0].blocks_count === 3 && r[0].is_member === false ? null : JSON.stringify(r));
await expectOk('search ignores 1 letter', `select * from public.search_condominiums('a')`, [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectOk('invite lookup (lower case)', `select * from public.find_condominium_by_invite($1)`, [code.toLowerCase()], (r) => r.length === 1 && r[0].blocks.length === 3 ? null : JSON.stringify(r));
await expectOk('unknown invite returns nothing', `select * from public.find_condominium_by_invite('ZZ-0000')`, [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectOk('blocks of a condo', `select public.condominium_blocks($1) b`, [condoId], (r) => r[0].b.join() === 'A,B,C' ? null : JSON.stringify(r));
await expectError('join with block outside the list', `select public.join_condominium($1, 'Z', '12', 'morador', null, null)`, [condoId], '22023');
await expectError('join without block when the condo has blocks', `select public.join_condominium($1, null, '12', 'morador', null, null)`, [condoId], '22023');
await expectOk('join', `select public.join_condominium($1, 'A', '12', 'trabalho', 'Marina R.', null)`, [condoId]);
await expectOk('join again is idempotent', `select public.join_condominium($1, 'C', '31', 'morador', '', '')`, [condoId]);
await expectOk('u2 now reads the condominium', 'select * from public.condominiums', [], (r) => r.length === 1 ? null : 'rows=' + r.length);
await expectOk('u2 sees only its own membership', 'select user_id, block, unit from public.memberships', [], (r) => r.length === 1 && r[0].user_id === U2 && r[0].block === 'C' ? null : JSON.stringify(r));
await expectOk('blank name/phone keep the profile', 'select full_name from public.profiles where id = $1', [U2], (r) => r[0].full_name === 'Marina R.' ? null : JSON.stringify(r));
await expectOk('u2 updates its own membership', `update public.memberships set unit = '32' where condo_id = $1 returning unit`, [condoId], (r) => r.length === 1 ? null : 'rows=' + r.length);

console.log('\n# vehicles');
await expectOk('add vehicle', `insert into public.vehicles (plate, model, color, type) values ('ABC1D23', 'Onix', 'Preto', 'carro') returning owner_id`, [], (r) => r[0].owner_id === U2 ? null : JSON.stringify(r));
await expectError('duplicate plate', `insert into public.vehicles (plate, model, color, type) values ('ABC1D23', 'Onix', 'Preto', 'carro')`, [], '23505');
await expectError('invalid plate', `insert into public.vehicles (plate, model, color, type) values ('abc-1d23', 'Onix', 'Preto', 'carro')`, [], '23514');
await expectError('vehicle for someone else', `insert into public.vehicles (owner_id, plate, model, color, type) values ($1, 'XYZ4E56', 'CG', 'Preto', 'moto')`, [U1], 'row-level security');
await as(U1);
await expectOk('u1 cannot see u2 vehicles', 'select * from public.vehicles', [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectOk('u1 sees only its own membership', 'select user_id from public.memberships', [], (r) => r.length === 1 && r[0].user_id === U1 ? null : JSON.stringify(r));

console.log('\n# leave + anon');
await as(U2);
await expectOk('u2 leaves', 'delete from public.memberships where condo_id = $1 returning 1', [condoId], (r) => r.length === 1 ? null : 'rows=' + r.length);
await expectOk('u2 lost access', 'select * from public.condominiums', [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await as('anon');
await expectError('anon cannot search', `select * from public.search_condominiums('alameda')`, [], 'permission denied');
await expectError('anon cannot generate codes', `select public.generate_invite_code('x')`, [], 'permission denied');

console.log('\n# invite code prefixes');
await db.exec('reset role');
for (const [name, prefix] of [['Edifício Santa Clara', 'SC'], ['Condomínio das Árvores', 'AR'], ['Ipê', 'IP'], ['Residencial', 'RE'], ['A', 'AX'], ['123', 'XX']]) {
  const r = await db.query('select public.generate_invite_code($1) c', [name]);
  const c = r.rows[0].c;
  if (c.startsWith(prefix + '-') && /^[A-Z]{2}-[A-Z0-9]{4}$/.test(c)) ok(`${name} -> ${c}`); else fail(name, c);
}

// --- Fase 2 · vagas ---------------------------------------------------------------------------
console.log('\n# spots (u1 owns, u2 joins later)');
await as(U1);
const garage = (await db.query(
  `select l.id level_id, l.name, s.id sector_id, s.name sector from public.condo_levels l
   left join public.condo_sectors s on s.level_id = l.id order by l.position, s.position`)).rows;
const s2 = garage.find((g) => g.name === 'Subsolo 2' && g.sector === 'B');
const s1 = garage.find((g) => g.name === 'Subsolo 1' && g.sector === 'A');
const terreo = garage.find((g) => g.name === 'Térreo');
const weekly = JSON.stringify([1, 2, 3, 4, 5].map((d) => ({ weekday: d, start: '08:00', end: '18:00' })));
const overrides = JSON.stringify([{ day: '2026-10-12', kind: 'blocked' }, { day: '2026-10-17', kind: 'open', start: '09:00', end: '12:00' }]);
const saveSpot = (id, level, sector, number, hour, day, week, weekly = '[]', overrides = '[]') =>
  `select public.save_spot(${id ? `'${id}'` : 'null'}, '${condoId}', '${level}', ${sector ? `'${sector}'` : 'null'}, '${number}',
     '2,5 × 5,0', 'Perto do elevador', '{coberta}', 210, 'Terceira depois do elevador', ${hour ?? 'null'}, ${day ?? 'null'}, ${week ?? 'null'}, 120, 24, 'manual',
     '{Sem caminhonete,Respeitar horário}', '${weekly}'::jsonb, '${overrides}'::jsonb) as id`;
const spotId = (await expectOk('create spot B2-14', saveSpot(null, s2.level_id, s2.sector_id, '14', 800, 3500, 18000, weekly, overrides)))?.[0]?.id;
await expectOk('weekly window saved', 'select count(*)::int n from public.spot_weekly_availability where spot_id = $1', [spotId], (r) => r[0].n === 5 ? null : 'n=' + r[0].n);
await expectOk('overrides saved', `select kind, to_char(start_time, 'HH24:MI') s from public.spot_date_overrides where spot_id = $1 order by day`, [spotId],
  (r) => r.length === 2 && r[0].kind === 'blocked' && r[1].s === '09:00' ? null : JSON.stringify(r));
await expectOk('spot without sector (Térreo)', saveSpot(null, terreo.level_id, null, '1', null, 3000, null));
await expectError('same number on the same level/sector', saveSpot(null, s2.level_id, s2.sector_id, ' 14', 900, null, null), [], '23505');
await expectError('sector from another level', saveSpot(null, s2.level_id, s1.sector_id, '15', 900, null, null), [], '22023');
await expectError('no price at all', saveSpot(null, s2.level_id, s2.sector_id, '16', null, null, null), [], '23514');
await expectError('invalid time window', saveSpot(null, s2.level_id, s2.sector_id, '17', 900, null, null,
  JSON.stringify([{ weekday: 1, start: '18:00', end: '08:00' }])), [], '23514');
await expectOk('edit replaces availability', saveSpot(spotId, s2.level_id, s2.sector_id, '14', 1000, null, null, '[]', '[]'));
await expectOk('availability replaced', 'select (select count(*) from public.spot_weekly_availability where spot_id = $1)::int w, (select count(*) from public.spot_date_overrides where spot_id = $1)::int o', [spotId],
  (r) => r[0].w === 0 && r[0].o === 0 ? null : JSON.stringify(r));
await expectError('direct insert into spots is blocked', `insert into public.spots (condo_id, level_id, number, price_hour_cents) values ($1, $2, '99', 100)`, [condoId, s2.level_id], 'row-level security');
await expectOk('search counts active spots', `select listed_spots from public.search_condominiums('alameda')`, [], (r) => r[0].listed_spots === 2 ? null : JSON.stringify(r));
await expectOk('pause', `select public.set_spot_status($1, 'paused')`, [spotId]);
await expectOk('owner still sees the paused spot', 'select status from public.spots where id = $1', [spotId], (r) => r[0]?.status === 'paused' ? null : JSON.stringify(r));

await as(U2);
await expectOk('outsider sees no spots', 'select * from public.spots', [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectError('outsider cannot create spots', saveSpot(null, s2.level_id, s2.sector_id, '20', 900, null, null), [], '42501');
await expectOk('u2 joins again', `select public.join_condominium($1, 'A', '12', 'morador', null, null)`, [condoId]);
await expectOk('member sees only active spots', 'select number from public.spots', [], (r) => r.length === 1 && r[0].number === '1' ? null : JSON.stringify(r));
await expectError('member cannot pause someone else\'s spot', `select public.set_spot_status($1, 'active')`, [spotId], 'P0002');
await expectError('member cannot edit someone else\'s spot', saveSpot(spotId, s2.level_id, s2.sector_id, '14', 1, null, null), [], 'P0002');
await expectOk('member creates a spot of their own', saveSpot(null, s1.level_id, s1.sector_id, '4', 700, null, null));
await as('anon');
await expectError('anon cannot save spots', saveSpot(null, s2.level_id, s2.sector_id, '30', 900, null, null), [], 'permission denied');

// --- Fase 3 · características e reservas -----------------------------------------------------
console.log('\n# spot details');
await as(U1);
await expectOk('features, height and directions saved', 'select features, height_cm, directions from public.spots where id = $1', [spotId],
  (r) => r[0].features.join() === 'coberta' && r[0].height_cm === 210 && r[0].directions === 'Terceira depois do elevador' ? null : JSON.stringify(r));
await expectError('unknown feature', saveSpot(null, s2.level_id, s2.sector_id, '40', 900, null, null).replace("'{coberta}'", "'{piscina}'"), [], '23514');
await expectOk('old save_spot signature is gone', `select count(*)::int n from pg_proc where proname = 'save_spot'`, [], (r) => r[0].n === 1 ? null : 'n=' + r[0].n);

console.log('\n# bookings');
// A Monday far enough ahead that "now" never catches up; times in Brasília (-03:00).
const monday = (() => { const d = new Date(Date.UTC(2030, 9, 7)); while (d.getUTCDay() !== 1) d.setUTCDate(d.getUTCDate() + 1); return d; })();
const dayAt = (offset, hhmm) => {
  const d = new Date(monday); d.setUTCDate(d.getUTCDate() + offset);
  return `${d.toISOString().slice(0, 10)}T${hhmm}:00-03:00`;
};
const dateOf = (offset) => dayAt(offset, '00:00').slice(0, 10);
const s2c = garage.find((g) => g.name === 'Subsolo 2' && g.sector === 'C');
const s2a = garage.find((g) => g.name === 'Subsolo 2' && g.sector === 'A');
const createSpot = async (name, { sector, number, hour = null, day = null, approval, min, weekly, overrides = '[]' }) =>
  (await expectOk(name, `select public.save_spot(null, '${condoId}', '${s2.level_id}', '${sector}', '${number}', null, null,
      '{}', null, null, ${hour ?? 'null'}, ${day ?? 'null'}, null, ${min}, 24, '${approval}', '{}', '${weekly}'::jsonb, '${overrides}'::jsonb) as id`))?.[0]?.id;
// Always open, instant booking, hour or day.
const spotA = await createSpot('spot A: always open, auto', {
  sector: s2a.sector_id, number: '30', hour: 800, day: 3500, approval: 'auto', min: 120,
  weekly: JSON.stringify([1, 2, 3, 4, 5, 6, 7].map((d) => ({ weekday: d, start: '00:00', end: '24:00' }))),
});
// Weekdays 08–18, approval, hour only; Wednesday blocked, Saturday opened 09–12.
const spotM = await createSpot('spot M: weekdays, manual', {
  sector: s2c.sector_id, number: '31', hour: 1000, approval: 'manual', min: 60,
  weekly: JSON.stringify([1, 2, 3, 4, 5].map((d) => ({ weekday: d, start: '08:00', end: '18:00' }))),
  overrides: JSON.stringify([{ day: dateOf(2), kind: 'blocked' }, { day: dateOf(5), kind: 'open', start: '09:00', end: '12:00' }]),
});

const search = (start, end) => `select id, available, is_mine, owner_name, owner_block, weekly from public.search_spots('${condoId}', '${start}', '${end}')`;
const availableIn = (rows, id) => rows.find((r) => r.id === id)?.available;
await as(U2);
let rows = await expectOk('search as member', search(dayAt(0, '09:00'), dayAt(0, '11:00')));
if (availableIn(rows, spotA) === true && availableIn(rows, spotM) === true) ok('both free on Monday 09–11'); else fail('both free on Monday 09–11', JSON.stringify(rows));
const a = rows.find((r) => r.id === spotA);
if (a.owner_name === 'Adam Foerster' && a.owner_block === 'B' && a.is_mine === false && a.weekly.length === 7) ok('owner name/block and weekly rule'); else fail('owner info', JSON.stringify(a));
if (!rows.some((r) => r.id === spotId)) ok('paused spot is not listed'); else fail('paused spot listed', '');
rows = await expectOk('search Monday 07–09', search(dayAt(0, '07:00'), dayAt(0, '09:00')));
if (availableIn(rows, spotM) === false) ok('outside the weekly window'); else fail('outside window', JSON.stringify(rows));
rows = await expectOk('search Wednesday (blocked)', search(dayAt(2, '09:00'), dayAt(2, '11:00')));
if (availableIn(rows, spotM) === false) ok('blocked date'); else fail('blocked date', '');
rows = await expectOk('search Saturday 09–12 (opened date)', search(dayAt(5, '09:00'), dayAt(5, '12:00')));
if (availableIn(rows, spotM) === true) ok('opened Saturday'); else fail('opened Saturday', '');
rows = await expectOk('search Saturday 08–12', search(dayAt(5, '08:00'), dayAt(5, '12:00')));
if (availableIn(rows, spotM) === false) ok('opened Saturday only 09–12'); else fail('opened Saturday window', '');
rows = await expectOk('search overnight Monday 17 → Tuesday 09', search(dayAt(0, '17:00'), dayAt(1, '09:00')));
if (availableIn(rows, spotM) === false && availableIn(rows, spotA) === true) ok('overnight needs the night open'); else fail('overnight', JSON.stringify(rows));
rows = await expectOk('search 30 min', search(dayAt(0, '09:00'), dayAt(0, '09:30')));
if (availableIn(rows, spotM) === false) ok('below the minimum is not available'); else fail('below minimum', '');

const vehicle = (await db.query('select id from public.vehicles where owner_id = $1', [U2])).rows[0].id;
const book = (spot, start, end, unit, veh = vehicle) =>
  `select * from public.request_booking('${spot}', '${start}', '${end}', '${unit}', ${veh ? `'${veh}'` : 'null'}, 'Chego cedo')`;
const instant = (await expectOk('instant booking Saturday 08 → Sunday 18 (days)', book(spotA, dayAt(5, '08:00'), dayAt(6, '18:00'), 'day'), [],
  (r) => r[0].status === 'confirmed' && r[0].total_cents === 7000 && Number(r[0].code) >= 1001 ? null : JSON.stringify(r)))?.[0];
await expectError('overlapping instant booking', book(spotA, dayAt(6, '10:00'), dayAt(6, '12:00'), 'hour'), [], 'spot_unavailable');
rows = await expectOk('search after the booking', search(dayAt(6, '10:00'), dayAt(6, '12:00')));
if (availableIn(rows, spotA) === false) ok('booked period is busy'); else fail('booked period busy', '');
await expectOk('busy ranges for the calendar', `select * from public.spot_busy_ranges('${spotA}', '${dayAt(0, '00:00')}', '${dayAt(7, '00:00')}')`, [],
  (r) => r.length === 1 ? null : 'rows=' + r.length);
await expectOk('request with approval is pending', book(spotM, dayAt(0, '09:00'), dayAt(0, '11:00'), 'hour'), [],
  (r) => r[0].status === 'pending' && r[0].total_cents === 2000 ? null : JSON.stringify(r));
await expectOk('pending requests do not block each other', book(spotM, dayAt(0, '10:00'), dayAt(0, '12:00'), 'hour'), [],
  (r) => r[0].status === 'pending' ? null : JSON.stringify(r));
await expectOk('pending has a response deadline', `select respond_by is not null ok from public.bookings where status = 'pending' limit 1`, [], (r) => r[0].ok ? null : 'no deadline');
await expectError('below the minimum', book(spotM, dayAt(0, '09:00'), dayAt(0, '09:30'), 'hour'), [], 'below_minimum');
await expectError('unit not offered', book(spotM, dayAt(0, '09:00'), dayAt(0, '11:00'), 'day'), [], 'unit_not_offered');
await expectError('outside availability', book(spotM, dayAt(2, '09:00'), dayAt(2, '11:00'), 'hour'), [], 'spot_unavailable');
await expectError('someone else\'s vehicle', book(spotM, dayAt(3, '09:00'), dayAt(3, '11:00'), 'hour', '00000000-0000-0000-0000-000000000001'), [], 'invalid_vehicle');
await expectError('period in the past', book(spotA, '2020-01-01T08:00:00-03:00', '2020-01-01T12:00:00-03:00', 'hour'), [], 'invalid_period');
await expectError('paused spot', book(spotId, dayAt(0, '09:00'), dayAt(0, '11:00'), 'hour'), [], 'spot_unavailable');
await expectError('direct insert into bookings is blocked', `insert into public.bookings (spot_id, condo_id, owner_id, renter_id, starts_at, ends_at, billing_unit, units, unit_price_cents, total_cents)
  values ($1, $2, $3, $4, now() + interval '1 day', now() + interval '2 days', 'day', 1, 1, 1)`, [spotA, condoId, U1, U2], 'row-level security');
await expectOk('renter sees own bookings', 'select count(*)::int n from public.bookings', [], (r) => r[0].n === 3 ? null : 'n=' + r[0].n);

await as(U1);
await expectError('owner cannot book own spot', book(spotA, dayAt(3, '09:00'), dayAt(3, '11:00'), 'hour', null), [], 'own_spot');
await expectOk('owner sees requests for their spots', 'select count(*)::int n from public.bookings', [], (r) => r[0].n === 3 ? null : 'n=' + r[0].n);
await expectOk('owner sees the renter\'s vehicle', 'select plate from public.vehicles where id = $1', [vehicle], (r) => r[0]?.plate === 'ABC1D23' ? null : JSON.stringify(r));
await expectOk('search marks own spots', search(dayAt(0, '09:00'), dayAt(0, '11:00')), [], (r) => r.find((x) => x.id === spotA)?.is_mine === true ? null : 'not mine');

const U3 = '33333333-3333-3333-3333-333333333333';
await db.exec(`reset role; insert into auth.users (id, email) values ('${U3}', 'outro@x.com');`);
await as(U3);
await expectOk('outsider sees no bookings', 'select * from public.bookings', [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectOk('outsider search is empty', search(dayAt(0, '09:00'), dayAt(0, '11:00')), [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectOk('outsider gets no busy ranges', `select * from public.spot_busy_ranges('${spotA}', '${dayAt(0, '00:00')}', '${dayAt(7, '00:00')}')`, [],
  (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectError('outsider cannot book', book(spotA, dayAt(3, '09:00'), dayAt(3, '11:00'), 'hour', null), [], 'spot_unavailable');
await as('anon');
await expectError('anon cannot search spots', search(dayAt(0, '09:00'), dayAt(0, '11:00')), [], 'permission denied');

await db.exec('reset role');
await expectError('exclusion constraint blocks overlapping confirmations', `insert into public.bookings
  (spot_id, condo_id, owner_id, renter_id, starts_at, ends_at, billing_unit, units, unit_price_cents, total_cents, status)
  values ('${spotA}', '${condoId}', '${U1}', '${U2}', '${dayAt(6, '09:00')}', '${dayAt(6, '10:00')}', 'hour', 1, 800, 800, 'confirmed')`, [], '23P01');
if (instant) ok('instant booking code ' + instant.code);

// --- Fase 4: ciclo da reserva -----------------------------------------------------------------
console.log('\n# booking lifecycle');
const approve = (id) => `select public.approve_booking('${id}')`;
const cancel = (id) => `select public.cancel_booking('${id}')`;
const checkIn = (id) => `select public.check_in('${id}')`;
const checkOut = (id) => `select public.check_out('${id}')`;
const extend = (id, end) => `select * from public.extend_booking('${id}', ${end})`;
const statusOf = async (id) => (await db.query('select status from public.my_bookings() where id = $1', [id])).rows[0]?.status;

await as(U2);
const renterRows = await expectOk('renter lists their bookings', 'select * from public.my_bookings()', [],
  (r) => r.length === 3 && r.every((x) => x.role === 'renter') ? null : JSON.stringify(r.map((x) => x.role)));
const instantRow = renterRows.find((r) => r.status === 'confirmed');
if (instantRow.counterpart_name === 'Adam Foerster' && instantRow.counterpart_block === 'B' && instantRow.vehicle_plate === 'ABC1D23'
  && instantRow.spot_number === '30' && instantRow.condo_name === 'Residencial Alameda Verde' && instantRow.cancel_notice_hours === 24)
  ok('renter row has owner, vehicle, spot and condo'); else fail('renter row', JSON.stringify(instantRow));
const [p1, p2] = renterRows.filter((r) => r.status === 'pending').sort((x, y) => x.starts_at - y.starts_at).map((r) => r.id);
await expectError('renter cannot approve', approve(p1), [], 'booking_not_found');
await expectError('check-in only on the day', checkIn(instantRow.id), [], 'check_in_closed');
await expectError('check-out needs a check-in', checkOut(instantRow.id), [], 'invalid_state');

await as(U1);
let ownerRows = await expectOk('owner lists the requests', 'select * from public.my_bookings()', [],
  (r) => r.length === 3 && r.every((x) => x.role === 'owner') && r.every((x) => x.counterpart_name && x.vehicle_plate === 'ABC1D23') ? null : JSON.stringify(r));
await expectOk('approve the first request', approve(p1));
ownerRows = await expectOk('the overlapping request shows the conflict', 'select * from public.my_bookings()', [],
  (r) => r.find((x) => x.id === p2)?.conflict_starts_at && !r.find((x) => x.id === p1)?.conflict_starts_at ? null : JSON.stringify(r));
await expectError('approving the overlapping request', approve(p2), [], 'conflict');
await expectError('approving twice', approve(p1), [], 'invalid_state');
await expectOk('reject with reason and message', `select public.reject_booking($1, 'visita', ' Posso liberar outro dia. ')`, [p2]);
await expectOk('rejection is stored', 'select reject_reason, reject_message from public.my_bookings() where id = $1', [p2],
  (r) => r[0].reject_reason === 'visita' && r[0].reject_message === 'Posso liberar outro dia.' ? null : JSON.stringify(r));
await expectError('rejecting a rejected request', `select public.reject_booking($1, 'outro', null)`, [p2], 'invalid_state');
await expectError('owner cannot check in', checkIn(p1), [], 'booking_not_found');
await expectError('owner cannot extend', extend(p1, `'${dayAt(0, '12:00')}'`), [], 'booking_not_found');

await as(U2);
await expectOk('extend into free time (recalculated)', extend(instantRow.id, `'${dayAt(7, '10:00')}'`), [],
  (r) => r[0].units === 3 && r[0].total_cents === 10500 ? null : JSON.stringify(r));
await expectError('extend backwards', extend(instantRow.id, `'${dayAt(6, '12:00')}'`), [], 'invalid_period');
await expectError('extend beyond the weekly window', extend(p1, `'${dayAt(0, '19:00')}'`), [], 'spot_unavailable');
await expectOk('renter cancels ahead of the notice', cancel(instantRow.id));
await expectOk('cancelled by the renter', 'select status, cancelled_by_owner from public.my_bookings() where id = $1', [instantRow.id],
  (r) => r[0].status === 'cancelled' && r[0].cancelled_by_owner === false ? null : JSON.stringify(r));
await expectError('cancelling twice', cancel(instantRow.id), [], 'invalid_state');

await as(U1);
await expectOk('owner cancels a confirmed booking', cancel(p1));
await as(U2);
await expectOk('renter sees it was the owner', 'select cancelled_by_owner from public.my_bookings() where id = $1', [p1],
  (r) => r[0].cancelled_by_owner === true ? null : JSON.stringify(r));

// A booking starting in 10 minutes (inserted directly, as the RPC would).
await db.exec('reset role');
const live = (await db.query(`insert into public.bookings
  (spot_id, condo_id, owner_id, renter_id, vehicle_id, starts_at, ends_at, billing_unit, units, unit_price_cents, total_cents, status)
  values ($1, $2, $3, $4, $5, now() + interval '10 minutes', now() + interval '3 hours', 'hour', 3, 800, 2400, 'confirmed') returning id`,
  [spotA, condoId, U1, U2, vehicle])).rows[0].id;
await as(U2);
await expectError('cancel after the notice window', cancel(live), [], 'cancel_window_closed');
await expectOk('check-in 10 min before the start', checkIn(live));
if (await statusOf(live) === 'in_progress') ok('in progress after check-in'); else fail('in progress after check-in', await statusOf(live));
await expectError('check-in twice', checkIn(live), [], 'invalid_state');
await expectOk('extend while parked', extend(live, `now() + interval '4 hours'`), [], (r) => r[0].units === 4 ? null : JSON.stringify(r));
await as(U3);
await expectOk('outsider has no bookings', 'select * from public.my_bookings()', [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectError('outsider cannot check out', checkOut(live), [], 'booking_not_found');
await as(U2);
await expectOk('check-out', checkOut(live));
if (await statusOf(live) === 'completed') ok('completed after check-out'); else fail('completed after check-out', await statusOf(live));

// Settling: an unanswered request past its deadline and a confirmed booking already over.
await db.exec('reset role');
const stale = (await db.query(`insert into public.bookings
  (spot_id, condo_id, owner_id, renter_id, starts_at, ends_at, billing_unit, units, unit_price_cents, total_cents, status, respond_by)
  values ($1, $2, $3, $4, now() + interval '2 days', now() + interval '2 days 2 hours', 'hour', 2, 1000, 2000, 'pending', now() - interval '1 minute') returning id`,
  [spotM, condoId, U1, U2])).rows[0].id;
const over = (await db.query(`insert into public.bookings
  (spot_id, condo_id, owner_id, renter_id, starts_at, ends_at, billing_unit, units, unit_price_cents, total_cents, status)
  values ($1, $2, $3, $4, now() - interval '5 hours', now() - interval '1 hour', 'hour', 4, 800, 3200, 'confirmed') returning id`,
  [spotA, condoId, U1, U2])).rows[0].id;
await as(U2);
if (await statusOf(stale) === 'expired') ok('unanswered request expires'); else fail('unanswered request expires', await statusOf(stale));
if (await statusOf(over) === 'completed') ok('finished booking completes'); else fail('finished booking completes', await statusOf(over));
await as(U1);
await expectError('owner cannot approve an expired request', approve(stale), [], 'invalid_state');
await expectError('settle_bookings is internal', 'select public.settle_bookings()', [], 'permission denied');
await as('anon');
await expectError('anon cannot list bookings', 'select * from public.my_bookings()', [], 'permission denied');
await expectError('anon cannot cancel', cancel(live), [], 'permission denied');
await db.exec('reset role');

// --- Fase 5: chat e notificações ------------------------------------------------------------
console.log('\n# messages and notifications');
const say = (booking, sender, body, kind = 'text') =>
  `insert into public.messages (booking_id, sender_id, kind, body) values ('${booking}', ${sender ? `'${sender}'` : 'null'}, '${kind}', '${body}')`;
const notes = (where = 'true') => `select kind, title, body, read_at, booking_id from public.notifications where ${where} order by created_at`;

await as(U2);
const chat = (await expectOk('a new request', book(spotM, dayAt(3, '09:00'), dayAt(3, '11:00'), 'hour')))?.[0]?.id;
await expectOk('the note opens the conversation', 'select kind, body, sender_id from public.messages where booking_id = $1', [chat],
  (r) => r.length === 1 && r[0].kind === 'text' && r[0].body === 'Chego cedo' && r[0].sender_id === U2 ? null : JSON.stringify(r));

await as(U1);
await expectOk('owner is notified of the request', notes(`booking_id = '${chat}'`), [],
  (r) => r.length === 1 && r[0].kind === 'request' && r[0].title === 'Marina R. pediu a vaga C2-31' && r[0].body.includes('Responda até')
    ? null : JSON.stringify(r));
await expectOk('owner answers in the chat', say(chat, U1, 'Pode vir, a vaga é a terceira.'));
await expectError('nobody writes as someone else', say(chat, U2, 'Fingindo ser a Marina'), [], 'row-level security');
await expectError('users cannot write system messages', say(chat, null, 'CHECK-IN · 08:00', 'system'), [], 'row-level security');
await db.query(`update public.messages set body = 'editada' where booking_id = '${chat}'`);
await expectOk('messages cannot be edited', `select count(*)::int n from public.messages where body = 'editada'`, [], (r) => r[0].n === 0 ? null : 'edited');

await as(U3);
await expectOk('outsider reads no messages', `select * from public.messages where booking_id = '${chat}'`, [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectError('outsider cannot write', say(chat, U3, 'Oi'), [], 'row-level security');
await expectOk('outsider has no conversations', 'select * from public.my_conversations()', [], (r) => r.length === 0 ? null : 'rows=' + r.length);

await as(U2);
await expectOk('renter conversation with the owner reply unread', 'select * from public.my_conversations() where booking_id = $1', [chat],
  (r) => r[0]?.unread === 1 && r[0].last_body === 'Pode vir, a vaga é a terceira.' && r[0].last_mine === false
    && r[0].counterpart_name === 'Adam Foerster' && r[0].spot_label === 'C2-31' && r[0].role === 'renter' ? null : JSON.stringify(r));
await expectOk('mark the conversation read', `select public.mark_messages_read('${chat}')`);
await expectOk('nothing unread', 'select unread from public.my_conversations() where booking_id = $1', [chat], (r) => r[0].unread === 0 ? null : JSON.stringify(r));
await as(U1);
await expectOk('owner still has the note unread', 'select unread from public.my_conversations() where booking_id = $1', [chat], (r) => r[0].unread === 1 ? null : JSON.stringify(r));
await expectOk('approve', approve(chat));
await as(U2);
await expectOk('renter is notified of the approval', notes(`booking_id = '${chat}'`), [],
  (r) => r.length === 1 && r[0].kind === 'approved' && r[0].title === 'Sua reserva da vaga C2-31 foi aprovada' ? null : JSON.stringify(r));
await expectOk('system message in the chat', `select body from public.messages where booking_id = '${chat}' and kind = 'system'`, [],
  (r) => r.length === 1 && r[0].body === 'RESERVA CONFIRMADA' ? null : JSON.stringify(r));
await expectOk('earlier rejection was notified with the reason', notes(`kind = 'rejected'`), [],
  (r) => r.length === 1 && r[0].title === 'Adam F. recusou a vaga C2-31' && r[0].body.startsWith('Motivo: vaga reservada para visita') ? null : JSON.stringify(r));
await expectOk('owner cancellation was notified', notes(`kind = 'cancelled' and booking_id = '${p1}'`), [],
  (r) => r.length === 1 && r[0].title === 'Adam F. cancelou a reserva da vaga C2-31' ? null : JSON.stringify(r));
await expectOk('unanswered request was notified', notes(`kind = 'expired'`), [], (r) => r.length === 1 ? null : JSON.stringify(r));
await expectOk('check-in, extension and check-out are in the chat', `select body from public.messages where booking_id = '${live}' and kind = 'system' order by created_at`, [],
  (r) => r.length === 3 && r[0].body.startsWith('CHECK-IN · ') && r[1].body.startsWith('SAÍDA AJUSTADA PARA ') && r[2].body.startsWith('CHECK-OUT · ')
    ? null : JSON.stringify(r));
await expectOk('renter only sees their notifications', notes(), [], (r) => r.length > 0 && !r.some((x) => x.kind === 'request' || x.kind === 'booked') ? null : JSON.stringify(r.map((x) => x.kind)));
await expectOk('mark notifications read', `select public.mark_notifications_read(null)`);
await expectOk('all read', notes('read_at is null'), [], (r) => r.length === 0 ? null : 'unread=' + r.length);
await as(U1);
await expectOk('marking read is per user', notes('read_at is null'), [], (r) => r.length > 0 ? null : 'owner lost unread');
await expectOk('mark one condominium read', `select public.mark_notifications_read('${condoId}')`);
await expectOk('owner notifications read', notes('read_at is null'), [], (r) => r.length === 0 ? null : 'unread=' + r.length);
await expectError('notifications cannot be inserted directly', `insert into public.notifications (user_id, condo_id, kind, title, body) values ('${U1}', '${condoId}', 'late', 'x', 'y')`, [], 'row-level security');

// Reminder (check-in within 24 h) and lateness (15 min past the exit), once each.
await db.exec('reset role');
const soonId = (await db.query(`insert into public.bookings
  (spot_id, condo_id, owner_id, renter_id, starts_at, ends_at, billing_unit, units, unit_price_cents, total_cents, status)
  values ($1, $2, $3, $4, now() + interval '3 hours', now() + interval '5 hours', 'hour', 2, 800, 1600, 'confirmed') returning id`,
  [spotA, condoId, U1, U2])).rows[0].id;
const lateId = (await db.query(`insert into public.bookings
  (spot_id, condo_id, owner_id, renter_id, starts_at, ends_at, billing_unit, units, unit_price_cents, total_cents, status, checked_in_at)
  values ($1, $2, $3, $4, now() - interval '3 hours', now() - interval '20 minutes', 'hour', 3, 800, 2400, 'in_progress', now() - interval '3 hours') returning id`,
  [spotA, condoId, U1, U2])).rows[0].id;
await as(U2);
await db.query('select * from public.my_bookings()');
await db.query('select * from public.my_bookings()');
await expectOk('one check-in reminder per booking', notes(`kind = 'reminder' and booking_id = '${soonId}'`), [],
  (r) => r.length === 1 && r[0].booking_id === soonId && r[0].title.startsWith('Check-in libera ') ? null : JSON.stringify(r));
await as(U1);
await expectOk('one lateness warning for the owner', notes(`kind = 'late'`), [],
  (r) => r.length === 1 && r[0].booking_id === lateId && r[0].title === 'Marina R. passou do horário na vaga A2-30' ? null : JSON.stringify(r));

await as('anon');
await expectOk('anon reads no messages', 'select * from public.messages', [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectOk('anon reads no notifications', 'select * from public.notifications', [], (r) => r.length === 0 ? null : 'rows=' + r.length);
await expectError('anon has no conversations', 'select * from public.my_conversations()', [], 'permission denied');
await expectError('anon cannot mark read', `select public.mark_notifications_read(null)`, [], 'permission denied');
await db.exec('reset role');
if (hasPublication) {
  await expectOk('tables published for Realtime', `select tablename from pg_publication_tables where pubname = 'supabase_realtime' order by tablename`, [],
    (r) => r.map((x) => x.tablename).join(',') === 'messages,notifications' ? null : JSON.stringify(r));
} else {
  console.log('  skip Realtime publication (not supported by PGlite)');
}

console.log(failures ? `\n${failures} FAILURE(S)` : '\nALL PASSED');
process.exit(failures ? 1 : 0);
