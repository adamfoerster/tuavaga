# Changelog

Todas as mudanças relevantes do projeto ficam registradas aqui.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa
[Versionamento Semântico](https://semver.org/lang/pt-BR/). A versão vive em `app.version`
(`gradle.properties`); `./gradlew verifyVersion` confere se ela bate com este arquivo e com o iOS.

## [Unreleased]

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
