package com.adamfoerster.tuavaga.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.util.onFailure
import com.adamfoerster.tuavaga.core.domain.util.onSuccess
import com.adamfoerster.tuavaga.feature.auth.domain.AuthRepository
import com.adamfoerster.tuavaga.feature.auth.domain.SignUpResult
import com.adamfoerster.tuavaga.feature.auth.domain.validation.CredentialsValidator
import com.adamfoerster.tuavaga.feature.auth.presentation.AuthTexts
import com.adamfoerster.tuavaga.feature.auth.presentation.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<RegisterEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: RegisterAction) {
        when (action) {
            is RegisterAction.OnFullNameChange -> _state.update { it.copy(fullName = action.fullName, error = null) }
            is RegisterAction.OnEmailChange -> _state.update { it.copy(email = action.email, error = null) }
            is RegisterAction.OnPasswordChange -> _state.update { it.copy(password = action.password, error = null) }
            is RegisterAction.OnPasswordConfirmationChange ->
                _state.update { it.copy(passwordConfirmation = action.password, error = null) }
            RegisterAction.OnRegisterClick -> register()
            RegisterAction.OnBackToLoginClick -> Unit
        }
    }

    private fun register() {
        val current = _state.value
        if (current.isLoading) return
        val validationError = when {
            current.fullName.isBlank() -> AuthTexts.nameRequired
            !CredentialsValidator.isValidEmail(current.email) -> AuthTexts.invalidEmail
            !CredentialsValidator.isValidPassword(current.password) -> AuthTexts.invalidPassword
            current.password != current.passwordConfirmation -> AuthTexts.passwordsDontMatch
            else -> null
        }
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.signUp(current.fullName, current.email, current.password)
                .onSuccess { result ->
                    _state.update { it.copy(isLoading = false) }
                    eventChannel.send(
                        when (result) {
                            SignUpResult.SIGNED_IN -> RegisterEvent.SignedIn
                            SignUpResult.CONFIRMATION_REQUIRED -> RegisterEvent.ConfirmationRequired(current.email.trim())
                        },
                    )
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.toUiText()) }
                }
        }
    }
}
