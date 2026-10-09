# Plano — implementar o design `tuavaga.html` em fases

| Fase | Versão | Estado |
| --- | --- | --- |
| 0 · Design system Kerb + auth | 0.2.0 | concluída |
| 1 · Condomínios e cadastro do morador | 0.3.0 | concluída |
| 2 · Locador: minhas vagas e cadastro de vaga | 0.4.0 | concluída |
| 3 · Explorar e pedir reserva | 0.5.0 | concluída |
| 4 · Ciclo da reserva | 0.6.0 | concluída |
| 5 · Chat e notificações em tempo real | 0.7.0 | concluída |
| 6 · Perfil | 0.8.0 | concluída |

## Contexto

`tuavaga.html` é um bundle de design com 30 pranchas (390 px, tema escuro + claro) no sistema visual
**Kerb**: fontes Barlow / Barlow Condensed (itálico, caixa alta) / JetBrains Mono, cantos chanfrados,
cor de destaque `apex #f4b91a`, tons `go/info/caution/danger/volt`. Cobre todo o produto:
introdução, login, entrar/criar condomínio, cadastro do morador + veículo, troca de condomínio,
explorar (lista + mapa da garagem), detalhe da vaga, montar/resumir reserva, reservas (3 abas),
detalhe da reserva, check-in/check-out, minhas vagas, cadastro de vaga em 3 passos, solicitações do
locador, agenda da vaga, chat da reserva, notificações, perfil, tab bar, seletor de condomínio e
estados de borda.

Hoje o app só tem auth (Material 3 sem estilo) + `feature/home` placeholder, e o Supabase só tem
`public.profiles`. O objetivo é chegar ao app completo do design em fases entregáveis, cada uma com
a(s) migration(s) Supabase que precisa.

**Decisões já tomadas com o usuário**
- Nome exibido continua **TuaVaga** (onde o design diz "Vaga Vizinha").
- **Fotos adiadas**: telas usam o placeholder listrado do design (`x-ph`); upload fica para a fase final.
- Login **só por e-mail**; a aba "Telefone" do design não é implementada agora.
- Chat e notificações com **Supabase Realtime** (sem push).
- Pagamento: nenhum — o design já diz "acerto direto entre moradores".

O design é um bundle (`tuavaga.html`, 30 pranchas aninhadas e comprimidas). Para ler uma prancha,
descompacte o manifest (base64 + gzip) do HTML raiz e depois o de cada página.

## Regras que valem para TODA fase (CLAUDE.md)

Cada fase é uma entrega fechada: testes em `commonTest` (ViewModels com fakes de repositório,
validadores/cálculos de domínio), README/supabase README/KDoc/CLAUDE.md revisados, entrada no
`CHANGELOG.md`, bump **minor** de `app.version` + `MARKETING_VERSION`, e verde em
`./gradlew verifyVersion testAndroidHostTest :androidApp:assembleDebug :app:wasmJsBrowserDistribution`.
Alteração de entidade Room → sobe `version` do `TuaVagaDatabase`, adiciona `AutoMigration` e versiona
o schema em `core/database/schemas/`. Erros de rede: `Result<T, E>` + `toRemoteError()`
(`core/data/util/RemoteErrors.kt`), capturando `Throwable` e repassando `CancellationException`.
Migrations novas em `supabase/migrations/YYYYMMDDHHMMSS_nome.sql`, idempotentes como a de profiles,
sempre com RLS; regras de negócio sensíveis em funções `security definer` com `set search_path = ''`.

## Arquitetura de módulos (alvo)

```
core/design-system   Kerb: tema, tipografia, componentes (substitui o placeholder Material)
core/domain          + condo/ (Condominium, Membership, ActiveCondoRepository), vehicle/, booking/ (BookingStatus, BookingSummary, BookingPeriod/PriceCalculator)
core/data            + implementações Supabase dos repos de core/domain, Realtime instalado
core/database        + AppPrefsEntity (introSeen, activeCondoId), cache de reservas
core/presentation    + CondoSelectorBar (usa ActiveCondoRepository), formatadores (R$, datas pt-BR)
feature/onboarding   intro, entrar/criar condomínio, cadastro do morador, troca de condomínio
feature/explore      lista, mapa, detalhe da vaga, montar + resumo da reserva
feature/bookings     reservas (abas), detalhe, check-in, check-out
feature/hosting      minhas vagas, wizard de vaga, agenda, solicitações
feature/messages     lista de conversas + chat da reserva
feature/notifications central de notificações
feature/profile      perfil, veículos, condomínios, sair/excluir conta
app                  shell com tab bar (Explorar, Reservas, Minhas vagas, Mensagens, Perfil) e a fiação de navegação entre features
```
`feature/home` é removido na fase 1 (vira o shell do `app`). Features não dependem entre si: o que
duas features usam (condomínio ativo, veículos, status/preço de reserva) fica em `core/*`;
navegação cruzada (ex.: "Enviar solicitação" → detalhe da reserva) é feita por callbacks ligados em
`app/App.kt`. Novas libs: `kotlinx-datetime` (períodos, fuso `America/Sao_Paulo`) e
`supabase realtime-kt` (fase 5).

