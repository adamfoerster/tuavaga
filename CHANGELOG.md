# Changelog

Todas as mudanças relevantes do projeto ficam registradas aqui.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa
[Versionamento Semântico](https://semver.org/lang/pt-BR/). A versão vive em `app.version`
(`gradle.properties`); `./gradlew verifyVersion` confere se ela bate com este arquivo e com o iOS.

## [Unreleased]

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
