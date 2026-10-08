package com.adamfoerster.tuavaga.app.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.condo.ActiveCondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.condo.resolveActiveMembership
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.messages.domain.MessagesRepository
import com.adamfoerster.tuavaga.feature.notifications.domain.NotificationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab { EXPLORE, BOOKINGS, MY_SPOTS, MESSAGES, PROFILE }

data class ShellState(
    val memberships: List<Membership> = emptyList(),
    val active: Membership? = null,
    val tab: MainTab = MainTab.EXPLORE,
    val isSwitcherOpen: Boolean = false,
    /** Unread notifications per condominium (bell badge and "N novas" in the switcher). */
    val unreadByCondo: Map<String, Int> = emptyMap(),
    /** Unread chat messages (Mensagens tab badge). */
    val unreadMessages: Int = 0,
) {
    val unreadNotifications: Int get() = unreadByCondo.values.sum()
}

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
    notificationsRepository: NotificationsRepository,
    messagesRepository: MessagesRepository,
) : ViewModel() {

    private val ui = MutableStateFlow(ShellState())

    // Failed loads keep the last counts (the badges are hints, not the source of truth).
    private val unreadByCondo = notificationsRepository.notifications
        .mapNotNull { result -> (result as? Result.Success)?.data?.filter { !it.isRead }?.groupingBy { it.condoId }?.eachCount() }
        .onStart { emit(emptyMap()) }

    private val unreadMessages = messagesRepository.conversations
        .mapNotNull { result -> (result as? Result.Success)?.data?.sumOf { it.unread } }
        .onStart { emit(0) }

    val state: StateFlow<ShellState> = combine(
        ui,
        condoRepository.memberships,
        activeCondoRepository.activeCondoId,
        unreadByCondo,
        unreadMessages,
    ) { ui, memberships, activeId, byCondo, messages ->
        ui.copy(
            memberships = memberships,
            active = resolveActiveMembership(memberships, activeId),
            unreadByCondo = byCondo,
            unreadMessages = messages,
        )
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
