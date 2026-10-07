# Supabase

## 1. Banco

Rode `migrations/20261007000000_profiles.sql` no SQL Editor do projeto (ou `supabase db push` com a CLI).
Ele cria `public.profiles` com RLS e o trigger que preenche o perfil no cadastro.

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
