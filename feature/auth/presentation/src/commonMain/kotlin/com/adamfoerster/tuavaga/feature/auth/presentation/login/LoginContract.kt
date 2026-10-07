package com.adamfoerster.tuavaga.feature.auth.presentation.login

import com.adamfoerster.tuavaga.core.presentation.UiText

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
)

sealed interface LoginAction {
    data class OnEmailChange(val email: String) : LoginAction
    data class OnPasswordChange(val password: String) : LoginAction
    data object OnLoginClick : LoginAction
    data object OnRegisterClick : LoginAction
    data object OnForgotPasswordClick : LoginAction
}

sealed interface LoginEvent {
    data object SignedIn : LoginEvent
    data class EmailNotConfirmed(val email: String) : LoginEvent
}
