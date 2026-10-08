# TuaVaga — instruções para o Claude

App de aluguel de vagas de garagem em condomínio. Kotlin Multiplatform + Compose Multiplatform
(Android, iOS, wasmJs), Supabase (Postgres + Auth), Room 3 para estado local. Visão geral, comandos e
arquitetura estão no [README.md](README.md); configuração do Supabase em [supabase/README.md](supabase/README.md).

## Regras obrigatórias para toda alteração

Uma alteração só está pronta quando cumpre **todos** os itens abaixo, na mesma entrega:

1. **Testes** — todo comportamento novo ou alterado tem teste cobrindo-o (correção de bug inclui um
   teste que falhava antes). Testes ficam em `commonTest` do módulo afetado; ViewModels são testados
   com fakes de repositório (ver `feature/auth/presentation/src/commonTest`). Mudanças só de
   build/infra são cobertas por uma verificação Gradle (como `verifyVersion`).
2. **Documentação revisada** — releia e atualize o que a mudança afeta: `README.md`,
   `supabase/README.md`, KDoc/comentários próximos ao código, `local.properties.example` e este arquivo.
3. **CHANGELOG** — registre a mudança em `CHANGELOG.md` (formato Keep a Changelog, em português),
   sob a nova versão, nas seções `Adicionado` / `Alterado` / `Corrigido` / `Removido`.
4. **Versão** — faça o bump em `app.version` (`gradle.properties`) e em `MARKETING_VERSION`
   (`iosApp/Configuration/Config.xcconfig`). SemVer, enquanto pré-1.0:
   - funcionalidade nova ou mudança incompatível → **minor** (0.1.0 → 0.2.0)
   - correção, refactor, docs, build → **patch** (0.1.0 → 0.1.1)

   O `versionCode` Android é derivado automaticamente da versão.
5. **Verificação** — antes de dar como concluído, rode e confirme verde:
   ```bash
   ./gradlew verifyVersion testAndroidHostTest :androidApp:assembleDebug :app:wasmJsBrowserDistribution
   ```
   `verifyVersion` falha se versão, CHANGELOG e iOS divergirem. (Não use o `check` global: ele
   tenta rodar testes wasm/iOS que exigem navegador/macOS.) Se a mudança tocou `supabase/migrations`,
   rode também `npm --prefix supabase/tests ci && npm --prefix supabase/tests test`.

## Convenções

- **Idioma**: código, identificadores e comentários em inglês; textos de UI, docs e CHANGELOG em português.
- **Camadas**: `presentation → domain ← data`. Features nunca dependem umas das outras; o que for
  compartilhado vai para `core/*`. Novos módulos usam os convention plugins (`tuavaga.kmp.*`).
- **Presentation**: MVI — `XContract.kt` (State/Action/Event), `XViewModel`, `XRoot` + `XScreen`.
  Argumentos de rota chegam ao ViewModel via `parametersOf` do Koin (não `SavedStateHandle.toRoute`,
  que não roda em teste JVM).
- **Erros**: `Result<T, E>` de `core/domain`. Ao capturar falhas de rede capture `Throwable`
  (no wasmJs o Ktor lança `kotlin.Error`) e repasse `CancellationException`.
- **UI**: design system **Kerb** (`core/design-system`, a partir de `tuavaga.html`). Use os componentes `Kb*`
  e os tokens de `KerbTheme` (cores, tipografia); nunca cores, fontes ou formas soltas. Textos de UI em display
  e rótulos ficam em caixa alta (o `KbText` faz isso). Estado sempre com palavra, nunca só cor.
- **Fases do design**: o plano de implementação por fases (com as migrations Supabase de cada uma) está
  em `docs/plano-design.md`; siga a ordem e atualize-o ao concluir uma fase.
- **Datas**: `kotlinx-datetime`. "Hoje" do app usa o fuso fixo UTC−3 (`APP_TIME_ZONE`, `appToday()`,
  `appNow()` em `core/domain/time/AppTime.kt`); nunca `TimeZone.of("America/Sao_Paulo")` — na web (wasmJs) não há
  base de fusos e o app quebra. Preços em centavos (`Int`), horários em minutos desde a meia-noite.
- **Segredos**: só em `local.properties` / variáveis de ambiente, lidos via BuildKonfig (`AppConfig`).
  Nunca use a `service_role` key no app.
- **Room**: alterou entidade → suba a `version` do banco, adicione um `AutoMigration` (para não perder a
  sessão salva) e versione o schema em `core/database/schemas/`.
- **Supabase**: migration nova em `supabase/migrations/` (idempotente, com RLS; regras sensíveis em funções
  `security definer` com `set search_path = ''`, sem `execute` para `anon`). Cubra-a em
  `supabase/tests/migrations.test.mjs` e rode `npm --prefix supabase/tests test` antes de concluir.
- **Worker SQLite web** (`core/database/sqlite-worker/worker.js`) é ligado pelo alias em
  `app/webpack.config.d/`; mudanças nele exigem testar a versão web no navegador.
