package com.adamfoerster.tuavaga.feature.auth.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.components.ErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.FormScaffold
import com.adamfoerster.tuavaga.core.designsystem.components.TvPasswordField
import com.adamfoerster.tuavaga.core.designsystem.components.TvPrimaryButton
import com.adamfoerster.tuavaga.core.designsystem.components.TvTextButton
import com.adamfoerster.tuavaga.core.designsystem.components.TvTextField
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoot(
    onSignedIn: () -> Unit,
    onEmailNotConfirmed: (email: String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: (email: String) -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            LoginEvent.SignedIn -> onSignedIn()
            is LoginEvent.EmailNotConfirmed -> onEmailNotConfirmed(event.email)
        }
    }

    LoginScreen(
        state = state,
        onAction = { action ->
            when (action) {
                LoginAction.OnRegisterClick -> onRegisterClick()
                LoginAction.OnForgotPasswordClick -> onForgotPasswordClick(state.email.trim())
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
) {
    FormScaffold(title = "Entrar") {
        TvTextField(
            value = state.email,
            onValueChange = { onAction(LoginAction.OnEmailChange(it)) },
            label = "E-mail",
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
        )
        TvPasswordField(
            value = state.password,
            onValueChange = { onAction(LoginAction.OnPasswordChange(it)) },
            label = "Senha",
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            onImeAction = { onAction(LoginAction.OnLoginClick) },
        )
        ErrorText(state.error?.asString())
        TvPrimaryButton(
            text = "Entrar",
            onClick = { onAction(LoginAction.OnLoginClick) },
            isLoading = state.isLoading,
        )
        TvTextButton(
            text = "Esqueci minha senha",
            onClick = { onAction(LoginAction.OnForgotPasswordClick) },
            enabled = !state.isLoading,
        )
        TvTextButton(
            text = "Criar conta",
            onClick = { onAction(LoginAction.OnRegisterClick) },
            enabled = !state.isLoading,
        )
    }
}
