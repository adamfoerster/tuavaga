package com.adamfoerster.tuavaga.feature.home.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbBrandHeader
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreenTitle
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoot(
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state = state, onAction = viewModel::onAction)
}

/** Placeholder until the tab shell (Explorar, Reservas, …) replaces it. */
@Composable
fun HomeScreen(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
) {
    KbScreen(
        header = { KbBrandHeader() },
        bottomBar = {
            KbButton(
                text = "Sair",
                onClick = { onAction(HomeAction.OnSignOutClick) },
                variant = KbButtonVariant.Ghost,
                size = KbButtonSize.Large,
                isLoading = state.isSigningOut,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        KbScreenTitle(title = "Olá${state.userName?.let { ", $it" }.orEmpty()}")
        KbText(
            text = "Você está conectado como ${state.userEmail}.",
            style = KerbTheme.typography.body,
            color = KerbTheme.colors.inkMuted,
        )
    }
}