---

## Fase 0 — Design system Kerb + auth restilizado (0.2.0) · sem migration

- `core/design-system`:
  - Fontes Barlow, Barlow Condensed (itálico 700/800) e JetBrains Mono (OFL) em `composeResources/font`.
  - `KerbColors` dark/light com os tokens exatos do design (`surface #14110e`, `surface-raised`,
    `surface-sunken`, `line`, `ink`, `ink-muted`, `apex`, `danger`, `volt`, `telemetry`, `caution`…),
    `KerbTypography` (`xl/lg/md/sm` display, `bl/b/bs` body, `label`, `dxl/d/ds` mono), espaçamentos
    4–64 e `ChamferShape` (6/10/18 dp). `TuaVagaTheme` passa a expor `KerbTheme` via CompositionLocal
    (mantendo MaterialTheme mapeado para quem ainda usa).
  - Componentes: `KbButton` (primary/ghost/volt, lg, loading), `KbTag` (go/info/caution/danger/neutral),
    `KbPanel`, `KbCard`, `KbField`/`KbPasswordField`, `KbSelect`, `KbToolbar` (segmentado), `KbMeter`
    (progresso "Passo 1 de 3"), `KbReadout` (rótulo + valor mono), `KbChip`, `KbAvatar` (iniciais),
    `KbPhotoPlaceholder` (`x-ph`), `KbZebraStripe`, `KbCheckRow`, `KbScreen` (substitui `FormScaffold`).
    Tab bar, calendário, mapa da garagem e cartão de vaga entram nas fases que os usam (tab bar na fase 1).
- Restilizar login/cadastro/confirmação/recuperação (prancha 19), sem aba Telefone.
- Introdução (prancha 00, "Pular"/"Continuar") exibida uma vez: `AppPrefsEntity.introSeen` no Room
  (DB v2 + AutoMigration + schema `2.json`). Pode morar em `feature/auth` nesta fase ou já criar
  `feature/onboarding` com só a intro.
- Atualizar CLAUDE.md: troca "sem estilização própria até o design chegar" por "use os componentes
  Kerb de `core/design-system`; nunca cores/fontes soltas".
- Testes: ViewModel da intro (marca `introSeen`, pular), mapeamento tom → cor do `KbTag`.

## Fase 1 — Condomínios e cadastro do morador (0.3.0)

**Migration `20261008000000_condominiums.sql`**
- `profiles` + `phone text`, `onboarded_at timestamptz`.
- `condominiums` (id, name, address, cep, timezone default `America/Sao_Paulo`, `invite_code` único
  gerado no formato `XX-XXXX`, created_by, created_at).
- `condo_levels` (id, condo_id, name, position) e `condo_sectors` (id, level_id, name, position) —
  "Subsolo 2 · Setor B".
- `memberships` (user_id, condo_id, block, unit, kind `morador|trabalho`, created_at; PK composta).
- `vehicles` (id, owner_id, plate único por dono, model, color, type `carro|moto|grande`, created_at).
- Helper `is_condo_member(condo uuid)` (`security definer stable`) usado nas policies.
- RLS: condomínio/andares/setores visíveis a membros; perfis visíveis a quem compartilha condomínio
  (nome, bloco, unidade) via view `condo_members` — nunca e-mail/telefone de terceiros; veículos
  visíveis ao dono (e, na fase 3, à contraparte da reserva).
- RPCs: `search_condominiums(q text)` (nome, endereço, nº de torres/vagas anunciadas — sem expor
  membros), `join_condominium(condo_id | invite_code, block, unit, kind)`,
  `create_condominium(name, address, cep, levels jsonb)` (cria e já torna o criador membro),
  `leave_condominium(condo_id)`.

