package com.adamfoerster.tuavaga.feature.auth.presentation

import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.feature.auth.domain.AuthError
import com.adamfoerster.tuavaga.feature.auth.domain.validation.CredentialsValidator

internal fun AuthError.toUiText(): UiText = UiText.Dynamic(
    when (this) {
        AuthError.INVALID_CREDENTIALS -> "E-mail ou senha incorretos."
        AuthError.EMAIL_NOT_CONFIRMED -> "Confirme seu e-mail antes de entrar."
        AuthError.EMAIL_ALREADY_REGISTERED -> "Já existe uma conta com este e-mail."
        AuthError.INVALID_EMAIL -> "E-mail inválido."
        AuthError.WEAK_PASSWORD -> "Senha muito fraca. Use pelo menos ${CredentialsValidator.MIN_PASSWORD_LENGTH} caracteres."
        AuthError.SAME_PASSWORD -> "A nova senha deve ser diferente da anterior."
        AuthError.INVALID_OR_EXPIRED_CODE -> "Código inválido ou expirado."
        AuthError.SIGNUP_DISABLED -> "Cadastro desabilitado no momento."
        AuthError.RATE_LIMITED -> "Muitas tentativas. Aguarde um pouco e tente novamente."
        AuthError.NO_INTERNET -> "Sem conexão com a internet."
        AuthError.TIMEOUT -> "O servidor demorou para responder. Tente novamente."
        AuthError.SERVER_ERROR -> "Erro no servidor. Tente novamente mais tarde."
        AuthError.UNKNOWN -> "Algo deu errado. Tente novamente."
    },
)

internal object AuthTexts {
    val invalidEmail = UiText.Dynamic("Informe um e-mail válido.")
    val invalidPassword = UiText.Dynamic("A senha precisa ter pelo menos ${CredentialsValidator.MIN_PASSWORD_LENGTH} caracteres.")
    val passwordsDontMatch = UiText.Dynamic("As senhas não conferem.")
    val invalidOtp = UiText.Dynamic("O código tem ${CredentialsValidator.OTP_LENGTH} dígitos.")
    val nameRequired = UiText.Dynamic("Informe seu nome.")
}
