package com.adamfoerster.tuavaga.feature.auth.presentation.login

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbField
import com.adamfoerster.tuavaga.core.designsystem.components.KbLabeledDivider
import com.adamfoerster.tuavaga.core.designsystem.components.KbPasswordField
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import com.adamfoerster.tuavaga.feature.auth.presentation.AuthScaffold
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

/** Board 19 · Login (e-mail only for now; the design's phone tab needs an SMS provider). */
@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
) {
    AuthScaffold(
        title = "Entrar",
        subtitle = "Use o e-mail da sua conta. Não tem conta? Crie em um minuto.",
        bottomBar = {
            KbButton(
                text = "Entrar",
                onClick = { onAction(LoginAction.OnLoginClick) },
                size = KbButtonSize.Large,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            KbLabeledDivider("ou")
            KbButton(
                text = "Criar conta",
                onClick = { onAction(LoginAction.OnRegisterClick) },
                variant = KbButtonVariant.Ghost,
                size = KbButtonSize.Large,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            KbText(
                text = "Ao continuar você aceita os termos e as regras gerais do app.",
                style = KerbTheme.typography.bodySmall,
                color = KerbTheme.colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        KbField(
            value = state.email,
            onValueChange = { onAction(LoginAction.OnEmailChange(it)) },
            label = "E-mail",
            placeholder = "voce@email.com",
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
        )
        KbPasswordField(
            value = state.password,
            onValueChange = { onAction(LoginAction.OnPasswordChange(it)) },
            label = "Senha",
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            onImeAction = { onAction(LoginAction.OnLoginClick) },
        )
        KbErrorText(state.error?.asString())
        KbButton(
            text = "Esqueci a senha",
            onClick = { onAction(LoginAction.OnForgotPasswordClick) },
            variant = KbButtonVariant.Ghost,
            enabled = !state.isLoading,
        )
    }
}
