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
   tenta rodar testes wasm/iOS que exigem navegador/macOS.)

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
- **Segredos**: só em `local.properties` / variáveis de ambiente, lidos via BuildKonfig (`AppConfig`).
  Nunca use a `service_role` key no app.
- **Room**: alterou entidade → suba a `version` do banco e versione o schema em `core/database/schemas/`.
- **Worker SQLite web** (`core/database/sqlite-worker/worker.js`) é ligado pelo alias em
  `app/webpack.config.d/`; mudanças nele exigem testar a versão web no navegador.
- iOS não compila no Windows; mudanças em `iosMain`/`iosApp` precisam ser validadas num Mac.
