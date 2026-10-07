package com.adamfoerster.tuavaga.feature.auth.presentation.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.util.onFailure
import com.adamfoerster.tuavaga.core.domain.util.onSuccess
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.feature.auth.domain.AuthRepository
import com.adamfoerster.tuavaga.feature.auth.domain.validation.CredentialsValidator
import com.adamfoerster.tuavaga.feature.auth.presentation.AuthTexts
import com.adamfoerster.tuavaga.feature.auth.presentation.reset.ResetPasswordState.Step
import com.adamfoerster.tuavaga.feature.auth.presentation.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResetPasswordViewModel(
    email: String,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ResetPasswordState(email = email),
    )
    val state = _state.asStateFlow()

    private val eventChannel = Channel<ResetPasswordEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: ResetPasswordAction) {
        when (action) {
            is ResetPasswordAction.OnEmailChange -> _state.update { it.copy(email = action.email, error = null) }
            is ResetPasswordAction.OnCodeChange -> _state.update {
                it.copy(code = action.code.filter(Char::isDigit).take(CredentialsValidator.OTP_LENGTH), error = null)
            }
            is ResetPasswordAction.OnNewPasswordChange ->
                _state.update { it.copy(newPassword = action.password, error = null) }
            is ResetPasswordAction.OnNewPasswordConfirmationChange ->
                _state.update { it.copy(newPasswordConfirmation = action.password, error = null) }
            ResetPasswordAction.OnSendCodeClick -> sendCode()
            ResetPasswordAction.OnResetClick -> reset()
            ResetPasswordAction.OnBackClick -> Unit
        }
    }

    private fun sendCode() {
        val current = _state.value
        if (current.isLoading) return
        if (!CredentialsValidator.isValidEmail(current.email)) {
            _state.update { it.copy(error = AuthTexts.invalidEmail) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, info = null) }
            authRepository.requestPasswordReset(current.email)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            step = Step.SET_NEW_PASSWORD,
                            info = UiText.Dynamic("Se existir uma conta para ${current.email.trim()}, enviamos um código."),
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, error = error.toUiText()) } }
        }
    }

    private fun reset() {
        val current = _state.value
        if (current.isLoading) return
        val validationError = when {
            !CredentialsValidator.isValidOtp(current.code) -> AuthTexts.invalidOtp
            !CredentialsValidator.isValidPassword(current.newPassword) -> AuthTexts.invalidPassword
            current.newPassword != current.newPasswordConfirmation -> AuthTexts.passwordsDontMatch
            else -> null
        }
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, info = null) }
            authRepository.resetPassword(current.email, current.code, current.newPassword)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    eventChannel.send(ResetPasswordEvent.PasswordReset)
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, error = error.toUiText()) } }
        }
    }
}
