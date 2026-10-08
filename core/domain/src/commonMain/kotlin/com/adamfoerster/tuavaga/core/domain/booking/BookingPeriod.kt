package com.adamfoerster.tuavaga.core.domain.booking

import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.spot.Prices
import com.adamfoerster.tuavaga.core.domain.time.APP_TIME_ZONE
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

/** Entrada and Saída of a booking, in Brasília local time. */
data class BookingPeriod(val start: LocalDateTime, val end: LocalDateTime) {
    val isValid: Boolean get() = start < end

    val durationMinutes: Int
        get() = ((end.toInstant(APP_TIME_ZONE) - start.toInstant(APP_TIME_ZONE)).inWholeMinutes).toInt()

    fun overlaps(other: BookingPeriod): Boolean = start < other.end && other.start < end

    /** The local dates this period touches (end exclusive: 10/10 08:00 → 11/10 00:00 is only 10/10). */
    fun days(): List<LocalDate> {
        val last = end.date.let { if (end.time == LocalTime(0, 0)) it.plus(-1, DateTimeUnit.DAY) else it }
        return generateSequence(start.date) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= last }.toList()
    }

    companion object {
        /**
         * Default search period: the next full hour for two hours; late at night (22:00 or later),
         * tomorrow 08:00–10:00.
         */
        fun defaultFrom(now: LocalDateTime): BookingPeriod {
            val startDate = if (now.hour >= 22) now.date.plus(1, DateTimeUnit.DAY) else now.date
            val startHour = if (now.hour >= 22) 8 else now.hour + 1
            val start = LocalDateTime(startDate, LocalTime(startHour, 0))
            return BookingPeriod(start, start.plusMinutes(120))
        }
    }
}

fun LocalDateTime.plusMinutes(minutes: Int): LocalDateTime =
    (toInstant(APP_TIME_ZONE) + minutes.minutes).toLocalDateTime(APP_TIME_ZONE)

/** What the renter pays: [units] × [unitPriceCents] (mirror of the database's billing_units). */
data class BookingQuote(val unit: BillingUnit, val units: Int, val unitPriceCents: Int) {
    val totalCents: Int get() = units * unitPriceCents
}

/** Quote for [period] charged by [unit]; `null` when the spot does not offer that unit or the period is empty. */
fun Prices.quote(period: BookingPeriod, unit: BillingUnit): BookingQuote? {
    val price = of(unit) ?: return null
    if (!period.isValid) return null
    return BookingQuote(unit, unit.unitsFor(period.durationMinutes), price)
}

/** The unit the design shows first for a period: days from 24 h, weeks from 7 days, else hours — if offered. */
fun Prices.suggestedUnit(period: BookingPeriod): BillingUnit? {
    val preferred = when {
        period.durationMinutes >= BillingUnit.WEEK.minutes -> listOf(BillingUnit.WEEK, BillingUnit.DAY, BillingUnit.HOUR)
        period.durationMinutes >= BillingUnit.DAY.minutes -> listOf(BillingUnit.DAY, BillingUnit.HOUR, BillingUnit.WEEK)
        else -> listOf(BillingUnit.HOUR, BillingUnit.DAY, BillingUnit.WEEK)
    }
    return preferred.firstOrNull { of(it) != null }
}
