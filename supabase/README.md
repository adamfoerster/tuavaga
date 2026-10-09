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
| `20261011000000_booking_lifecycle.sql` | ciclo da reserva: colunas de cancelamento e check-in/out, `my_bookings` (lista do locatário e do locador) e as RPCs `approve_booking`, `reject_booking`, `cancel_booking`, `check_in`, `check_out`, `extend_booking` |
| `20261012000000_messages_notifications.sql` | chat (`messages`) e notificações (`notifications`) com RLS, gatilho que gera avisos e mensagens de sistema a cada mudança da reserva, lembrete de check-in e aviso de atraso, as RPCs `my_conversations`, `mark_messages_read`, `mark_notifications_read`, e a publicação no Realtime |
| `20261013000000_account.sql` | perfil: trava para remover veículo em uso, `leave_condominium` e `delete_own_account` |

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

Regras do ciclo da reserva:

- Cada mudança de estado é uma RPC que confere o papel e a transição (erro `booking_not_found` para quem
  não é a parte certa, `invalid_state` quando a reserva já mudou):
  - locador: `approve_booking` (falha com `conflict` se já houver reserva confirmada no período) e
    `reject_booking` (motivo `visita`/`uso`/`veiculo`/`outro` + mensagem opcional);
  - `cancel_booking`: o locatário cancela um pedido a qualquer momento e uma reserva confirmada só até
    `cancel_notice_hours` antes da entrada (`cancel_window_closed`); o locador cancela uma confirmada
    até a entrada;
  - locatário: `check_in` (de 30 min antes da entrada até a saída, senão `check_in_closed`),
    `check_out` e `extend_booking` (nova saída livre e dentro da disponibilidade, até 7 dias a mais; o
    valor é recalculado na mesma forma de cobrança).
- `my_bookings` devolve as reservas em que o usuário é locatário ou locador, com vaga, condomínio, nome,
  bloco e unidade da outra parte, veículo e, para pedidos pendentes, a reserva confirmada que conflita.
- Pedidos sem resposta no prazo expiram, reservas confirmadas que passaram terminam e reservas em curso
  terminam 12 h depois da saída sem check-out. Isso acontece em `settle_bookings()`, chamada no início de
  `my_bookings` e de cada RPC do ciclo, então não depende de agendador. Para manter o banco em dia mesmo
  sem uso do app, habilite **pg_cron** (Database → Extensions) e agende, no SQL Editor:

  ```sql
  select cron.schedule('settle-bookings', '*/5 * * * *', 'select public.settle_bookings()');
  ```

Regras do perfil e da conta:

- Veículo usado por reserva pendente, confirmada ou em curso não pode ser removido (`vehicle_in_use`,
  gatilho `vehicles_guard`); editar continua livre.
- `leave_condominium` recusa a saída com reserva pendente, confirmada ou em curso naquele condomínio
  (`active_bookings`); ao sair, as vagas do usuário ali ficam pausadas.
- `delete_own_account` recusa com reserva em curso (`booking_in_progress`); senão cancela as reservas
  futuras (a outra parte é notificada), apaga as reservas do usuário e o próprio `auth.users`, que leva em
  cascata perfil, vínculos, veículos, vagas e notificações. As notificações da outra parte ficam (sem o
  link para a reserva apagada).

Regras do chat e das notificações:

- Mensagens: só locatário e locador da reserva leem e escrevem; o insert é direto na tabela, sempre
  com `sender_id` = o próprio usuário e `kind = 'text'`. Não há edição nem exclusão. Mensagens de
  sistema ("CHECK-IN · 08:02", "RESERVA CONFIRMADA"…) e a observação do pedido (primeira mensagem) vêm do
  gatilho `bookings_events`.
- Notificações: cada usuário só lê as suas; nascem do gatilho (pedido, reserva confirmada na hora,
  aprovada, recusada com o motivo, cancelada, sem resposta) e de `settle_bookings` (lembrete na véspera do
  check-in e atraso 15 min após a saída, uma vez por reserva). "Marcar como lidas" é
  `mark_notifications_read` (todas ou de um condomínio).
- Realtime: a migration adiciona `messages` e `notifications` à publicação `supabase_realtime` (se ela
  existir). Confira em **Database → Publications** que as duas tabelas estão marcadas; o Realtime aplica a
  RLS, então cada usuário só recebe o que pode ler.

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
