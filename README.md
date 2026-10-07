# TuaVaga

App de aluguel de vagas de garagem em condomínio. Kotlin Multiplatform + Compose Multiplatform
para **Android**, **iOS** e **Web (wasmJs)**, com **Supabase** (Postgres + Auth) no backend e **Room**
para estado local em todos os targets.

> Estado atual: base do projeto + autenticação (login, logout, cadastro com confirmação por código,
> recuperação de senha por código) + introdução, já no design system Kerb. O restante do design
> (`tuavaga.html`) entra em fases — veja [docs/plano-design.md](docs/plano-design.md).

## Configuração

1. Copie `local.properties.example` para `local.properties` e preencha `SUPABASE_URL` e `SUPABASE_ANON_KEY`.
   (Também podem vir de variáveis de ambiente com o mesmo nome, útil em CI.)
2. Configure o projeto Supabase seguindo [`supabase/README.md`](supabase/README.md) — em especial os
   templates de e-mail com `{{ .Token }}`, pois o app usa código de 8 dígitos em vez de link.

Requisitos: JDK 17+, Android SDK (compileSdk 37 é baixado automaticamente), Xcode 16+ para iOS.

## Rodando

| Target | Comando |
| --- | --- |
| Android | `./gradlew :androidApp:installDebug` (ou rode `androidApp` pelo Android Studio) |
| Web | `./gradlew :app:wasmJsBrowserDevelopmentRun` → http://localhost:8080 |
| iOS | Abra `iosApp/iosApp.xcodeproj` no Xcode (macOS) e rode. O build phase chama `:app:embedAndSignAppleFrameworkForXcode`. Defina `TEAM_ID` em `iosApp/Configuration/Config.xcconfig` para rodar em device. |
| Testes | `./gradlew testAndroidHostTest` (roda os `commonTest` na JVM) |

Build de produção web: `./gradlew :app:wasmJsBrowserDistribution` → `app/build/dist/wasmJs/productionExecutable/`.

## Versão e changelog

A versão fica em `app.version` no `gradle.properties` (o `versionCode` Android é derivado dela) e
precisa ser repetida em `MARKETING_VERSION` no `iosApp/Configuration/Config.xcconfig`. Toda alteração
faz bump de versão e ganha entrada no [CHANGELOG.md](CHANGELOG.md); `./gradlew verifyVersion`
falha se os três divergirem. As regras de contribuição estão no
[CLAUDE.md](CLAUDE.md).

## Arquitetura

Modularizado por feature e por camada (`presentation → domain ← data`), com convention plugins em `build-logic/`.

```
build-logic/convention      convention plugins (tuavaga.kmp.library / .compose / .feature / .room)
app                         KMP: App() raiz, navegação, Koin, entry points iOS e wasm
androidApp                  Application Android (AGP 9 não permite KMP no módulo de app)
iosApp                      projeto Xcode
core/domain                 Result/DataError, User, SessionRepository
core/data                   cliente Supabase, sessão persistida no Room, BuildKonfig (AppConfig)
core/database               Room 3: TuaVagaDatabase, DAOs, drivers por plataforma, worker SQLite web
core/presentation           UiText, ObserveAsEvents
core/design-system          Kerb: tema escuro/claro, fontes (Barlow, Barlow Condensed, JetBrains Mono) e componentes Kb*
feature/auth/{domain,data,presentation}   login, cadastro, confirmação, recuperação de senha
feature/onboarding/presentation   introdução "Como funciona" (mostrada uma vez por aparelho)
feature/home/presentation   tela logada (placeholder) com "Sair"
supabase                    migrations SQL e instruções do painel
```

- **Sessão**: o supabase-kt persiste os tokens através de um `SessionManager` próprio (`RoomSessionManager`)
  que grava no Room; o usuário logado também é espelhado na tabela `user` do Room.
- **Navegação**: type-safe (navigation-compose). A tela inicial é decidida pela sessão salva; logout ou
  sessão expirada voltam para o login automaticamente.
- **Room na Web**: usa `WebWorkerSQLiteDriver` com o worker em `core/database/sqlite-worker/worker.js`
  (SQLite WASM + OPFS). O alias do webpack que liga o worker fica em `app/webpack.config.d/`.
  Sem OPFS (ex.: aba anônima) o banco cai para memória.

## Próximos passos (fora do escopo atual)

- Armazenamento de imagens no servidor PHP (DreamHost): `ASSETS_BASE_URL` já está em `AppConfig`.
- Dashboard administrativo em Kotlin no fly.io.
