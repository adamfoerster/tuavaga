package com.adamfoerster.tuavaga.core.domain.spot

/** How the owner accepts requests. */
enum class ApprovalMode { MANUAL, AUTO }

/** Prices in cents; `null` = that period is not offered. */
data class Prices(val hourCents: Int? = null, val dayCents: Int? = null, val weekCents: Int? = null) {
    val isEmpty: Boolean get() = hourCents == null && dayCents == null && weekCents == null

    fun of(unit: BillingUnit): Int? = when (unit) {
        BillingUnit.HOUR -> hourCents
        BillingUnit.DAY -> dayCents
        BillingUnit.WEEK -> weekCents
    }

    /** Units offered, in the order the design lists them (Hora, Dia, Semana). */
    val offeredUnits: List<BillingUnit> get() = BillingUnit.entries.filter { of(it) != null }
}

/** What a booking is charged by; partial units round up (34 h by the day = 2 days). */
enum class BillingUnit(val minutes: Int) {
    HOUR(60),
    DAY(24 * 60),
    WEEK(7 * 24 * 60),
    ;

    fun unitsFor(durationMinutes: Int): Int = (durationMinutes + minutes - 1) / minutes
}

/** Characteristics shown as tags and used as filters (boards 04 and 07). Stored by [key]. */
enum class SpotFeature(val key: String, val label: String) {
    COVERED("coberta", "Coberta"),
    WIDE("larga", "Larga"),
    ELECTRIC("eletrica", "Elétrica"),
    NEAR_ELEVATOR("elevador", "Elevador"),
    MOTORCYCLE("moto", "Moto"),
    ;

    companion object {
        fun fromKey(key: String): SpotFeature? = entries.firstOrNull { it.key == key }
    }
}
