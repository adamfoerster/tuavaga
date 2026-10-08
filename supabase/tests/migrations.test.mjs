// Applies every migration to an in-memory Postgres (PGlite) with stand-ins for Supabase's `auth`
// schema and roles, then checks RLS and the RPCs as two different users.
// Run: npm --prefix supabase/tests ci && npm --prefix supabase/tests test
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { PGlite } from '@electric-sql/pglite';

const migrationsDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'migrations');
const db = new PGlite();

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
  create schema auth;
  create table auth.users (id uuid primary key, email text, raw_user_meta_data jsonb default '{}'::jsonb);
  create function auth.uid() returns uuid language sql stable as
    $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
  grant usage on schema public, auth to anon, authenticated;
  grant execute on function auth.uid() to anon, authenticated;
  alter default privileges in schema public grant all on tables to anon, authenticated;
  alter default privileges in schema public grant usage on types to anon, authenticated;
`);

// --- Migrations --------------------------------------------------------------------------------
const files = readdirSync(migrationsDir).filter((f) => f.endsWith('.sql')).sort();
for (const f of files) {
  try { await db.exec(readFileSync(join(migrationsDir, f), 'utf8')); ok('apply ' + f); }
  catch (e) { fail('apply ' + f, e.message); process.exit(1); }
}
const condoFile = files.find((f) => f.includes('condominiums'));
try { await db.exec(readFileSync(join(migrationsDir, condoFile), 'utf8')); ok('re-apply ' + condoFile + ' (idempotent)'); }
catch (e) { fail('re-apply ' + condoFile, e.message); }

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

console.log(failures ? `\n${failures} FAILURE(S)` : '\nALL PASSED');
process.exit(failures ? 1 : 0);
