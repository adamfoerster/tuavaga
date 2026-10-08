package com.adamfoerster.tuavaga.app.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbIcons
import com.adamfoerster.tuavaga.core.designsystem.components.KbTab
import com.adamfoerster.tuavaga.core.designsystem.components.KbTabBar
import com.adamfoerster.tuavaga.feature.profile.presentation.ProfileRoot
import org.koin.compose.viewmodel.koinViewModel

private val tabs = listOf(
    KbTab(MainTab.EXPLORE, "Explorar", KbIcons.Search),
    KbTab(MainTab.BOOKINGS, "Reservas", KbIcons.Calendar),
    KbTab(MainTab.MY_SPOTS, "Minhas vagas", KbIcons.Spot),
    KbTab(MainTab.MESSAGES, "Mensagens", KbIcons.Message),
    KbTab(MainTab.PROFILE, "Perfil", KbIcons.Profile),
)

@Composable
fun MainShellRoot(
    onAddCondo: () -> Unit,
    viewModel: ShellViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MainShellScreen(
        state = state,
        onAction = { action ->
            if (action == ShellAction.OnAddCondoClick) onAddCondo()
            viewModel.onAction(action)
        },
    )
}

@Composable
fun MainShellScreen(
    state: ShellState,
    onAction: (ShellAction) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(KerbTheme.colors.surface),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 480.dp).safeDrawingPadding()) {
            CondoSelectorBar(active = state.active, onClick = { onAction(ShellAction.OnOpenSwitcher) })
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (state.tab) {
                    MainTab.EXPLORE -> ExplorePlaceholder(
                        active = state.active,
                        onListSpot = { onAction(ShellAction.OnTabSelect(MainTab.MY_SPOTS)) },
                    )
                    MainTab.BOOKINGS -> ComingSoonTab(
                        title = "Reservas",
                        text = "Suas reservas aparecem aqui quando a busca de vagas chegar.",
                    )
                    MainTab.MY_SPOTS -> ComingSoonTab(
                        title = "Minhas vagas",
                        text = "Em breve você cadastra sua vaga, define preço e disponibilidade.",
                    )
                    MainTab.MESSAGES -> ComingSoonTab(
                        title = "Mensagens",
                        text = "As conversas com vizinhos sobre cada reserva vão ficar aqui.",
                    )
                    MainTab.PROFILE -> ProfileRoot()
                }
            }
            KbTabBar(tabs = tabs, selected = state.tab, onSelect = { onAction(ShellAction.OnTabSelect(it)) })
        }
        if (state.isSwitcherOpen) {
            CondoSwitcherSheet(
                memberships = state.memberships,
                activeId = state.active?.condo?.id,
                onSelect = { onAction(ShellAction.OnCondoSelect(it)) },
                onAddCondo = { onAction(ShellAction.OnAddCondoClick) },
                onDismiss = { onAction(ShellAction.OnCloseSwitcher) },
            )
        }
    }
}
