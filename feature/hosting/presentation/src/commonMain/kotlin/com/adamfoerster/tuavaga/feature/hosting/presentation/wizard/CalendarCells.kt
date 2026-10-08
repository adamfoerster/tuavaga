package com.adamfoerster.tuavaga.feature.hosting.presentation.wizard

import com.adamfoerster.tuavaga.core.designsystem.components.KbDayCell
import com.adamfoerster.tuavaga.core.designsystem.components.KbDayState
import com.adamfoerster.tuavaga.feature.hosting.domain.Availability
import com.adamfoerster.tuavaga.feature.hosting.domain.DayAvailability
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotFormats
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.plus

private val monthNames = listOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
)

fun Month.namePt(): String = monthNames[number - 1]

/** "Outubro 2026". */
fun LocalDate.monthTitle(): String = "${month.namePt().replaceFirstChar { it.uppercase() }} $year"

/** "SET" */
fun LocalDate.monthShort(): String = month.namePt().take(3).uppercase()

/** Empty cells before day 1 in a Monday-first grid. */
fun LocalDate.leadingBlanks(): Int = firstOfMonth().dayOfWeek.isoDayNumber - 1

/** Days of [month]'s month as calendar cells: past, selected, blocked, free or closed, plus "HOJE". */
fun calendarCells(
    month: LocalDate,
    today: LocalDate,
    availability: Availability,
    selected: Set<LocalDate>,
): List<KbDayCell> {
    val first = month.firstOfMonth()
    val next = first.plus(1, DateTimeUnit.MONTH)
    return generateSequence(first) { it.plus(1, DateTimeUnit.DAY) }
        .takeWhile { it < next }
        .map { date ->
            val availabilityOfDay = availability.on(date)
            val (state, mark) = when {
                date < today -> KbDayState.Past to ""
                date in selected -> KbDayState.Selected to "SEL"
                availabilityOfDay == DayAvailability.Blocked -> KbDayState.Blocked to "BLQ"
                availabilityOfDay is DayAvailability.Open -> KbDayState.Free to if (date == today) "HOJE" else ""
                else -> KbDayState.Closed to if (date == today) "HOJE" else ""
            }
            KbDayCell(
                day = date.day,
                state = state,
                mark = mark,
                enabled = date >= today,
                description = describe(date, state, availabilityOfDay),
            )
        }
        .toList()
}

private fun describe(date: LocalDate, state: KbDayState, availability: DayAvailability): String {
    val what = when (state) {
        KbDayState.Past -> "passado"
        KbDayState.Selected -> "selecionado"
        KbDayState.Blocked -> "bloqueado"
        KbDayState.Booked -> "reservado"
        KbDayState.Free -> (availability as DayAvailability.Open).window.let {
            "livre das ${SpotFormats.formatTime(it.startMinutes)} às ${SpotFormats.formatTime(it.endMinutes)}"
        }
        KbDayState.Closed -> "fechado"
    }
    return "${date.day} de ${date.month.namePt()}, $what"
}
