package com.adamfoerster.tuavaga.feature.hosting.presentation.agenda

import com.adamfoerster.tuavaga.core.designsystem.components.KbDayCell
import com.adamfoerster.tuavaga.core.designsystem.components.KbDayState
import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.spot.DayAvailability
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.core.presentation.firstOfMonth
import com.adamfoerster.tuavaga.core.presentation.namePt
import com.adamfoerster.tuavaga.feature.hosting.domain.Spot
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

/** Bookings that hold the spot (RES) and requests still waiting (PED) show on the agenda. */
private val AGENDA = setOf(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED)
private val HOLDS = setOf(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED)

data class AgendaState(
    val today: LocalDate,
    /** First day of the month shown. */
    val month: LocalDate,
    val spot: Spot? = null,
    /** Owner bookings of this spot (every month). */
    val bookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiText? = null,
) {
    private val monthPeriod: BookingPeriod
        get() = BookingPeriod(
            LocalDateTime(month, LocalTime(0, 0)),
            LocalDateTime(month.plus(1, DateTimeUnit.MONTH), LocalTime(0, 0)),
        )

    /** The month's bookings and requests, by start (board 28 list). */
    val monthBookings: List<Booking>
        get() = bookings.filter { it.status in AGENDA && it.period.overlaps(monthPeriod) }.sortedBy { it.period.start }

    val canGoBack: Boolean get() = month > today.firstOfMonth()

    /** RES = held by a booking, PED = a request waiting for you, BLQ = blocked, muted = closed or past. */
    fun cells(): List<KbDayCell> {
        val held = monthBookings.filter { it.status in HOLDS }.flatMap { it.period.days() }.toSet()
        val requested = monthBookings.filter { it.status == BookingStatus.PENDING }.flatMap { it.period.days() }.toSet()
        val availability = spot?.availability
        val next = month.plus(1, DateTimeUnit.MONTH)
        return generateSequence(month) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it < next }.map { date ->
            val (state, mark, what) = when {
                date in held -> Triple(KbDayState.Booked, "RES", "reservada")
                date < today -> Triple(KbDayState.Past, "", "passado")
                date in requested -> Triple(KbDayState.Free, "PED", "com pedido")
                else -> when (availability?.on(date)) {
                    DayAvailability.Blocked -> Triple(KbDayState.Blocked, "BLQ", "bloqueada")
                    is DayAvailability.Open -> Triple(KbDayState.Free, if (date == today) "HOJE" else "", "livre")
                    else -> Triple(KbDayState.Closed, "", "fechada")
                }
            }
            KbDayCell(day = date.day, state = state, mark = mark, enabled = false, description = "${date.day} de ${date.month.namePt()}, $what")
        }.toList()
    }
}

sealed interface AgendaAction {
    data object OnPreviousMonth : AgendaAction
    data object OnNextMonth : AgendaAction
    data object OnRetry : AgendaAction

    // Navigation, handled by the Root.
    data object OnBackClick : AgendaAction
    data object OnEditClick : AgendaAction
    data object OnRequestsClick : AgendaAction
    data class OnBookingClick(val bookingId: String) : AgendaAction
}