**App**
- `feature/onboarding`: entrar no condomínio (01: busca + código de convite + erro de código
  inválido), cadastrar meu condomínio (20: andares/setores ordenáveis), dados do morador + veículo
  (02: bloco/unidade/telefone, placa Mercosul, modelo, cor, tipo, termos), troca de condomínio (03).
- `core/domain|data`: `CondoRepository`, `VehicleRepository`, `ActiveCondoRepository` (id ativo no
  `AppPrefsEntity`, DB v3).
- `app`: após login, se não há membership → onboarding; senão shell com `KbTabBar` + `CondoSelectorBar`
  (prancha "Seletor de condomínio"). Abas ainda não implementadas mostram o estado vazio do design
  ("Entre num condomínio" etc.). Remove `feature/home`.
- Testes: `PlateValidator` (ABC1D23 e ABC1234), `InviteCodeValidator`, ViewModels de entrar/criar
  condomínio e cadastro do morador, roteamento inicial (sem condomínio → onboarding).

**Como ficou (diferenças em relação ao plano acima)**
- Sem `onboarded_at` e sem a view `condo_members`: nenhuma tela da fase 1 mostra outros moradores; a
  exposição de nome/bloco do locador entra na fase 3 junto com as vagas.
- `join_condominium` recebe o id (o código é resolvido antes por `find_condominium_by_invite`, que
  alimenta a prévia). `create_condominium` cria garagem + vínculo numa transação só e recebe também os
  blocos/torres (campo novo no "Cadastrar meu condomínio", que o design não tinha). Ambas atualizam nome
  e telefone do perfil. Sair do condomínio é um `delete` direto permitido pela RLS (sem RPC).
- O condomínio novo só é criado no passo "Seus dados" (o design tem "Criar condomínio" no passo 1).
- Os passos do fluxo são "1 de 2 / 2 de 2" (o design mostrava "de 3" sem um terceiro passo).
- Ordenar andares usa "↑" (o design previa arrastar).
- Vínculos ficam em cache no Room (DB v3) para abrir offline; a navegação passou a ser por áreas
  (`AppState.area()`), e `feature/home` virou `feature/profile` (Perfil mínimo até a fase 6).
- Testes SQL em `supabase/tests` (PGlite) cobrem RLS e RPCs.
- Pendência conhecida: na web, o primeiro toque logo após escolher um item de `KbSelect` é ignorado.

## Fase 2 — Locador: minhas vagas e cadastro de vaga (0.4.0)

**Migration `20261009000000_spots.sql`**
- Enums `spot_status (active, paused)`, `approval_mode (manual, auto)`.
- `spots` (id, condo_id, owner_id, level_id, sector_id, number, size_label, height_m, description,
  directions, features `text[]` — coberta/larga/elétrica/perto do elevador/moto —, prices em
  centavos `price_hour/day/week_cents` nulos = não oferece, `min_period_minutes`
  (60/120/240/1440), `cancel_notice_hours` (2/24/48), `approval_mode`, `rules text[]` (presets +
  livres), status, created_at; único (condo_id, level_id, sector_id, number)).
  Código exibido "B2-27" derivado de andar+setor+número.
- `spot_weekly_availability` (spot_id, weekday 1–7, start_time, end_time) e
  `spot_blocked_dates` (spot_id, date) — modelo do passo 3 ("Frequência", "Das/Às", dias da semana,
  "Aplicar ao calendário", BLQ).
- RLS: membros do condomínio leem vagas `active` (dono lê todas as suas); só o dono insere/edita;
  dono precisa ser membro do condomínio (check via `is_condo_member`).

**App** — `feature/hosting`
- Minhas vagas (12) com alternância "Quero uma vaga / Tenho uma vaga", vagas agrupadas por
  condomínio, ativa/pausada, pausar/reativar, "Anunciar neste condomínio"; readouts de ganhos e
  reservas mostram 0 até a fase 4.
- Wizard 13/14/15: localização (andares/setores do condomínio, fotos = placeholder), preço e regras
  (toolbars de mínimo/cancelamento/aprovação, chips de regras), disponibilidade com `KbCalendar`
  (componente novo no design-system: estados livre/sel/res/blq/passado). Estado do wizard num único
  ViewModel com `parametersOf` para edição.
