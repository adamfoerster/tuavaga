package com.adamfoerster.tuavaga.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbAvatar
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.initialsOf
import org.koin.compose.viewmodel.koinViewModel

/** Content of the Perfil tab (inside the app shell). */
@Composable
fun ProfileRoot(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileScreen(state = state, onAction = viewModel::onAction, modifier = modifier)
}

/** Board 24 · Perfil, reduced to identity and sign-out until phase 6 (vehicles, condominiums…). */
@Composable
fun ProfileScreen(
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    val name = state.userName?.takeIf { it.isNotBlank() } ?: state.userEmail
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            KbAvatar(initialsOf(name), size = 64.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KbText(name, typography.md)
                KbText(state.userEmail, typography.dataSmall, color = colors.inkMuted)
            }
        }
        KbPanel(title = "Em breve") {
            KbText(
                text = "Veículos, condomínios e preferências de notificação vão aparecer aqui.",
                style = typography.bodySmall,
                color = colors.inkMuted,
            )
        }
        KbButton(
            text = "Sair da conta",
            onClick = { onAction(ProfileAction.OnSignOutClick) },
            variant = KbButtonVariant.Ghost,
            isLoading = state.isSigningOut,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
