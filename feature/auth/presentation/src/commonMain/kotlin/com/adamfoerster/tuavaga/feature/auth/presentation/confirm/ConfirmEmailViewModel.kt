package com.adamfoerster.tuavaga.feature.auth.presentation.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.util.onFailure
import com.adamfoerster.tuavaga.core.domain.util.onSuccess
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.feature.auth.domain.AuthRepository
import com.adamfoerster.tuavaga.feature.auth.domain.validation.CredentialsValidator
import com.adamfoerster.tuavaga.feature.auth.presentation.AuthTexts
import com.adamfoerster.tuavaga.feature.auth.presentation.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConfirmEmailViewModel(
    email: String,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ConfirmEmailState(email = email),
    )
    val state = _state.asStateFlow()

    private val eventChannel = Channel<ConfirmEmailEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: ConfirmEmailAction) {
        when (action) {
            is ConfirmEmailAction.OnCodeChange -> _state.update {
                it.copy(code = action.code.filter(Char::isDigit).take(CredentialsValidator.OTP_LENGTH), error = null)
            }
            ConfirmEmailAction.OnConfirmClick -> confirm()
            ConfirmEmailAction.OnResendClick -> resend()
            ConfirmEmailAction.OnBackClick -> Unit
        }
    }

    private fun confirm() {
        val current = _state.value
        if (current.isLoading) return
        if (!CredentialsValidator.isValidOtp(current.code)) {
            _state.update { it.copy(error = AuthTexts.invalidOtp) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, info = null) }
            authRepository.confirmSignUp(current.email, current.code)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    eventChannel.send(ConfirmEmailEvent.SignedIn)
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, error = error.toUiText()) } }
        }
    }

    private fun resend() {
        val current = _state.value
        if (current.isResending) return
        viewModelScope.launch {
            _state.update { it.copy(isResending = true, error = null, info = null) }
            authRepository.resendSignUpCode(current.email)
                .onSuccess {
                    _state.update { it.copy(isResending = false, info = UiText.Dynamic("Enviamos um novo código.")) }
                }
                .onFailure { error -> _state.update { it.copy(isResending = false, error = error.toUiText()) } }
        }
    }
}
