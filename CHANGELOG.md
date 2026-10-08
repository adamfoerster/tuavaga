# Changelog

Todas as mudanças relevantes do projeto ficam registradas aqui.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa
[Versionamento Semântico](https://semver.org/lang/pt-BR/). A versão vive em `app.version`
(`gradle.properties`); `./gradlew verifyVersion` confere se ela bate com este arquivo e com o iOS.

## [Unreleased]

## [0.7.0] - 2026-10-08

### Adicionado

- Migration `20261012000000_messages_notifications.sql`: chat de cada reserva e notificações, com RLS
  (só as partes da reserva leem e escrevem no chat; cada um lê as suas notificações), avisos e mensagens
  de sistema gerados a cada mudança da reserva, lembrete na véspera do check-in, aviso de atraso ao
  locador, as RPCs `my_conversations`, `mark_messages_read` e `mark_notifications_read`, e as tabelas na
  publicação do Supabase Realtime.
- Aba Mensagens com uma conversa por reserva (última mensagem e não lidas) e chat da reserva com
  respostas rápidas, envio imediato na tela e "Tentar de novo" quando falha.
- Central de notificações: filtro por condomínio, agrupada em Hoje/Ontem, "Marcar como lidas" e abre a
  reserva do aviso.
- Sino com contador ao lado do seletor de condomínio, "N novas" por condomínio na troca e contador na
  aba Mensagens.
- Botão "Mensagem" no detalhe da reserva.
- Supabase Realtime no app (`realtime-kt`): listas ao vivo que recarregam a cada mudança e reconectam
  sozinhas quando a conexão volta.

### Removido

- Aba "em breve" de Mensagens (todas as abas agora têm conteúdo).

## [0.6.0] - 2026-10-08

### Adicionado

- Migration `20261011000000_booking_lifecycle.sql`: `my_bookings` (reservas do locatário e do locador,
  com a outra parte, o veículo e o conflito de horário dos pedidos) e as RPCs `approve_booking`,
  `reject_booking`, `cancel_booking`, `check_in`, `check_out` e `extend_booking`, cada uma conferindo
  papel e transição. Pedidos sem resposta expiram e reservas passadas terminam sozinhos
  (`settle_bookings`; pg_cron opcional).
- Aba Reservas com Próximas, Em curso e Histórico; abre offline com as reservas salvas no aparelho.
- Detalhe da reserva: entrada e saída, "Começa em", check-in liberado 30 min antes, como chegar,
  locador, valor, prazo de cancelamento sem aviso, cancelar, e os estados recusada, cancelada pelo
  locador, sem resposta e atraso ("Passou do horário").
- Check-in e check-out com as três confirmações, e "Preciso de mais tempo" para estender a saída com o
  novo valor.
- Minhas vagas: ganhos e reservas do mês, solicitações aguardando (aceitar ou recusar com motivo e
  mensagem, aviso de conflito de horário) e agenda de cada vaga com calendário e reservas do mês.
- "Ver reserva" ao terminar um pedido no Explorar.
- `KbReadout` aceita tom no valor (verde na entrada do check-in, vermelho no atraso).
- Cache local das reservas no Room (banco v4, com migração automática).

### Alterado

- Período, cotação, status de reserva e nomes abreviados passaram de `feature/explore` para `core`,
  junto com os textos de valor e período, pois Reservas e Minhas vagas também os usam.
- Cartão de Minhas vagas com "Agenda" e o número de pedidos da vaga.

## [0.5.0] - 2026-10-08

### Adicionado

- Migration `20261010000000_spot_details.sql`: características da vaga (coberta, larga, elétrica,
  elevador, moto), pé-direito e "como chegar"; o cadastro de vaga ganhou esses campos no passo 1.
- Migration `20261010000100_bookings.sql`: reservas com código sequencial ("Reserva 1001"), trava
  contra reservas confirmadas sobrepostas, RLS (só locatário e locador leem) e as RPCs `search_spots`,
  `spot_busy_ranges` e `request_booking` (aprovação automática confirma na hora; manual fica aguardando
  resposta em até 12 h).
- Aba Explorar: período de entrada/saída, lista de vagas livres com filtros (coberta, larga, elétrica,
  elevador, moto, até R$ 10/h), mapa da garagem por andar (livre, ocupada, selecionada, sua vaga) e os
  estados "Nenhuma vaga livre", sem conexão e "Seja o primeiro".
- Detalhe da vaga: locador, preços, dimensões, pé-direito, como chegar, regras numeradas, calendário do
  mês (seu período, reservada, bloqueada) e o valor do período.
- Pedido de reserva em 2 passos (montar e resumo): forma de cobrança, período, veículo, observações,
  aceite das regras e envio, terminando em "Pedido enviado" ou "Reserva confirmada".
- Componentes Kerb `KbGarageSpot`, `KbSpotLegend` e `KbBottomSheet`.
- Testes das migrations cobrindo reservas: busca, sobreposição, período mínimo, vaga própria, veículo
  de outro usuário, aprovação automática e acesso de quem não é parte da reserva.

### Alterado

- Disponibilidade, formatos e termos de vaga (preços, forma de cobrança, características) e o fuso do
  app (`appToday()`/`appNow()`) passaram de `feature/hosting` para `core/domain`, pois o Explorar também
  os usa.

## [0.4.1] - 2026-10-08

### Corrigido

- Dropdown de condomínio vazio no cadastro de vaga (e "Minhas vagas" sem os condomínios): a lista de
  condomínios lia o estado inicial "carregando" da sessão como "sem usuário" e devolvia vazio. O mesmo
  fazia a atualização dos condomínios e o "sair do condomínio" falharem logo após entrar no app
  (regressão da 0.3.2). Agora esse estado inicial é ignorado.

## [0.4.0] - 2026-10-08

### Adicionado

- Migration `20261009000000_spots.sql`: vagas com preço por hora/dia/semana, período mínimo, prazo de
  cancelamento, modo de aprovação e regras; disponibilidade semanal e exceções por data; RLS (membros veem
  vagas ativas, o dono vê todas) e as RPCs `save_spot` e `set_spot_status`. A busca de condomínios passa
  a mostrar quantas vagas estão anunciadas.
- Aba "Minhas vagas": vagas agrupadas por condomínio, pausar e reativar, "Anunciar neste condomínio" e
  atalho para "Quero uma vaga".
- Cadastro e edição de vaga em 3 passos: localização (andar, setor, número, tamanho, descrição), preço e
  regras, e disponibilidade com calendário (liberar/bloquear dias) e repetição semanal.
- "Anunciar minha vaga" no Explorar abre o cadastro no condomínio ativo.
- Calendário mensal Kerb (`KbMonthCalendar`) com os estados livre, fechado, passado, selecionado,
  bloqueado e reservado.
- Testes das migrations cobrindo vagas: duplicidade, setor de outro andar, preço obrigatório, horário
  inválido, edição, vagas pausadas e acesso de quem não é membro.

### Alterado

- Rótulos longos no `KbToolbar` diminuem para caber em vez de serem cortados.
- O teste das migrations reaplica todas (a partir da segunda) em ordem e só aceita erros vindos do
  Postgres, evitando falsos positivos.

## [0.3.2] - 2026-10-08

### Corrigido

- No Android, bloquear a tela (ou trocar de app) no meio de um fluxo apagava o que tinha sido
  preenchido e voltava ao início: o supabase-kt marca a sessão como "inicializando" ao ir para segundo
  plano e o app tratava isso como carregamento, recriando todas as telas. Agora o estado de carregamento
  só existe na abertura do app.

## [0.3.1] - 2026-10-08

### Alterado

- `CLAUDE.md` ganhou a seção "Como testar": testes das migrations com PGlite, ViewModels com tempo
  virtual, verificação visual da versão web (inclusive com fakes temporários e como zerar o banco local)
  e armadilhas do ambiente Windows.

## [0.3.0] - 2026-10-07

### Adicionado

- Migration `20261008000000_condominiums.sql`: condomínios com código de convite, andares e setores da
  garagem, vínculos do morador (bloco, unidade, morador/trabalho), veículos, telefone no perfil, RLS e as
  RPCs de busca, convite, entrada e criação de condomínio.
- Testes das migrations em `supabase/tests` (Postgres em memória com PGlite): RLS entre dois usuários,
  RPCs, bloqueio para `anon` e reaplicação idempotente.
- Fluxo "Entrar no condomínio": busca por nome ou endereço, código de convite (com erros de formato e
  de código não encontrado) e "Cadastrar meu condomínio" com garagem (andares, setores e blocos/torres).
- "Seus dados no condomínio": nome, bloco, unidade, telefone, vínculo e veículos (placa antiga ou
  Mercosul), com aceite dos termos.
- Telas principais com seletor de condomínio ativo, troca e "Adicionar outro condomínio", e a tab bar
  (Explorar, Reservas, Minhas vagas, Mensagens, Perfil). Explorar mostra o estado "Seja o primeiro" com
  o código de convite; as demais abas indicam o que vem nas próximas versões.
- Aba Perfil com identidade e "Sair da conta".
- Componentes Kerb: ícones do design, botão de ícone, tab bar, item de lista selecionável, cabeçalho de
  seção e cabeçalho de passos.
- Tela "Sem conexão" quando os condomínios não carregam no primeiro acesso.

### Alterado

- Navegação dividida em áreas (sem sessão, onboarding e principal), decididas pelo estado da sessão e
  dos condomínios.
- Banco local na versão 3: cache dos condomínios do usuário (o app abre offline) e condomínio ativo.
- Rótulos longos de botão diminuem para caber em vez de serem cortados.

### Removido

- Módulo `feature/home` (substituído pela aba Perfil em `feature/profile`).

### Corrigido

- Menu do `KbSelect` com a largura do campo (antes ficava estreito e sobreposto ao conteúdo).

## [0.2.0] - 2026-10-07

### Adicionado

- Design system **Kerb** em `core/design-system`, a partir do design `tuavaga.html`: tokens de cor dos
  temas escuro (padrão) e claro, escala tipográfica, cantos chanfrados e componentes `Kb*` (botão,
  tag, painel, card, campo, senha, select, toolbar, meter, readout, chip, avatar, checkbox, faixa
  zebrada, placeholder de foto e layout de tela).
- Fontes Barlow, Barlow Condensed e JetBrains Mono embarcadas como recursos Compose (licença OFL em
  `core/design-system/licenses/`).
- Tela de introdução "Como funciona" (módulo `feature/onboarding`), mostrada uma vez por aparelho antes
  do login; a preferência fica no Room (tabela `app_prefs`).
- Plano de implementação do design em fases, com as migrations Supabase de cada uma, em
  `docs/plano-design.md`.

### Alterado

- Login, cadastro, confirmação de e-mail, recuperação de senha e tela inicial usam o visual Kerb.
- Banco local na versão 2 com migração automática (a sessão salva é mantida na atualização).

### Removido

- Componentes Material provisórios (`TvPrimaryButton`, `TvTextField`, `FormScaffold` etc.).

### Corrigido

- `kotlin-js-store/wasm/yarn.lock` sem a entrada obsoleta do worker SQLite (hoje ligado por alias do webpack).

## [0.1.0] - 2026-10-07

### Adicionado

- Projeto Kotlin Multiplatform + Compose Multiplatform com targets Android, iOS e Web (wasmJs).
- Modularização por feature e camada (`core/*`, `feature/auth/*`, `feature/home/*`) com convention
  plugins em `build-logic/`.
- Integração com Supabase Auth: login, logout, cadastro com confirmação por código de 8 dígitos e
  recuperação de senha por código.
- Room 3 em todos os targets, inclusive na Web via worker SQLite WASM com OPFS; a sessão do Supabase e
  o usuário logado são persistidos localmente.
- Migration SQL de `public.profiles` com RLS e trigger de criação no cadastro.
- Telas mínimas em Material 3 (sem design) e tela inicial logada com "Sair".
- Testes de validação de credenciais e dos ViewModels de login e recuperação de senha.
- `CLAUDE.md` com as regras do projeto; versão única em `app.version` verificada por `verifyVersion`.
