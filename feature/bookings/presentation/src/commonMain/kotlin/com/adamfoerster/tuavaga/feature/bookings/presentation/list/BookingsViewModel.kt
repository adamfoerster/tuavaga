package com.adamfoerster.tuavaga.feature.bookings.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.BookingTab
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.presentation.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Board 12 · Reservas: the cached list right away, then a refresh from the server. */
class BookingsViewModel(
    private val bookingRepository: BookingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BookingsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            bookingRepository.bookings.collect { all ->
                val mine = all.filter { it.role == BookingRole.RENTER }
                _state.update { state ->
                    // Open on "Em curso" when the user is parked right now.
                    val tab = if (state.bookings.isEmpty() && mine.any { it.status == BookingStatus.IN_PROGRESS }) {
                        BookingTab.ONGOING
                    } else {
                        state.tab
                    }
                    state.copy(bookings = mine, tab = tab)
                }
            }
        }
        refresh()
    }

    fun onAction(action: BookingsAction) {
        when (action) {
            is BookingsAction.OnTabSelect -> _state.update { it.copy(tab = action.tab) }
            BookingsAction.OnRefresh -> refresh()
            is BookingsAction.OnBookingClick, BookingsAction.OnExploreClick -> Unit
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val result = bookingRepository.refresh()
            _state.update {
                it.copy(isRefreshing = false, error = (result as? Result.Failure)?.error?.toUiText())
            }
        }
    }
}
