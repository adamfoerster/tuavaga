package com.adamfoerster.tuavaga.feature.hosting.presentation.myspots

import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.ownerBookingsIn
import com.adamfoerster.tuavaga.core.domain.booking.pendingRequests
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.feature.hosting.domain.Spot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/** The user's spots in one of their condominiums (empty list = "Nenhuma vaga sua neste condomínio"). */
data class CondoSpots(val condoId: String, val condoName: String, val spots: List<Spot>)

data class MySpotsState(
    val today: LocalDate,
    val groups: List<CondoSpots> = emptyList(),
    /** Bookings of the user's spots (owner side), from the bookings cache. */
    val ownerBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiText? = null,
    /** Spot whose pause/reactivate request is running. */
    val updatingSpotId: String? = null,
) {
    private val monthBookings: List<Booking> get() = ownerBookings.ownerBookingsIn(today.year, today.month.number)

    /** "Ganhos do mês": confirmed, in progress or completed bookings starting this month. */
    val monthEarningsCents: Int get() = monthBookings.sumOf { it.totalCents }
    val monthBookingCount: Int get() = monthBookings.size

    /** Requests waiting for an answer (board 17). */
    val pendingCount: Int get() = ownerBookings.pendingRequests().size

    fun pendingFor(spotId: String): Int = ownerBookings.pendingRequests().count { it.spotId == spotId }
}

sealed interface MySpotsAction {
    data object OnRefresh : MySpotsAction
    data class OnToggleStatus(val spotId: String) : MySpotsAction

    // Navigation, handled by the Root.
    /** `null` = the active condominium. */
    data class OnCreateSpot(val condoId: String?) : MySpotsAction
    data class OnEditSpot(val spotId: String) : MySpotsAction
    data class OnAgendaClick(val spotId: String) : MySpotsAction
    data object OnRequestsClick : MySpotsAction
    data object OnWantSpotClick : MySpotsAction
}
