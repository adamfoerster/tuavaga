# Supabase

## 1. Banco

Rode as migrations **em ordem** no SQL Editor do projeto (ou `supabase db push` com a CLI):

| Migration | O que cria |
| --- | --- |
| `20261007000000_profiles.sql` | `public.profiles` com RLS e o trigger que preenche o perfil no cadastro |
| `20261008000000_condominiums.sql` | condomínios, andares/setores da garagem, vínculos (`memberships`), veículos, RLS e as RPCs `search_condominiums`, `find_condominium_by_invite`, `condominium_blocks`, `join_condominium`, `create_condominium` |
| `20261009000000_spots.sql` | vagas (`spots`), janela semanal (`spot_weekly_availability`), exceções por data (`spot_date_overrides`), RLS e as RPCs `save_spot` e `set_spot_status`; a busca e o convite passam a contar as vagas ativas |
| `20261010000000_spot_details.sql` | características (`features`), pé-direito (`height_cm`) e "como chegar" (`directions`) da vaga; `save_spot` passa a recebê-los |
| `20261010000100_bookings.sql` | extensão `btree_gist`, reservas (`bookings`) com código sequencial e trava contra sobreposição, RLS e as RPCs `search_spots`, `spot_busy_ranges` e `request_booking` |

Regras da fase de condomínios:

- Só membros leem o condomínio e a garagem. Quem ainda não entrou usa as RPCs de busca e de convite,
  que não expõem membros nem o código de convite.
- O vínculo só nasce por `join_condominium` / `create_condominium` (insert direto é bloqueado pela RLS);
  as duas também atualizam nome e telefone em `profiles`.
- Veículos são do usuário (não do condomínio) e só o dono os vê.
- As RPCs só podem ser chamadas por usuários logados (`anon` não tem `execute`).

Regras da fase de vagas:

- Membros do condomínio leem as vagas **ativas** e a disponibilidade delas; o dono lê todas as suas
  (inclusive pausadas).
- Vaga só é criada ou editada por `save_spot`, que confere o vínculo com o condomínio e se andar e setor
  são dele; a edição substitui a disponibilidade inteira. Pausar/reativar é `set_spot_status`.
- Um número por andar/setor (`spots_place_unique`); pelo menos um preço (hora, dia ou semana).

Regras da fase de reservas (pedido):

- `btree_gist` é criada no schema `extensions` (já existe no Supabase); se o painel recusar, habilite-a
  em **Database → Extensions** e rode a migration de novo.
- Reserva só nasce por `request_booking`, que confere vínculo, vaga ativa, que a vaga não é sua, veículo
  do solicitante, entrada no futuro, período mínimo, forma de cobrança oferecida e disponibilidade
  (janelas semanais, exceções e reservas confirmadas). O valor é calculado no banco (unidades
  arredondadas para cima). Erros chegam como mensagem: `spot_unavailable`, `own_spot`, `invalid_period`,
  `invalid_vehicle`, `below_minimum`, `unit_not_offered`.
- Vaga com aprovação automática vira `confirmed` na hora; manual fica `pending` com `respond_by` em 12 h.
- Duas reservas `confirmed`/`in_progress` da mesma vaga nunca se sobrepõem (`bookings_no_overlap`);
  pedidos pendentes podem conflitar até o locador decidir (fase 4).
- Só locatário e locador leem a reserva; o locador também vê o veículo dela. `spot_busy_ranges` só
  devolve os horários ocupados, sem dizer quem reservou.

### Testes das migrations

`supabase/tests` aplica todas as migrations num Postgres em memória (PGlite), com um schema `auth`
simulado, e confere RLS e RPCs com dois usuários. Não precisa de Docker nem de projeto Supabase:

```bash
npm --prefix supabase/tests ci
npm --prefix supabase/tests test
```

Toda migration nova ganha casos em `supabase/tests/migrations.test.mjs`.

## 2. Auth → Providers → Email

- **Enable Email provider**: ligado.
- **Confirm email**: recomendado ligado. O app trata os dois casos (com confirmação abre a tela de código; sem confirmação entra direto).
- **Minimum password length**: 6 (o app valida o mesmo valor em `CredentialsValidator.MIN_PASSWORD_LENGTH`; se mudar aqui, mude lá).
- **Email OTP length**: 8 (`CredentialsValidator.OTP_LENGTH`; se mudar aqui, mude lá).

## 3. Auth → Email Templates

O app usa **código de 8 dígitos**, não link. Os templates precisam exibir `{{ .Token }}`:

**Confirm signup**

```html
<h2>Confirme seu cadastro no TuaVaga</h2>
<p>Seu código de confirmação é:</p>
<p style="font-size:24px;font-weight:bold;letter-spacing:4px">{{ .Token }}</p>
```

**Reset password**

```html
<h2>Recuperação de senha do TuaVaga</h2>
<p>Seu código para redefinir a senha é:</p>
<p style="font-size:24px;font-weight:bold;letter-spacing:4px">{{ .Token }}</p>
<p>Se você não pediu isso, ignore este e-mail.</p>
```

## 4. SMTP

O SMTP padrão do Supabase tem limite baixo de envios por hora (só para testes).
Para produção configure um SMTP próprio em **Project Settings → Auth → SMTP Settings**.

## 5. Chaves no app

Em **Project Settings → API** copie a *Project URL* e a *anon/publishable key* para o `local.properties`
na raiz do repositório (veja `local.properties.example`). Nunca coloque a `service_role` key no app.
