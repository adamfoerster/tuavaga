package com.adamfoerster.tuavaga.feature.auth.presentation.confirm

import com.adamfoerster.tuavaga.core.presentation.UiText

data class ConfirmEmailState(
    val email: String,
    val code: String = "",
    val isLoading: Boolean = false,
    val isResending: Boolean = false,
    val error: UiText? = null,
    val info: UiText? = null,
)

sealed interface ConfirmEmailAction {
    data class OnCodeChange(val code: String) : ConfirmEmailAction
    data object OnConfirmClick : ConfirmEmailAction
    data object OnResendClick : ConfirmEmailAction
    data object OnBackClick : ConfirmEmailAction
}

sealed interface ConfirmEmailEvent {
    data object SignedIn : ConfirmEmailEvent
}