- iOS não compila no Windows; mudanças em `iosMain`/`iosApp` precisam ser validadas num Mac.

## Como testar

### Migrations (PGlite, sem Docker nem projeto Supabase)

- `supabase/tests/migrations.test.mjs` sobe um Postgres em memória (`@electric-sql/pglite`, versão
  fixada em `supabase/tests/package.json`) e simula o Supabase: roles `anon`/`authenticated`, schema
  `auth` com `auth.users` e `auth.uid()` lendo `request.jwt.claim.sub`, e `alter default privileges`
  dando acesso às tabelas como o Supabase faz.
- Aplica **todas** as migrations em ordem e reaplica a mais nova (precisa ser idempotente).
- Para agir como um usuário, o helper `as(uuid)` faz `set role authenticated` + define o `sub`;
  `as('anon')` testa o acesso anônimo. Use `expectOk(nome, sql, params, check)` e
  `expectError(nome, sql, params, código)` (ex.: `42501` RLS/permissão, `23505` unique, `23514` check,
  `22023` erro de validação lançado pelas RPCs).
- Toda migration nova ganha casos com **dois usuários**: o que cada um vê e o que é negado (RLS),
  as RPCs no caminho feliz e nos erros, e `anon` sem `execute`.
- Rodar: `npm --prefix supabase/tests ci` (uma vez) e `npm --prefix supabase/tests test`.
- Nunca aplique migrations no projeto Supabase do usuário por conta própria; ele aplica no painel.

### ViewModels

- Debounce/tempo: `Dispatchers.setMain(StandardTestDispatcher())`; o `runTest` reaproveita o scheduler
  do Main, então use `advanceTimeBy` / `advanceUntilIdle`. Sem tempo envolvido, `UnconfinedTestDispatcher`.
- Fakes ficam no `commonTest` de cada módulo (ex.: `feature/onboarding/.../Fakes.kt`, `app/.../AppFakes.kt`).
- Regra de navegação do app é função pura (`AppState.area()`, `signedOutStart()`) e se testa sem UI.
- O `SessionRepository` real emite `Loading` antes do estado conhecido para **cada** coletor. Fakes que
  emitem `SignedIn` direto escondem bugs de quem faz `first()`: ao consumir a sessão, ignore `Loading`
  (ver `signedInUserId()` em `core/data`) e, nos testes desse consumo, faça o fake emitir `Loading` antes.

### App web no navegador (verificação visual)

- `.claude/launch.json` tem a configuração `web` (`:app:wasmJsBrowserDevelopmentRun`, porta 8080).
  Não há hot reload: depois de editar, pare e suba o servidor de novo. Espere o bundle responder
  (`curl -sf http://localhost:8080/tuavaga.js`) antes de abrir.
- Confira em viewport de celular (375×812) nos temas escuro **e** claro, comparando com as pranchas.
- **Telas que dependem do backend** (não há conta de teste no Supabase): sobrescreva repositórios só
  localmente — um arquivo temporário em `app/src/wasmJsMain` com um módulo Koin de fakes e
  `loadKoinModules(...)` logo após `initKoin()` em `main.kt`. Reverta os dois antes de concluir
  (`grep -rn TEMPORARY app/src` deve voltar vazio) e nunca faça commit disso.
- **Mesmo teste no Android**: coloque o arquivo de fakes temporário em `app/src/commonMain` e chame
  `loadKoinModules(...)` no fim de `TuaVagaApplication.onCreate`; gere o APK, instale com
  `adb -s <emulador> install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`, navegue com
  `adb shell input tap X Y` e capture com `adb exec-out screencap -p`. Reverta tudo ao terminar.
- **Zerar o banco local da web** (ex.: rever a introdução): o worker SQLite trava os arquivos OPFS
  enquanto o app roda. Navegue para uma URL estática da mesma origem (`http://localhost:8080/tuavaga.js`)
  e apague com `navigator.storage.getDirectory()` + `removeEntry(nome, { recursive: true })`.
- O canvas do Compose web demora a redesenhar: espere ~1–2 s antes do screenshot e não repita toques
  achando que falharam (dois toques no seletor abrem e fecham a folha). Conhecido: o primeiro toque
  logo após escolher um item de `KbSelect` é ignorado.

### Armadilhas do ambiente (Windows / Git Bash)

- Novo módulo KMP altera `kotlin-js-store/wasm/yarn.lock`: rode `./gradlew kotlinWasmUpgradeYarnLock`
  e confira o diff antes de seguir.
- Não use crases dentro de `node -e "..."` no bash: elas viram substituição de comando. Para editar
  texto com crases (Markdown, SQL), use as ferramentas de edição de arquivo.
- Não edite SQL com `.replace` do JavaScript: no texto de substituição `$$` vira `$` e quebra os
  corpos de função (`as $$ … $$`). Use as ferramentas de edição de arquivo.
- Não há `python` no Windows deste projeto (o alias abre a Microsoft Store).
- Heredocs longos no Bash às vezes quebram; prefira as ferramentas de escrita de arquivo.
