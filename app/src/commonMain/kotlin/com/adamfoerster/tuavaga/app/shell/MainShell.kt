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
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.presentation.list.ExploreRoot
import com.adamfoerster.tuavaga.feature.hosting.presentation.myspots.MySpotsRoot
import com.adamfoerster.tuavaga.feature.profile.presentation.ProfileRoot
import org.koin.compose.viewmodel.koinViewModel

private val tabs = listOf(
    KbTab(MainTab.EXPLORE, "Explorar", KbIcons.Search),
    KbTab(MainTab.BOOKINGS, "Reservas", KbIcons.Calendar),
    KbTab(MainTab.MY_SPOTS, "Minhas vagas", KbIcons.Spot),
    KbTab(MainTab.MESSAGES, "Mensagens", KbIcons.Message),
    KbTab(MainTab.PROFILE, "Perfil", KbIcons.Profile),
)

/** Navigation out of the shell, wired by the app root. */
class ShellNavigation(
    val onAddCondo: () -> Unit,
    /** `null` condoId = the active condominium. */
    val onCreateSpot: (condoId: String?) -> Unit,
    val onEditSpot: (spotId: String) -> Unit,
    val onOpenSpot: (condoId: String, spotId: String, period: BookingPeriod) -> Unit,
)

@Composable
fun MainShellRoot(
    navigation: ShellNavigation,
    viewModel: ShellViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MainShellScreen(
        state = state,
        navigation = navigation,
        onAction = { action ->
            if (action == ShellAction.OnAddCondoClick) navigation.onAddCondo()
            viewModel.onAction(action)
        },
    )
}

@Composable
fun MainShellScreen(
    state: ShellState,
    navigation: ShellNavigation,
    onAction: (ShellAction) -> Unit,
) {
    val createInActiveCondo = { navigation.onCreateSpot(state.active?.condo?.id) }
    Box(
        modifier = Modifier.fillMaxSize().background(KerbTheme.colors.surface),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 480.dp).safeDrawingPadding()) {
            CondoSelectorBar(active = state.active, onClick = { onAction(ShellAction.OnOpenSwitcher) })
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (state.tab) {
                    MainTab.EXPLORE -> ExploreRoot(onOpenSpot = navigation.onOpenSpot, onListSpot = createInActiveCondo)
                    MainTab.BOOKINGS -> ComingSoonTab(
                        title = "Reservas",
                        text = "Suas reservas e pedidos vão aparecer aqui na próxima versão.",
                    )
                    MainTab.MY_SPOTS -> MySpotsRoot(
                        onCreateSpot = { condoId -> navigation.onCreateSpot(condoId ?: state.active?.condo?.id) },
                        onEditSpot = navigation.onEditSpot,
                        onWantSpot = { onAction(ShellAction.OnTabSelect(MainTab.EXPLORE)) },
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
