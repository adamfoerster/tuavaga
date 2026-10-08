package com.adamfoerster.tuavaga.feature.explore.presentation.common

import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.spot.RepeatFrequency
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import com.adamfoerster.tuavaga.core.domain.spot.SpotFormats
import com.adamfoerster.tuavaga.core.domain.spot.TimeWindow
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.core.presentation.full
import com.adamfoerster.tuavaga.core.presentation.shortPt
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.core.presentation.weekdayDayMonth
import com.adamfoerster.tuavaga.feature.explore.domain.BookingError
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.domain.BookingQuote
import com.adamfoerster.tuavaga.feature.explore.domain.SpotListing
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

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

/** The cheapest offered price for list cards: (cents, unit). */
fun SpotListing.headlinePrice(): Pair<Int, BillingUnit>? =
    prices.offeredUnits.firstOrNull()?.let { unit -> prices.of(unit)!! to unit }

/** "SUBSOLO 2 · SETOR B · PERTO DO ELEVADOR" (description, else size). */
fun SpotListing.metaLine(): String = listOfNotNull(
    levelName,
    sectorName?.let { "Setor $it" },
    description ?: sizeLabel?.let { "$it m" },
).joinToString(" · ").uppercase()

/** "Marina · Bloco A" (first name only, like the cards of board 04). */
fun SpotListing.ownerLine(): String? {
    val firstName = ownerName?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotEmpty() }
    return listOfNotNull(firstName, ownerBlock?.let { "Bloco $it" }).joinToString(" · ").ifEmpty { null }
}

/** Short description of the weekly rule: "livre das 08:00 às 18:00", "livre o fim de semana"… */
fun weeklySummary(weekly: Map<DayOfWeek, TimeWindow>): String {
    if (weekly.isEmpty()) return "livre em datas específicas"
    val windows = weekly.values.toSet()
    if (windows.size > 1) return "horários variados"
    val window = windows.single()
    val fullDay = window == TimeWindow(0, TimeWindow.MINUTES_PER_DAY)
    val hours = "das ${SpotFormats.formatTime(window.startMinutes)} às ${SpotFormats.formatTime(window.endMinutes)}"
    val days = weekly.keys
    return when (days) {
        RepeatFrequency.EVERY_DAY.days -> if (fullDay) "livre a qualquer hora" else "livre todos os dias $hours"
        RepeatFrequency.WEEKDAYS.days -> if (fullDay) "livre em dias úteis" else "livre $hours"
        RepeatFrequency.WEEKENDS.days -> if (fullDay) "livre o fim de semana" else "livre no fim de semana $hours"
        else -> {
            val list = days.sortedBy { it.isoDayNumber }.joinToString(", ") { it.shortPt().lowercase() }
            if (fullDay) "livre $list" else "livre $list $hours"
        }
    }
}

/** "TIPO · MOTO" / "TIPO · CARRO" aside of the detail panel. */
fun SpotListing.vehicleKind(): String = if (SpotFeature.MOTORCYCLE in features) "Moto" else "Carro"

fun BookingError.toUiText(): UiText = when (this) {
    BookingError.SpotUnavailable -> UiText.Dynamic("Essa vaga não está mais livre nesse período. Escolha outro horário ou outra vaga.")
    BookingError.BelowMinimum -> UiText.Dynamic("O período é menor que o mínimo desta vaga.")
    BookingError.UnitNotOffered -> UiText.Dynamic("Esta vaga não é cobrada nesse período. Escolha outra forma de cobrança.")
    BookingError.OwnSpot -> UiText.Dynamic("Esta vaga é sua.")
    BookingError.InvalidVehicle -> UiText.Dynamic("Escolha um dos seus veículos.")
    BookingError.InvalidPeriod -> UiText.Dynamic("A entrada já passou. Escolha um horário a partir de agora.")
    is BookingError.Remote -> error.toUiText()
}
