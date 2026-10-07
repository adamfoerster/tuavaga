package com.adamfoerster.tuavaga.feature.auth.presentation.register

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbField
import com.adamfoerster.tuavaga.core.designsystem.components.KbPasswordField
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import com.adamfoerster.tuavaga.feature.auth.domain.validation.CredentialsValidator
import com.adamfoerster.tuavaga.feature.auth.presentation.AuthScaffold
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterRoot(
    onSignedIn: () -> Unit,
    onConfirmationRequired: (email: String) -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            RegisterEvent.SignedIn -> onSignedIn()
            is RegisterEvent.ConfirmationRequired -> onConfirmationRequired(event.email)
        }
    }

    RegisterScreen(
        state = state,
        onAction = { action ->
            if (action == RegisterAction.OnBackToLoginClick) onBackToLogin()
            viewModel.onAction(action)
        },
    )
}

@Composable
fun RegisterScreen(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
) {
    AuthScaffold(
        title = "Criar conta",
        subtitle = "Leva um minuto. Depois você entra no seu condomínio.",
        onBack = { onAction(RegisterAction.OnBackToLoginClick) },
        backEnabled = !state.isLoading,
        bottomBar = {
            KbButton(
                text = "Criar conta",
                onClick = { onAction(RegisterAction.OnRegisterClick) },
                size = KbButtonSize.Large,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        KbField(
            value = state.fullName,
            onValueChange = { onAction(RegisterAction.OnFullNameChange(it)) },
            label = "Nome completo",
            enabled = !state.isLoading,
        )
        KbField(
            value = state.email,
            onValueChange = { onAction(RegisterAction.OnEmailChange(it)) },
            label = "E-mail",
            placeholder = "voce@email.com",
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
        )
        KbPasswordField(
            value = state.password,
            onValueChange = { onAction(RegisterAction.OnPasswordChange(it)) },
            label = "Senha",
            hint = "Mínimo de ${CredentialsValidator.MIN_PASSWORD_LENGTH} caracteres.",
            enabled = !state.isLoading,
            imeAction = ImeAction.Next,
        )
        KbPasswordField(
            value = state.passwordConfirmation,
            onValueChange = { onAction(RegisterAction.OnPasswordConfirmationChange(it)) },
            label = "Confirmar senha",
            enabled = !state.isLoading,
            onImeAction = { onAction(RegisterAction.OnRegisterClick) },
        )
        KbErrorText(state.error?.asString())
    }
}
