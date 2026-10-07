package com.adamfoerster.tuavaga.feature.auth.presentation.register

import com.adamfoerster.tuavaga.core.presentation.UiText

data class RegisterState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
)

sealed interface RegisterAction {
    data class OnFullNameChange(val fullName: String) : RegisterAction
    data class OnEmailChange(val email: String) : RegisterAction
    data class OnPasswordChange(val password: String) : RegisterAction
    data class OnPasswordConfirmationChange(val password: String) : RegisterAction
    data object OnRegisterClick : RegisterAction
    data object OnBackToLoginClick : RegisterAction
}

sealed interface RegisterEvent {
    data object SignedIn : RegisterEvent
    data class ConfirmationRequired(val email: String) : RegisterEvent
}
