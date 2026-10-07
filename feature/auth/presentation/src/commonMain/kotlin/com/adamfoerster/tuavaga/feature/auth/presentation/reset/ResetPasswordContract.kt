package com.adamfoerster.tuavaga.feature.auth.presentation.reset

import com.adamfoerster.tuavaga.core.presentation.UiText

data class ResetPasswordState(
    val step: Step = Step.REQUEST_CODE,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val newPasswordConfirmation: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val info: UiText? = null,
) {
    enum class Step {
        /** Ask for the e-mail and send the recovery code. */
        REQUEST_CODE,

        /** Enter the code received and choose a new password. */
        SET_NEW_PASSWORD,
    }
}

sealed interface ResetPasswordAction {
    data class OnEmailChange(val email: String) : ResetPasswordAction
    data class OnCodeChange(val code: String) : ResetPasswordAction
    data class OnNewPasswordChange(val password: String) : ResetPasswordAction
    data class OnNewPasswordConfirmationChange(val password: String) : ResetPasswordAction
    data object OnSendCodeClick : ResetPasswordAction
    data object OnResetClick : ResetPasswordAction
    data object OnBackClick : ResetPasswordAction
}

sealed interface ResetPasswordEvent {
    /** The password was changed and the user is signed in with it. */
    data object PasswordReset : ResetPasswordEvent
}
