package com.adamfoerster.tuavaga.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.notifications.domain.NotificationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Board 26 · Notificações: filter by condominium, grouped by day, "Marcar como lidas". */
class NotificationsViewModel(
    private val notificationsRepository: NotificationsRepository,
    condoRepository: CondoRepository,
    today: () -> LocalDate,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsState(today = today()))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            notificationsRepository.notifications.collect { result ->
                _state.update {
                    when (result) {
                        is Result.Success -> it.copy(notifications = result.data, isLoading = false, error = null)
                        is Result.Failure -> it.copy(isLoading = false, error = result.error.toUiText())
                    }
                }
            }
        }
        viewModelScope.launch {
            condoRepository.memberships.collect { list -> _state.update { it.copy(memberships = list) } }
        }
    }

    fun onAction(action: NotificationsAction) {
        when (action) {
            is NotificationsAction.OnFilterSelect -> _state.update { it.copy(condoFilter = action.condoId) }
            NotificationsAction.OnMarkAllRead -> markRead()
            NotificationsAction.OnBackClick, is NotificationsAction.OnNotificationClick -> Unit
        }
    }

    private fun markRead() {
        val state = _state.value
        if (state.isMarking || state.unread == 0) return
        viewModelScope.launch {
            _state.update { it.copy(isMarking = true, error = null) }
            val result = notificationsRepository.markRead(state.condoFilter)
            // Realtime brings the read flags; until then the badge clears right away.
            _state.update { current ->
                when (result) {
                    is Result.Success -> current.copy(
                        isMarking = false,
                        notifications = current.notifications.map {
                            if (state.condoFilter == null || it.condoId == state.condoFilter) it.copy(isRead = true) else it
                        },
                    )
                    is Result.Failure -> current.copy(isMarking = false, error = result.error.toUiText())
                }
            }
        }
    }
}
