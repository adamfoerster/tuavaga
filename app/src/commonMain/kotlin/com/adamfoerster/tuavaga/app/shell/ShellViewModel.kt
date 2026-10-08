package com.adamfoerster.tuavaga.app.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.condo.ActiveCondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.condo.resolveActiveMembership
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab { EXPLORE, BOOKINGS, MY_SPOTS, MESSAGES, PROFILE }

data class ShellState(
    val memberships: List<Membership> = emptyList(),
    val active: Membership? = null,
    val tab: MainTab = MainTab.EXPLORE,
    val isSwitcherOpen: Boolean = false,
)

sealed interface ShellAction {
    data class OnTabSelect(val tab: MainTab) : ShellAction
    data object OnOpenSwitcher : ShellAction
    data object OnCloseSwitcher : ShellAction
    data class OnCondoSelect(val condoId: String) : ShellAction
    data object OnAddCondoClick : ShellAction
}

/** Main screens: condominium selector on top (board "Seletor de condomínio"), tabs at the bottom. */
class ShellViewModel(
    condoRepository: CondoRepository,
    private val activeCondoRepository: ActiveCondoRepository,
) : ViewModel() {

    private val ui = MutableStateFlow(ShellState())

    val state: StateFlow<ShellState> = combine(
        ui,
        condoRepository.memberships,
        activeCondoRepository.activeCondoId,
    ) { ui, memberships, activeId ->
        ui.copy(memberships = memberships, active = resolveActiveMembership(memberships, activeId))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ShellState())

    fun onAction(action: ShellAction) {
        when (action) {
            is ShellAction.OnTabSelect -> ui.update { it.copy(tab = action.tab) }
            ShellAction.OnOpenSwitcher -> ui.update { it.copy(isSwitcherOpen = true) }
            ShellAction.OnCloseSwitcher -> ui.update { it.copy(isSwitcherOpen = false) }
            is ShellAction.OnCondoSelect -> {
                ui.update { it.copy(isSwitcherOpen = false) }
                viewModelScope.launch { activeCondoRepository.setActiveCondo(action.condoId) }
            }
            // Navigation is done by the Root; the sheet closes so it is not open on return.
            ShellAction.OnAddCondoClick -> ui.update { it.copy(isSwitcherOpen = false) }
        }
    }
}
