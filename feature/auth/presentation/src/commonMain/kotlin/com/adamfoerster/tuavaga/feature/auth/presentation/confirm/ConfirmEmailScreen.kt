package com.adamfoerster.tuavaga.feature.auth.presentation.confirm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.components.ErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.FormScaffold
import com.adamfoerster.tuavaga.core.designsystem.components.InfoText
import com.adamfoerster.tuavaga.core.designsystem.components.TvPrimaryButton
import com.adamfoerster.tuavaga.core.designsystem.components.TvTextButton
import com.adamfoerster.tuavaga.core.designsystem.components.TvTextField
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import com.adamfoerster.tuavaga.feature.auth.domain.validation.CredentialsValidator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ConfirmEmailRoot(
    email: String,
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: ConfirmEmailViewModel = koinViewModel { parametersOf(email) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ConfirmEmailEvent.SignedIn -> onSignedIn()
        }
    }

    ConfirmEmailScreen(
        state = state,
        onAction = { action ->
            if (action == ConfirmEmailAction.OnBackClick) onBack()
            viewModel.onAction(action)
        },
    )
}

@Composable
fun ConfirmEmailScreen(
    state: ConfirmEmailState,
    onAction: (ConfirmEmailAction) -> Unit,
) {
    FormScaffold(
        title = "Confirme seu e-mail",
        subtitle = "Enviamos um código de ${CredentialsValidator.OTP_LENGTH} dígitos para ${state.email}.",
    ) {
        TvTextField(
            value = state.code,
            onValueChange = { onAction(ConfirmEmailAction.OnCodeChange(it)) },
            label = "Código",
            enabled = !state.isLoading,
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
            onImeAction = { onAction(ConfirmEmailAction.OnConfirmClick) },
        )
        ErrorText(state.error?.asString())
        InfoText(state.info?.asString())
        TvPrimaryButton(
            text = "Confirmar",
            onClick = { onAction(ConfirmEmailAction.OnConfirmClick) },
            isLoading = state.isLoading,
        )
        TvTextButton(
            text = if (state.isResending) "Enviando..." else "Reenviar código",
            onClick = { onAction(ConfirmEmailAction.OnResendClick) },
            enabled = !state.isLoading && !state.isResending,
        )
        TvTextButton(
            text = "Voltar",
            onClick = { onAction(ConfirmEmailAction.OnBackClick) },
            enabled = !state.isLoading,
        )
    }
}
