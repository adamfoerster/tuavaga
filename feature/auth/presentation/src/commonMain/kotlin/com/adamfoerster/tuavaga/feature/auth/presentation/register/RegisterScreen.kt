package com.adamfoerster.tuavaga.feature.auth.presentation.register

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
    FormScaffold(title = "Criar conta") {
        TvTextField(
            value = state.fullName,
            onValueChange = { onAction(RegisterAction.OnFullNameChange(it)) },
            label = "Nome completo",
            enabled = !state.isLoading,
        )
        TvTextField(
            value = state.email,
            onValueChange = { onAction(RegisterAction.OnEmailChange(it)) },
            label = "E-mail",
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
        )
        TvPasswordField(
            value = state.password,
            onValueChange = { onAction(RegisterAction.OnPasswordChange(it)) },
            label = "Senha",
            enabled = !state.isLoading,
            imeAction = ImeAction.Next,
        )
        TvPasswordField(
            value = state.passwordConfirmation,
            onValueChange = { onAction(RegisterAction.OnPasswordConfirmationChange(it)) },
            label = "Confirmar senha",
            enabled = !state.isLoading,
            onImeAction = { onAction(RegisterAction.OnRegisterClick) },
        )
        ErrorText(state.error?.asString())
        TvPrimaryButton(
            text = "Criar conta",
            onClick = { onAction(RegisterAction.OnRegisterClick) },
            isLoading = state.isLoading,
        )
        TvTextButton(
            text = "Já tenho conta",
            onClick = { onAction(RegisterAction.OnBackToLoginClick) },
            enabled = !state.isLoading,
        )
    }
}