- Estado de borda "Seja o primeiro" (convite com código) no Explorar vazio.
- Testes: validação de cada passo (preço ≥ 1 período, número obrigatório), geração das janelas a
  partir de frequência + dias, ViewModels do wizard e de minhas vagas.

**Como ficou (diferenças em relação ao plano acima)**
- Exceções por data em uma tabela só, `spot_date_overrides` (`open` com horário ou `blocked`), em vez de
  só `spot_blocked_dates`: "Liberar" um dia fora da regra semanal também precisa ser salvo.
- Gravação por RPC (`save_spot`, `set_spot_status`), sem insert/update direto; a edição substitui a
  disponibilidade inteira. `can_read_spot` (security definer) evita recursão de RLS.
- A garagem (`GarageLevel`) entrou em `core` (`CondoRepository.garageOf`), pois a fase 3 também usa.
- Calendário: os dias são selecionados (SEL) e um botão "Liberar N dias" / "Bloquear N dias" aplica a
  ação escolhida no toolbar; a prancha não mostrava como confirmar. Legenda com "Livre" e "Fechada"
  além de SEL/BLQ (RES chega na fase 4).
- Cards de "Minhas vagas": "Pausar" na vaga ativa (a prancha mostra "Agenda", que é da fase 4);
  tocar no card abre a edição. Ganhos e reservas aparecem como 0 até a fase 4.
- Fotos de "como chegar": placeholders, envio adiado como decidido.
- Fuso fixo UTC−3 para "hoje" (ver `CLAUDE.md` → Datas).

## Fase 3 — Explorar e pedir reserva (0.5.0)

**Migration `20261010000000_bookings.sql`**
- `create extension if not exists btree_gist`.
- Enums `billing_unit (hour, day, week)`, `booking_status (pending, confirmed, rejected, cancelled,
  expired, in_progress, completed)`, `reject_reason (visita, uso, veiculo, outro)`.
- `bookings` (id uuid, `code bigint generated always as identity` → "Reserva 4821", spot_id,
  condo_id, owner_id, renter_id, vehicle_id, starts_at, ends_at, billing_unit, units, total_cents,
  note, status, reject_reason, reject_message, respond_by (created + 12 h), cancelled_by,
  checked_in_at, checked_out_at, created_at, updated_at).
  `exclude using gist (spot_id with =, tstzrange(starts_at, ends_at) with &&) where status in
  ('confirmed','in_progress')` — pendentes podem conflitar (o locador vê "Conflito de horário").
- `booking_price(spot, starts_at, ends_at, unit)` — fonte da verdade do valor (horas/dias/semanas
  arredondados para cima: 10/10 08:00 → 11/10 18:00 = 34 h = 2 diárias).
- `search_available_spots(condo, starts_at, ends_at, filters)` → vagas com disponibilidade no
  período (janelas semanais, bloqueios, reservas confirmadas), preço "a partir de", dono (nome,
  bloco); `garage_map(condo, level, starts_at, ends_at)` → livre/ocupada/minha por vaga.
- `request_booking(spot, starts_at, ends_at, unit, vehicle, note)` (`security definer`): checa
  membership, vaga ativa, não é o dono, veículo do solicitante, período ≥ mínimo, disponibilidade;
  `auto` → confirma direto (respeitando a exclusão), `manual` → pending.
- RLS de `bookings`: select só para renter e owner; nenhuma escrita direta (só via RPC).
  Veículo da reserva passa a ser visível ao dono da vaga.

**App** — `feature/explore`
- Lista (04) com seletor de período Entrada/Saída, toolbar Lista/Mapa, chips de filtro, cartões de
  vaga (`KbSpotCard` no design-system); modo claro (05b) só pelo tema do sistema.
- Mapa da garagem (06) com toolbar de andares e `KbGarageSpot` (livre/ocupada/selecionada/sua).
- Detalhe da vaga (07): tags, locador, readouts de preço, dimensões, como chegar, regras numeradas,
  calendário do mês, total e "Solicitar reserva".
- Montar reserva (08) e resumo (09) num fluxo de 2 passos; confirma → navega (callback no `app`) para
  o detalhe da reserva da fase 4 (até lá, para "Reservas").
- Estados: nenhuma vaga livre, carregando (meter), sem conexão, vaga pausada/indisponível,
  aguardando aprovação/confirmada.
