# Supabase

## 1. Banco

Rode as migrations **em ordem** no SQL Editor do projeto (ou `supabase db push` com a CLI):

| Migration | O que cria |
| --- | --- |
| `20261007000000_profiles.sql` | `public.profiles` com RLS e o trigger que preenche o perfil no cadastro |
| `20261008000000_condominiums.sql` | condomínios, andares/setores da garagem, vínculos (`memberships`), veículos, RLS e as RPCs `search_condominiums`, `find_condominium_by_invite`, `condominium_blocks`, `join_condominium`, `create_condominium` |

Regras da fase de condomínios:

- Só membros leem o condomínio e a garagem. Quem ainda não entrou usa as RPCs de busca e de convite,
  que não expõem membros nem o código de convite.
- O vínculo só nasce por `join_condominium` / `create_condominium` (insert direto é bloqueado pela RLS);
  as duas também atualizam nome e telefone em `profiles`.
- Veículos são do usuário (não do condomínio) e só o dono os vê.
- As RPCs só podem ser chamadas por usuários logados (`anon` não tem `execute`).

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
