package com.adamfoerster.tuavaga.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.util.onFailure
import com.adamfoerster.tuavaga.core.domain.util.onSuccess
import com.adamfoerster.tuavaga.feature.auth.domain.AuthError
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

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<LoginEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnEmailChange -> _state.update { it.copy(email = action.email, error = null) }
            is LoginAction.OnPasswordChange -> _state.update { it.copy(password = action.password, error = null) }
            LoginAction.OnLoginClick -> login()
            // Navigation-only actions are handled by the Root composable.
            LoginAction.OnRegisterClick, LoginAction.OnForgotPasswordClick -> Unit
        }
    }

    private fun login() {
        val current = _state.value
        if (current.isLoading) return
        if (!CredentialsValidator.isValidEmail(current.email)) {
            _state.update { it.copy(error = AuthTexts.invalidEmail) }
            return
        }
        if (current.password.isEmpty()) {
            _state.update { it.copy(error = AuthTexts.invalidPassword) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.signIn(current.email, current.password)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    eventChannel.send(LoginEvent.SignedIn)
                }
                .onFailure { error ->
                    if (error == AuthError.EMAIL_NOT_CONFIRMED) {
                        // Send a fresh code so the confirmation screen is immediately usable.
                        authRepository.resendSignUpCode(current.email)
                        _state.update { it.copy(isLoading = false) }
                        eventChannel.send(LoginEvent.EmailNotConfirmed(current.email.trim()))
                    } else {
                        _state.update { it.copy(isLoading = false, error = error.toUiText()) }
                    }
                }
        }
    }
}