- Testes: `PriceCalculator` (espelha `booking_price`, mesmos casos), período mínimo, ViewModels de
  explorar (filtros, troca lista/mapa, troca de condomínio recarrega), detalhe e montar/resumo.

**Como ficou (diferenças em relação ao plano acima)**
- Duas migrations: `20261010000000_spot_details.sql` (características, pé-direito e "como chegar", que
  o detalhe e os filtros usam e a fase 2 não gravava — o passo 1 do cadastro de vaga ganhou esses campos)
  e `20261010000100_bookings.sql`.
- `btree_gist` no schema `extensions`. Funções com outros nomes: `billing_units` (unidades arredondadas
  para cima; o valor é preço × unidades), `search_spots` (lista **e** mapa numa chamada só: todas as
  vagas ativas com `available` e `is_mine`, em vez de `search_available_spots` + `garage_map`) e
  `spot_busy_ranges` (dias RES do calendário). Filtros (coberta, larga, elétrica, elevador, moto,
  "até R$ 10/h") são aplicados no app sobre o resultado.
- `request_booking` devolve (id, code, status, total) e falha com mensagens estáveis
  (`spot_unavailable`, `own_spot`, `invalid_period`, `invalid_vehicle`, `below_minimum`,
  `unit_not_offered`), mapeadas para `BookingError`. Veículo é opcional (quem não tem veículo pede sem).
- Período: as caixas Entrada/Saída abrem uma folha com dia (até 60 dias) e hora (de 30 em 30 min);
  o padrão é a próxima hora cheia por 2 h. O preço é calculado no app (`Prices.quote`) com a mesma regra.
- `KbGarageSpot`/`KbSpotLegend` e `KbBottomSheet` entraram no design system; os cartões da lista ficaram
  no próprio `feature/explore` (sem `KbSpotCard`).
- "Enviar solicitação" termina numa tela "Pedido enviado"/"Reserva confirmada" com o código; a aba
  Reservas continua "em breve" até a fase 4.
- O placeholder de foto do detalhe tem 120 dp (a prancha usa uma foto alta) para sobrar área rolável.

## Fase 4 — Ciclo da reserva (0.6.0)

**Migration `20261011000000_booking_lifecycle.sql`**
- RPCs com checagem de papel e de transição de estado: `approve_booking` (falha com erro
  identificável se conflitar), `reject_booking(reason, message)`, `cancel_booking` (locatário sozinho
  só até `starts_at - cancel_notice_hours`), `check_in` (libera no dia da reserva, a partir de 30 min
  antes), `check_out`, `extend_booking(new_ends_at)` para "Preciso de mais tempo"/"Ajustar horário"
  (vira pedido ao locador se a vaga for manual).
- `pg_cron` (habilitar no painel): a cada 5 min expira pendentes vencidas (`respond_by`) e marca
  `completed` reservas encerradas sem check-out após tolerância.
- View `owner_month_earnings` para "Ganhos do mês" (soma de confirmed/in_progress/completed do mês).

**App** — `feature/bookings` + partes de `feature/hosting`
- Reservas (11) com abas Próximas / Em curso / Histórico, cartões com status.
- Detalhe da reserva (10/10b): readouts, check-in, "Começa em", como chegar, contato com locador
  (botão Mensagem leva à fase 5; até lá oculto), regra de cancelamento, cancelar, relatar problema
  (oculto até existir o fluxo).
- Check-in (21) e check-out (22) com checklist de confirmações e foto opcional = placeholder.
- Hosting: solicitações (16) com aceitar/recusar (motivo + mensagem) e aviso de conflito; agenda da
  vaga (25) com calendário mensal e lista de reservas; ganhos/reservas reais em minhas vagas.
- Estados: recusada, cancelada pelo locador (vagas parecidas), atraso ("Passou do horário").
- Room: cache das reservas do usuário (DB v4) para "Suas reservas salvas continuam visíveis" offline.
- Testes: regras de transição/cancelamento no domínio (`BookingRules.canCancel/canCheckIn`),
  ViewModels de reservas, detalhe, check-in/out, solicitações, agenda; repositório com cache (fake
  remoto + DAO em memória quando viável).

**Como ficou (diferenças em relação ao plano acima)**
- Sem pg_cron obrigatório: `settle_bookings()` expira pedidos vencidos e encerra reservas passadas no
  início de `my_bookings` e de cada RPC do ciclo; o agendamento pelo pg_cron ficou opcional
  (supabase/README.md). Reserva confirmada que passou sem check-in vira `completed`.
