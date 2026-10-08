package com.adamfoerster.tuavaga.feature.hosting.domain

/** Parsing and display of the values typed in the spot wizard (Brazilian formats). */
object SpotFormats {

    private val timeRegex = Regex("""^(\d{1,2})(?:[:h](\d{2}))?h?$""")

    /**
     * "8", "8,00", "1.234,50", "R$ 35" → cents; `null` if empty, not a number, zero or too big.
     * A dot followed by 1–2 digits at the end, with no comma ("8.50"), is taken as the decimal mark.
     */
    fun parsePriceCents(input: String): Int? {
        val compact = input.replace("R$", "").replace(" ", "").trim()
        val normalized = if (',' !in compact && Regex("""^\d+\.\d{1,2}$""").matches(compact)) compact.replace('.', ',') else compact
        val cleaned = normalized.replace(".", "")
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split(',')
        if (parts.size > 2 || parts.any { part -> part.any { !it.isDigit() } }) return null
        val reais = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
        val centsPart = parts.getOrNull(1).orEmpty()
        if (centsPart.length > 2) return null
        val cents = reais * 100 + centsPart.padEnd(2, '0').ifEmpty { "0" }.toLong()
        return cents.takeIf { it in 1..MAX_PRICE_CENTS }?.toInt()
    }

    /** 800 → "8,00"; 18000 → "180". */
    fun formatPriceInput(cents: Int): String =
        if (cents % 100 == 0) (cents / 100).toString() else "${cents / 100},${(cents % 100).toString().padStart(2, '0')}"

    /** 800 → "R$ 8"; 850 → "R$ 8,50". */
    fun formatPrice(cents: Int): String = "R$ ${formatPriceInput(cents)}"

    /** "08:00", "8", "8h", "18h30" → minutes from midnight; "24:00" is the end of the day. */
    fun parseTime(input: String): Int? {
        val match = timeRegex.matchEntire(input.trim().lowercase()) ?: return null
        val hours = match.groupValues[1].toInt()
        val minutes = match.groupValues[2].ifEmpty { "0" }.toInt()
        if (minutes > 59) return null
        val total = hours * 60 + minutes
        return total.takeIf { it in 0..TimeWindow.MINUTES_PER_DAY }
    }

    /** 480 → "08:00". */
    fun formatTime(minutes: Int): String =
        "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"

    /** The cheapest unit for the card meta: "R$ 8/H", else "R$ 35/DIA", else "R$ 180/SEMANA". */
    fun shortPrice(prices: Prices): String? = when {
        prices.hourCents != null -> "${formatPrice(prices.hourCents)}/h"
        prices.dayCents != null -> "${formatPrice(prices.dayCents)}/dia"
        prices.weekCents != null -> "${formatPrice(prices.weekCents)}/semana"
        else -> null
    }

    /** 60 → "1 h", 1440 → "1 dia". */
    fun formatMinPeriod(minutes: Int): String = if (minutes % 1440 == 0) {
        val days = minutes / 1440
        if (days == 1) "1 dia" else "$days dias"
    } else {
        "${minutes / 60} h"
    }

    const val MAX_PRICE_CENTS = 10_000_000L
}

/**
 * Short code of a spot as the design shows it: sector + level number + "-" + spot number,
 * e.g. ("Subsolo 2", "B", "27") → "B2-27"; levels without a digit use their initial ("Térreo" → T).
 */
fun spotCode(levelName: String, sectorName: String?, number: String): String {
    val level = levelName.filter { it.isDigit() }.ifEmpty { levelName.trim().take(1).uppercase() }
    val sector = sectorName?.trim()?.uppercase().orEmpty()
    val n = number.trim().uppercase().let { if (it.length == 1 && it[0].isDigit()) "0$it" else it }
    return "$sector$level-$n"
}
