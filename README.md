# TuaVaga

App de aluguel de vagas de garagem em condomínio. Kotlin Multiplatform + Compose Multiplatform
para **Android**, **iOS** e **Web (wasmJs)**, com **Supabase** (Postgres + Auth) no backend e **Room**
para estado local em todos os targets.

> Estado atual (fase 2 do design): autenticação, introdução, entrar/criar condomínio, cadastro do
> morador e veículos, seletor e troca de condomínio, tab bar e, para quem aluga, "Minhas vagas" com o
> cadastro de vaga em 3 passos (localização, preço e regras, disponibilidade), tudo no design system
> Kerb. As próximas fases estão em [docs/plano-design.md](docs/plano-design.md).

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
| Testes das migrations | `npm --prefix supabase/tests ci && npm --prefix supabase/tests test` (Postgres em memória) |

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
app                         KMP: App() raiz, áreas de navegação, shell (seletor de condomínio + tab bar), Koin, entry points
androidApp                  Application Android (AGP 9 não permite KMP no módulo de app)
iosApp                      projeto Xcode
core/domain                 Result/DataError, User, sessão, condomínios (Membership, CondoRepository), veículos, BrFormats
core/data                   cliente Supabase, repositórios de sessão/condomínios/veículos, BuildKonfig (AppConfig)
core/database               Room 3: TuaVagaDatabase, DAOs, drivers por plataforma, worker SQLite web
core/presentation           UiText, ObserveAsEvents
core/design-system          Kerb: tema escuro/claro, fontes (Barlow, Barlow Condensed, JetBrains Mono) e componentes Kb*
feature/auth/{domain,data,presentation}   login, cadastro, confirmação, recuperação de senha
feature/hosting/{domain,data,presentation}   locador: Minhas vagas, cadastro/edição de vaga, disponibilidade
feature/onboarding/presentation   introdução, entrar/criar condomínio, cadastro do morador e veículos
feature/profile/presentation      aba Perfil (por ora: identidade e "Sair da conta")
supabase                    migrations SQL, testes das migrations (PGlite) e instruções do painel
```

- **Sessão**: o supabase-kt persiste os tokens através de um `SessionManager` próprio (`RoomSessionManager`)
  que grava no Room; o usuário logado também é espelhado na tabela `user` do Room.
- **Navegação**: type-safe (navigation-compose), dividida em áreas com NavHost próprio — sem sessão
  (introdução + login), onboarding (sem condomínio) e principal. A área segue o estado (`AppState.area()`):
  entrar, concluir o primeiro cadastro de condomínio ou sair não exigem chamadas de navegação.
- **Condomínios**: os vínculos do usuário ficam em cache no Room (tabela `membership`), então o app abre
  offline; o condomínio ativo é uma preferência local (`app_prefs.activeCondoId`).
- **Room na Web**: usa `WebWorkerSQLiteDriver` com o worker em `core/database/sqlite-worker/worker.js`
  (SQLite WASM + OPFS). O alias do webpack que liga o worker fica em `app/webpack.config.d/`.
  Sem OPFS (ex.: aba anônima) o banco cai para memória.

## Próximos passos (fora do escopo atual)

- Armazenamento de imagens no servidor PHP (DreamHost): `ASSETS_BASE_URL` já está em `AppConfig`.
- Dashboard administrativo em Kotlin no fly.io.
