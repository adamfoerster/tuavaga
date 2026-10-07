# Changelog

Todas as mudanças relevantes do projeto ficam registradas aqui.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa
[Versionamento Semântico](https://semver.org/lang/pt-BR/). A versão vive em `app.version`
(`gradle.properties`); `./gradlew verifyVersion` confere se ela bate com este arquivo e com o iOS.

## [Unreleased]

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
