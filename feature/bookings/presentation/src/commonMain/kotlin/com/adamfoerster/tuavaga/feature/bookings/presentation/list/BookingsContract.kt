package com.adamfoerster.tuavaga.feature.bookings.presentation.list

import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingTab
import com.adamfoerster.tuavaga.core.domain.booking.tab
import com.adamfoerster.tuavaga.core.presentation.UiText

data class BookingsState(
    val tab: BookingTab = BookingTab.UPCOMING,
    /** The user's bookings as renter (owner bookings live in Minhas vagas). */
    val bookings: List<Booking> = emptyList(),
    /** Until the first refresh answers; the cached list shows meanwhile. */
    val isRefreshing: Boolean = true,
    /** Refresh failed: the cached list stays on screen with a warning. */
    val error: UiText? = null,
) {
    /** Upcoming and ongoing by start; history most recent first. */
    val visible: List<Booking>
        get() = bookings.filter { it.tab == tab }
            .let { list -> if (tab == BookingTab.HISTORY) list.sortedByDescending { it.period.start } else list.sortedBy { it.period.start } }

    fun count(tab: BookingTab): Int = bookings.count { it.tab == tab }

    /** "3 PRÓXIMAS · 2 CONDOMÍNIOS". */
    val condoCount: Int get() = visible.map { it.condoId }.distinct().size
}

sealed interface BookingsAction {
    data class OnTabSelect(val tab: BookingTab) : BookingsAction
    data object OnRefresh : BookingsAction

    // Navigation, handled by the Root.
    data class OnBookingClick(val bookingId: String) : BookingsAction
    data object OnExploreClick : BookingsAction
}
