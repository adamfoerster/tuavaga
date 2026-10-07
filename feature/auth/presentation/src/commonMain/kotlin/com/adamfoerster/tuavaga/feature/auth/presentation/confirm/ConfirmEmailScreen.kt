package com.adamfoerster.tuavaga.feature.auth.presentation.confirm

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
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import com.adamfoerster.tuavaga.feature.auth.domain.validation.CredentialsValidator
import com.adamfoerster.tuavaga.feature.auth.presentation.AuthScaffold
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
    AuthScaffold(
        title = "Confirme seu e-mail",
        subtitle = "Enviamos um código de ${CredentialsValidator.OTP_LENGTH} dígitos para ${state.email}.",
        onBack = { onAction(ConfirmEmailAction.OnBackClick) },
        backEnabled = !state.isLoading,
        bottomBar = {
            KbButton(
                text = "Confirmar",
                onClick = { onAction(ConfirmEmailAction.OnConfirmClick) },
                size = KbButtonSize.Large,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        KbField(
            value = state.code,
            onValueChange = { onAction(ConfirmEmailAction.OnCodeChange(it)) },
            label = "Código",
            placeholder = "0".repeat(CredentialsValidator.OTP_LENGTH),
            enabled = !state.isLoading,
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
            onImeAction = { onAction(ConfirmEmailAction.OnConfirmClick) },
        )
        KbErrorText(state.error?.asString())
        KbInfoText(state.info?.asString())
        KbButton(
            text = if (state.isResending) "Enviando..." else "Reenviar código",
            onClick = { onAction(ConfirmEmailAction.OnResendClick) },
            variant = KbButtonVariant.Ghost,
            enabled = !state.isLoading && !state.isResending,
        )
    }
}
