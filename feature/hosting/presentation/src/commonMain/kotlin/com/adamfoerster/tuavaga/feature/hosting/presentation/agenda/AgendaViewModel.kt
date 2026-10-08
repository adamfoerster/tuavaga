package com.adamfoerster.tuavaga.feature.hosting.presentation.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.presentation.firstOfMonth
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.hosting.domain.HostingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Board 28 · Agenda da vaga: month calendar and the bookings of one of the user's spots. */
class AgendaViewModel(
    private val spotId: String,
    private val hostingRepository: HostingRepository,
    private val bookingRepository: BookingRepository,
    today: () -> LocalDate,
) : ViewModel() {

    private val _state = MutableStateFlow(today().let { AgendaState(today = it, month = it.firstOfMonth()) })
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            bookingRepository.bookings.collect { all ->
                _state.update { it.copy(bookings = all.filter { b -> b.role == BookingRole.OWNER && b.spotId == spotId }) }
            }
        }
        load()
    }

    fun onAction(action: AgendaAction) {
        when (action) {
            AgendaAction.OnPreviousMonth -> _state.update {
                if (it.canGoBack) it.copy(month = it.month.minus(1, DateTimeUnit.MONTH)) else it
            }
            AgendaAction.OnNextMonth -> _state.update { it.copy(month = it.month.plus(1, DateTimeUnit.MONTH)) }
            AgendaAction.OnRetry -> load()
            AgendaAction.OnBackClick, AgendaAction.OnEditClick, AgendaAction.OnRequestsClick,
            is AgendaAction.OnBookingClick,
            -> Unit
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            launch { bookingRepository.refresh() }
            when (val result = hostingRepository.spot(spotId)) {
                is Result.Success -> _state.update { it.copy(isLoading = false, spot = result.data) }
                is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            }
        }
    }
}
