package com.adamfoerster.tuavaga.feature.messages.presentation.common

import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.presentation.dayMonth
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.number

/** "10/10", "10–11/10" or "30/10–02/11" (chat header and conversation cards). */
fun BookingPeriod.periodShort(): String {
    val first = start.date
    // An exit at midnight belongs to the day before.
    val last = if (end.time == LocalTime(0, 0) && end.date > start.date) end.date.minus(1, DateTimeUnit.DAY) else end.date
    return when {
        first == last -> first.dayMonth()
        first.month == last.month && first.year == last.year ->
            "${first.day.toString().padStart(2, '0')}–${last.day.toString().padStart(2, '0')}/${last.month.number.toString().padStart(2, '0')}"
        else -> "${first.dayMonth()}–${last.dayMonth()}"
    }
}
