package com.adamfoerster.tuavaga.feature.home.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.components.FormScaffold
import com.adamfoerster.tuavaga.core.designsystem.components.TvPrimaryButton
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoot(
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state = state, onAction = viewModel::onAction)
}

/** Placeholder until the parking-spot features exist. */
@Composable
fun HomeScreen(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
) {
    FormScaffold(title = "Olá${state.userName?.let { ", $it" }.orEmpty()}!") {
        Text("Você está conectado como ${state.userEmail}.")
        TvPrimaryButton(
            text = "Sair",
            onClick = { onAction(HomeAction.OnSignOutClick) },
            isLoading = state.isSigningOut,
        )
    }
}
