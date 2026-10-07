package com.adamfoerster.tuavaga.feature.auth.presentation.reset

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.components.ErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.FormScaffold
import com.adamfoerster.tuavaga.core.designsystem.components.InfoText
import com.adamfoerster.tuavaga.core.designsystem.components.TvPasswordField
import com.adamfoerster.tuavaga.core.designsystem.components.TvPrimaryButton
import com.adamfoerster.tuavaga.core.designsystem.components.TvTextButton
import com.adamfoerster.tuavaga.core.designsystem.components.TvTextField
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import com.adamfoerster.tuavaga.feature.auth.presentation.reset.ResetPasswordState.Step
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ResetPasswordRoot(
    email: String,
    onPasswordReset: () -> Unit,
    onBack: () -> Unit,
    viewModel: ResetPasswordViewModel = koinViewModel { parametersOf(email) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ResetPasswordEvent.PasswordReset -> onPasswordReset()
        }
    }

    ResetPasswordScreen(
        state = state,
        onAction = { action ->
            if (action == ResetPasswordAction.OnBackClick) onBack()
            viewModel.onAction(action)
        },
    )
}

@Composable
fun ResetPasswordScreen(
    state: ResetPasswordState,
    onAction: (ResetPasswordAction) -> Unit,
) {
    when (state.step) {
        Step.REQUEST_CODE -> FormScaffold(
            title = "Recuperar senha",
            subtitle = "Informe seu e-mail para receber um código de recuperação.",
        ) {
            TvTextField(
                value = state.email,
                onValueChange = { onAction(ResetPasswordAction.OnEmailChange(it)) },
                label = "E-mail",
                enabled = !state.isLoading,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                onImeAction = { onAction(ResetPasswordAction.OnSendCodeClick) },
            )
            ErrorText(state.error?.asString())
            TvPrimaryButton(
                text = "Enviar código",
                onClick = { onAction(ResetPasswordAction.OnSendCodeClick) },
                isLoading = state.isLoading,
            )
            TvTextButton(
                text = "Voltar",
                onClick = { onAction(ResetPasswordAction.OnBackClick) },
                enabled = !state.isLoading,
            )
        }

        Step.SET_NEW_PASSWORD -> FormScaffold(title = "Nova senha") {
            InfoText(state.info?.asString())
            TvTextField(
                value = state.code,
                onValueChange = { onAction(ResetPasswordAction.OnCodeChange(it)) },
                label = "Código",
                enabled = !state.isLoading,
                keyboardType = KeyboardType.NumberPassword,
            )
            TvPasswordField(
                value = state.newPassword,
                onValueChange = { onAction(ResetPasswordAction.OnNewPasswordChange(it)) },
                label = "Nova senha",
                enabled = !state.isLoading,
                imeAction = ImeAction.Next,
            )
            TvPasswordField(
                value = state.newPasswordConfirmation,
                onValueChange = { onAction(ResetPasswordAction.OnNewPasswordConfirmationChange(it)) },
                label = "Confirmar nova senha",
                enabled = !state.isLoading,
                onImeAction = { onAction(ResetPasswordAction.OnResetClick) },
            )
            ErrorText(state.error?.asString())
            TvPrimaryButton(
                text = "Redefinir senha",
                onClick = { onAction(ResetPasswordAction.OnResetClick) },
                isLoading = state.isLoading,
            )
            TvTextButton(
                text = "Reenviar código",
                onClick = { onAction(ResetPasswordAction.OnSendCodeClick) },
                enabled = !state.isLoading,
            )
            TvTextButton(
                text = "Voltar",
                onClick = { onAction(ResetPasswordAction.OnBackClick) },
                enabled = !state.isLoading,
            )
        }
    }
}
