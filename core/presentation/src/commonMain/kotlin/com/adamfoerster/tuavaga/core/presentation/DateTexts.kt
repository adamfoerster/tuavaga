package com.adamfoerster.tuavaga.core.presentation

import com.adamfoerster.tuavaga.core.domain.spot.SpotFormats
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

// Brazilian Portuguese date texts used across features (calendars, periods, bookings).

private val monthNames = listOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
)

private val weekdayShort = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

fun Month.namePt(): String = monthNames[number - 1]

/** "Seg" … "Dom". */
fun DayOfWeek.shortPt(): String = weekdayShort[isoDayNumber - 1]

fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, month, 1)

/** "Outubro 2026". */
fun LocalDate.monthTitle(): String = "${month.namePt().replaceFirstChar { it.uppercase() }} $year"

/** "SET" */
fun LocalDate.monthShort(): String = month.namePt().take(3).uppercase()

/** Empty cells before day 1 in a Monday-first month grid. */
fun LocalDate.leadingBlanks(): Int = firstOfMonth().dayOfWeek.isoDayNumber - 1

/** "10/10" */
fun LocalDate.dayMonth(): String = "${day.toString().padStart(2, '0')}/${month.number.toString().padStart(2, '0')}"

/** "Sáb 10/10" */
fun LocalDate.weekdayDayMonth(): String = "${dayOfWeek.shortPt()} ${dayMonth()}"

/** "Sáb 10/10 · 08:00" */
fun LocalDateTime.full(): String = "${date.weekdayDayMonth()} · ${hhmm()}"

/** "10/10 · 08:00" */
fun LocalDateTime.short(): String = "${date.dayMonth()} · ${hhmm()}"

/** "08:00" */
fun LocalDateTime.hhmm(): String = SpotFormats.formatTime(hour * 60 + minute)
