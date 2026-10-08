package com.adamfoerster.tuavaga.feature.explore.presentation.detail

import com.adamfoerster.tuavaga.core.designsystem.components.KbDayCell
import com.adamfoerster.tuavaga.core.designsystem.components.KbDayState
import com.adamfoerster.tuavaga.core.domain.spot.Availability
import com.adamfoerster.tuavaga.core.domain.spot.DayAvailability
import com.adamfoerster.tuavaga.core.presentation.firstOfMonth
import com.adamfoerster.tuavaga.core.presentation.namePt
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

/** The whole month of [month] as a period (for loading the confirmed bookings). */
fun monthRange(month: LocalDate): BookingPeriod {
    val first = month.firstOfMonth()
    return BookingPeriod(LocalDateTime(first, LocalTime(0, 0)), LocalDateTime(first.plus(1, DateTimeUnit.MONTH), LocalTime(0, 0)))
}

/**
 * Board 07 "Disponibilidade": SEL = days of the chosen period, RES = days with a confirmed booking,
 * BLQ = blocked, plain = open, muted = closed or past.
 */
fun detailCalendarCells(
    month: LocalDate,
    today: LocalDate,
    availability: Availability,
    busy: List<BookingPeriod>,
    period: BookingPeriod,
): List<KbDayCell> {
    val first = month.firstOfMonth()
    val next = first.plus(1, DateTimeUnit.MONTH)
    val chosenDays = period.days().toSet()
    val bookedDays = busy.flatMap { it.days() }.toSet()
    return generateSequence(first) { it.plus(1, DateTimeUnit.DAY) }
        .takeWhile { it < next }
        .map { date ->
            val (state, mark, what) = when {
                date < today -> Triple(KbDayState.Past, "", "passado")
                date in chosenDays -> Triple(KbDayState.Selected, "SEL", "no seu período")
                date in bookedDays -> Triple(KbDayState.Booked, "RES", "com reserva")
                else -> when (availability.on(date)) {
                    DayAvailability.Blocked -> Triple(KbDayState.Blocked, "BLQ", "bloqueado")
                    is DayAvailability.Open -> Triple(KbDayState.Free, if (date == today) "HOJE" else "", "livre")
                    DayAvailability.Closed -> Triple(KbDayState.Closed, if (date == today) "HOJE" else "", "fechado")
                }
            }
            KbDayCell(
                day = date.day,
                state = state,
                mark = mark,
                enabled = false,
                description = "${date.day} de ${date.month.namePt()}, $what",
            )
        }
        .toList()
}
