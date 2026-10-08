package com.adamfoerster.tuavaga.core.presentation

import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingQuote
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.spot.SpotFormats

// Money and period texts shared by Explorar, Reservas and Minhas vagas.

/** "R$ 70,00" — totals always show cents (board 08). */
fun formatMoney(cents: Int): String =
    "R$ ${cents / 100},${(cents % 100).toString().padStart(2, '0')}"

/** "/h", "/dia", "/semana" */
fun BillingUnit.suffix(): String = when (this) {
    BillingUnit.HOUR -> "/h"
    BillingUnit.DAY -> "/dia"
    BillingUnit.WEEK -> "/semana"
}

/** Toolbar label: "Hora", "Dia", "Semana". */
fun BillingUnit.label(): String = when (this) {
    BillingUnit.HOUR -> "Hora"
    BillingUnit.DAY -> "Dia"
    BillingUnit.WEEK -> "Semana"
}

/** "1 hora", "2 diárias", "1 semana". */
fun BookingQuote.unitsLabel(): String = when (unit) {
    BillingUnit.HOUR -> if (units == 1) "1 hora" else "$units horas"
    BillingUnit.DAY -> if (units == 1) "1 diária" else "$units diárias"
    BillingUnit.WEEK -> if (units == 1) "1 semana" else "$units semanas"
}

/** "2 diárias × R$ 35,00" */
fun BookingQuote.breakdown(): String = "${unitsLabel()} × ${formatMoney(unitPriceCents)}"

/** "34 h na vaga", "2 h 30 min na vaga". */
fun BookingPeriod.durationLabel(): String {
    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60
    return when {
        hours == 0 -> "$minutes min na vaga"
        minutes == 0 -> "$hours h na vaga"
        else -> "$hours h $minutes min na vaga"
    }
}

/** "Sáb 10/10 → Dom 11/10" or, on the same day, "Sáb 10/10 · 08:00 → 18:00". */
fun BookingPeriod.rangeLabel(): String =
    if (start.date == end.date) {
        "${start.full()} → ${SpotFormats.formatTime(end.hour * 60 + end.minute)}"
    } else {
        "${start.date.weekdayDayMonth()} → ${end.date.weekdayDayMonth()}"
    }

