package com.adamfoerster.tuavaga.feature.auth.presentation.reset

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbField
import com.adamfoerster.tuavaga.core.designsystem.components.KbInfoText
import com.adamfoerster.tuavaga.core.designsystem.components.KbPasswordField
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import com.adamfoerster.tuavaga.feature.auth.domain.validation.CredentialsValidator
import com.adamfoerster.tuavaga.feature.auth.presentation.AuthScaffold
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
        Step.REQUEST_CODE -> AuthScaffold(
            title = "Recuperar senha",
            subtitle = "Informe seu e-mail para receber um código de recuperação.",
            onBack = { onAction(ResetPasswordAction.OnBackClick) },
            backEnabled = !state.isLoading,
            bottomBar = {
                KbButton(
                    text = "Enviar código",
                    onClick = { onAction(ResetPasswordAction.OnSendCodeClick) },
                    size = KbButtonSize.Large,
                    isLoading = state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            KbField(
                value = state.email,
                onValueChange = { onAction(ResetPasswordAction.OnEmailChange(it)) },
                label = "E-mail",
                placeholder = "voce@email.com",
                enabled = !state.isLoading,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                onImeAction = { onAction(ResetPasswordAction.OnSendCodeClick) },
            )
            KbErrorText(state.error?.asString())
        }

        Step.SET_NEW_PASSWORD -> AuthScaffold(
            title = "Nova senha",
            onBack = { onAction(ResetPasswordAction.OnBackClick) },
            backEnabled = !state.isLoading,
            bottomBar = {
                KbButton(
                    text = "Redefinir senha",
                    onClick = { onAction(ResetPasswordAction.OnResetClick) },
                    size = KbButtonSize.Large,
                    isLoading = state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            KbInfoText(state.info?.asString())
            KbField(
                value = state.code,
                onValueChange = { onAction(ResetPasswordAction.OnCodeChange(it)) },
                label = "Código",
                placeholder = "0".repeat(CredentialsValidator.OTP_LENGTH),
                enabled = !state.isLoading,
                keyboardType = KeyboardType.NumberPassword,
            )
            KbPasswordField(
                value = state.newPassword,
                onValueChange = { onAction(ResetPasswordAction.OnNewPasswordChange(it)) },
                label = "Nova senha",
                hint = "Mínimo de ${CredentialsValidator.MIN_PASSWORD_LENGTH} caracteres.",
                enabled = !state.isLoading,
                imeAction = ImeAction.Next,
            )
            KbPasswordField(
                value = state.newPasswordConfirmation,
                onValueChange = { onAction(ResetPasswordAction.OnNewPasswordConfirmationChange(it)) },
                label = "Confirmar nova senha",
                enabled = !state.isLoading,
                onImeAction = { onAction(ResetPasswordAction.OnResetClick) },
            )
            KbErrorText(state.error?.asString())
            KbButton(
                text = "Reenviar código",
                onClick = { onAction(ResetPasswordAction.OnSendCodeClick) },
                variant = KbButtonVariant.Ghost,
                enabled = !state.isLoading,
            )
        }
    }
}