- Sem a view `owner_month_earnings`: ganhos e reservas do mês são somados no app a partir da lista do
  locador (`ownerBookingsIn`), que já vem de `my_bookings`.
- Uma RPC de leitura, `my_bookings`, serve locatário e locador (com `role`, nome/bloco da outra parte,
  veículo e o conflito dos pedidos pendentes); não há agenda separada no banco.
- `extend_booking` muda a saída na hora quando o trecho a mais está livre e dentro da disponibilidade,
  mesmo em vaga com aprovação manual (o locador vê na agenda); não vira pedido.
- Modelo de reserva, regras (`BookingRules.kt`) e `BookingRepository` ficaram em `core` (Reservas e
  Minhas vagas usam), e `BookingPeriod`/`BookingQuote`/`shortName` saíram do Explorar para `core`.
  `feature/bookings` só tem a camada de apresentação.
- Cache no Room como um JSON por usuário (`booking_cache`, DB v4), não uma tabela por reserva.
- O detalhe da reserva também atende o locador (aceitar/recusar, cancelar); "Mensagem" e "Relatar
  problema" ficam de fora até a fase 5/backlog, e as fotos de check-in/out são placeholder.
- "Preciso de mais tempo" abre uma folha com a nova saída (de 30 em 30 min, sempre depois de agora) e o
  novo valor. A avaliação pós-uso continua no backlog.
- Cartão de Minhas vagas ganhou "Agenda" e o número de pedidos; o botão "N solicitações aguardando"
  leva à prancha 17.

## Fase 5 — Chat e notificações em tempo real (0.7.0)

**Migration `20261012000000_messages_notifications.sql`**
- `messages` (id, booking_id, sender_id nulo = mensagem de sistema, body, kind `text|system`,
  created_at, read_at); RLS: só renter/owner da reserva; insert só como `sender_id = auth.uid()`.
- `notifications` (id, user_id, condo_id, booking_id, kind `request|approved|rejected|cancelled|
  reminder|late|review`, title, body, created_at, read_at); RLS: dono lê e marca lida.
- Triggers em `bookings` geram notificações e mensagens de sistema ("CHECK-IN · 08:02");
  jobs `pg_cron` de lembrete (véspera, "check-in libera amanhã") e atraso (passou da saída).
- `alter publication supabase_realtime add table messages, notifications`.
- RPC `mark_notifications_read(condo_id?)`.

**App**
- `core/data`: instala `Realtime` no `SupabaseClientFactory`; repositórios expõem `Flow` (carga
  inicial + postgres changes), reconectando ao voltar ao app.
