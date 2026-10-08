package com.adamfoerster.tuavaga.feature.hosting.presentation.myspots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.hosting.domain.HostingRepository
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * Board 12 · Minhas vagas: the user's spots grouped by condominium, with pause / reactivate, the
 * month's earnings and the requests waiting for an answer.
 */
class MySpotsViewModel(
    private val hostingRepository: HostingRepository,
    private val condoRepository: CondoRepository,
    private val bookingRepository: BookingRepository,
    today: () -> LocalDate,
) : ViewModel() {

    private val _state = MutableStateFlow(MySpotsState(today = today()))
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            bookingRepository.bookings.collect { all ->
                _state.update { it.copy(ownerBookings = all.filter { b -> b.role == BookingRole.OWNER }) }
            }
        }
    }

    fun onAction(action: MySpotsAction) {
        when (action) {
            MySpotsAction.OnRefresh -> refresh()
            is MySpotsAction.OnToggleStatus -> toggle(action.spotId)
            is MySpotsAction.OnCreateSpot, is MySpotsAction.OnEditSpot, is MySpotsAction.OnAgendaClick,
            MySpotsAction.OnRequestsClick, MySpotsAction.OnWantSpotClick,
            -> Unit
        }
    }

    private fun refresh() {
        // Earnings and requests come from the bookings cache; a failed reload keeps the cached numbers.
        viewModelScope.launch { bookingRepository.refresh() }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val memberships = condoRepository.memberships.first()
            when (val result = hostingRepository.mySpots()) {
                is Result.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        groups = memberships.map { membership ->
                            CondoSpots(
                                condoId = membership.condo.id,
                                condoName = membership.condo.name,
                                spots = result.data.filter { spot -> spot.condoId == membership.condo.id },
                            )
                        },
                    )
                }
                is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun toggle(spotId: String) {
        if (_state.value.updatingSpotId != null) return
        val spot = _state.value.groups.flatMap { it.spots }.firstOrNull { it.id == spotId } ?: return
        val newStatus = if (spot.status == SpotStatus.ACTIVE) SpotStatus.PAUSED else SpotStatus.ACTIVE
        viewModelScope.launch {
            _state.update { it.copy(updatingSpotId = spotId, error = null) }
            when (val result = hostingRepository.setStatus(spotId, newStatus)) {
                is Result.Success -> _state.update { current ->
                    current.copy(
                        updatingSpotId = null,
                        groups = current.groups.map { group ->
                            group.copy(spots = group.spots.map { if (it.id == spotId) it.copy(status = newStatus) else it })
                        },
                    )
                }
                is Result.Failure -> _state.update { it.copy(updatingSpotId = null, error = result.error.toUiText()) }
            }
        }
    }
}
