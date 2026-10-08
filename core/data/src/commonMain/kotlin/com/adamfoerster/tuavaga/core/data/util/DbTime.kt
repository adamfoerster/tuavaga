package com.adamfoerster.tuavaga.core.data.util

import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.time.APP_TIME_ZONE
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Timestamps go to Postgres as instants (UTC); local times are Brasília. */
fun LocalDateTime.toDbTimestamp(): String = toInstant(APP_TIME_ZONE).toString()

/** A `timestamptz` from PostgREST ("2026-10-10T11:00:00+00:00") as Brasília local time. */
fun fromDbTimestamp(value: String): LocalDateTime = Instant.parse(value).toLocalDateTime(APP_TIME_ZONE)

/** Mirror of the database's `billing_unit`. */
fun BillingUnit.toDb(): String = when (this) {
    BillingUnit.HOUR -> "hour"
    BillingUnit.DAY -> "day"
    BillingUnit.WEEK -> "week"
}

fun billingUnitFromDb(value: String): BillingUnit = when (value) {
    "hour" -> BillingUnit.HOUR
    "day" -> BillingUnit.DAY
    else -> BillingUnit.WEEK
}