- `feature/messages`: aba Mensagens (lista de conversas por reserva — não desenhada, usa
  `KbCard`) e chat 1:1 (23) com respostas rápidas ("Cheguei", "Pode liberar a vaga?", "Saindo
  agora", "Preciso de mais 30 min").
- `feature/notifications` (26): filtro por condomínio, agrupado Hoje/Ontem, "Marcar como lidas";
  badges no seletor de condomínio ("1 nova") e na tab bar.
- Testes: ViewModels de chat (envio otimista, falha) e notificações (filtro, agrupamento, marcar
  lidas) com fakes de `Flow`.

**Como ficou (diferenças em relação ao plano acima)**
- Sem pg_cron para lembrete e atraso: `notify_due_bookings()` roda dentro de `settle_bookings()` (fase 4),
  que já é chamada a cada leitura; um índice único garante um aviso de cada por reserva. Tipos de
  notificação: `request`, `booked` (aprovação automática), `approved`, `rejected`, `cancelled`,
  `expired`, `reminder`, `late`; `review` ficou no backlog com a avaliação.
- Sem RPC de envio: o insert em `messages` é direto, protegido pela RLS (sempre como si mesmo, só texto).
  A observação do pedido vira a primeira mensagem do chat.
- Mensagens de sistema também para "RESERVA CONFIRMADA", "PEDIDO RECUSADO", "RESERVA CANCELADA",
  "CHECK-OUT · …" e "SAÍDA AJUSTADA PARA …" (extensão da fase 4).
- `my_conversations` monta a aba Mensagens (última mensagem e não lidas por reserva); conversas sem
  mensagens aparecem enquanto a reserva está ativa.
- Realtime: em vez de aplicar cada evento, o app recarrega a consulta a cada mudança
  (`RealtimeChanges.liveQuery`), o que evita buracos entre a carga inicial e a assinatura; sem Realtime,
  carrega e tenta assinar de novo (2 s → 60 s).
- Contadores: sino ao lado do seletor de condomínio (não havia lugar desenhado para entrar nas
  notificações), "N novas" por condomínio na troca e badge na aba Mensagens. "Mensagem" no detalhe da
  reserva abre o chat; tocar no nome no chat abre a reserva.
- Respostas rápidas são enviadas na hora (sem passar pelo campo).

## Fase 6 — Perfil (0.8.0)

**Migration `20261013000000_account.sql`**
- RPC `delete_own_account()` (`security definer`): cancela reservas futuras, apaga
  `auth.users` do próprio `auth.uid()` (cascata nas tabelas).

**App** — `feature/profile` (24): cabeçalho com avatar de iniciais, readout de reservas, veículos
(adicionar/editar/remover), condomínios (ativo, adicionar → onboarding, sair), sair da conta e
excluir conta com confirmação. Itens não desenhados (preferências de notificação, ajuda, denunciar)
aparecem como entradas desabilitadas "em breve".
Testes: ViewModel do perfil (remover veículo em uso por reserva futura → erro), exclusão de conta.

**Como ficou (diferenças em relação ao plano acima)**
- A migration também trava a remoção de veículo em uso (gatilho), troca a saída de condomínio para a
  RPC `leave_condominium` (recusa com reserva ativa e pausa as vagas do usuário) e mantém as notificações
  da outra parte quando a conta é apagada (`booking_id` vira nulo).
- `delete_own_account` recusa enquanto houver reserva em curso; as reservas do usuário são apagadas antes
  do `auth.users` (as vagas têm `on delete restrict` nas reservas).
- Sem a tag "Verificado" (não há verificação de morador) e sem edição de nome/telefone (não desenhada).
- "Reservas" conta as reservas feitas como locatário (confirmadas, em curso ou concluídas).
- O condomínio ativo não tem "Sair": é preciso ativar outro antes. Erros de sair/excluir aparecem numa
  faixa fixa no rodapé, já que esses botões ficam no fim da lista.
- Veículos: adicionar e editar numa folha (placa, modelo, cor, tipo) com remoção confirmada.

## Backlog (não desenhado ou adiado)

Fotos (Storage + upload/compressão; foto de perfil, "como chegar", check-in/out), avaliação
pós-uso e nota do locador, resumo de ganhos, denunciar problema, preferências de notificação,
login por telefone (SMS), push notifications.

## Arquivos-chave

- `core/design-system/src/commonMain/.../TuaVagaTheme.kt`, `components/*` (substituídos pelo Kerb)
- `app/src/commonMain/.../App.kt` (shell + tab bar + roteamento por sessão/onboarding)
- `core/data/.../supabase/SupabaseClientFactory.kt` (Realtime), `util/RemoteErrors.kt` (reuso)
- `core/database/.../TuaVagaDatabase.kt` + `schemas/` (v2 prefs, v3 condo ativo, v4 cache de reservas)
- `settings.gradle.kts`, `gradle/libs.versions.toml` (novos módulos, kotlinx-datetime, realtime-kt)
- `supabase/migrations/*.sql` (uma por fase acima), `supabase/README.md` (ordem das migrations,
  habilitar `pg_cron`/`btree_gist`, Realtime)
- Padrão de testes a seguir: `feature/auth/presentation/src/commonTest` (`FakeAuthRepository`,
  `LoginViewModelTest`)

## Verificação (por fase)

1. `./gradlew verifyVersion testAndroidHostTest :androidApp:assembleDebug :app:wasmJsBrowserDistribution`.
2. Aplicar a migration da fase num projeto Supabase de dev (SQL Editor ou `supabase db push`) e
   testar as policies/RPCs com dois usuários (locador e locatário) — inclusive as negações (RLS).
3. Rodar a web (`:app:wasmJsBrowserDevelopmentRun`) no browser pane e percorrer as telas da fase,
   em tema escuro e claro, comparando com as pranchas; Android pelo emulador.
4. iOS: mudanças em `iosMain`/`iosApp` (fontes no bundle, se necessário) validadas num Mac.
