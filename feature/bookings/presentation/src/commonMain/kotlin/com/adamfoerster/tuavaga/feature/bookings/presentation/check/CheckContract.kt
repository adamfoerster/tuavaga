package com.adamfoerster.tuavaga.feature.bookings.presentation.check

import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingQuote
import com.adamfoerster.tuavaga.core.domain.booking.minutesBetween
import com.adamfoerster.tuavaga.core.domain.booking.plusMinutes
import com.adamfoerster.tuavaga.core.presentation.UiText
import kotlinx.datetime.LocalDateTime

/** Board 24 (check-in) or 25 (check-out). */
enum class CheckKind { IN, OUT }

data class CheckState(
    val kind: CheckKind,
    val now: LocalDateTime,
    val booking: Booking? = null,
    /** Indexes of the confirmations ticked (all three are required). */
    val checked: Set<Int> = emptySet(),
    val isWorking: Boolean = false,
    val error: UiText? = null,
    /** "Preciso de mais tempo" sheet (check-out only). */
    val isExtendOpen: Boolean = false,
    val extendTo: LocalDateTime? = null,
) {
    val canConfirm: Boolean get() = booking != null && checked.size == CONFIRMATIONS && !isWorking

    /**
     * New exit options: [EXTEND_STEPS] times, 30 min apart, counted from the current exit and all
     * later than now (when already late, the first one is the next half hour step after now).
     */
    val extendOptions: List<LocalDateTime>
        get() = booking?.let { b ->
            val late = minutesBetween(b.period.end, now)
            val first = if (late < 0) 1 else late / 30 + 1
            (first until first + EXTEND_STEPS).map { b.period.end.plusMinutes(it * 30) }
        }.orEmpty()

    /** What the booking costs with the exit at [end] (same billing unit). */
    fun quoteFor(end: LocalDateTime): BookingQuote? = booking?.let { b ->
        b.quote.copy(units = b.quote.unit.unitsFor(minutesBetween(b.period.start, end)))
    }

    companion object {
        const val CONFIRMATIONS = 3
        const val EXTEND_STEPS = 12
    }
}

sealed interface CheckAction {
    data class OnToggle(val index: Int) : CheckAction
    data object OnConfirmClick : CheckAction
    data object OnTick : CheckAction
    data object OnExtendClick : CheckAction
    data class OnExtendSelect(val end: LocalDateTime) : CheckAction
    data object OnExtendConfirm : CheckAction
    data object OnExtendDismiss : CheckAction

    // Navigation, handled by the Root.
    data object OnBackClick : CheckAction
}

sealed interface CheckEvent {
    /** Check-in or check-out registered: back to the booking. */
    data object Done : CheckEvent
}
