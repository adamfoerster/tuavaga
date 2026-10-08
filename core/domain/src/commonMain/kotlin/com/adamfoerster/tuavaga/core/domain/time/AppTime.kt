package com.adamfoerster.tuavaga.core.domain.time

import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * The app's time zone: Brasília, as a fixed UTC−3 offset (Brazil has had no daylight saving since
 * 2019). Named zones like "America/Sao_Paulo" need a timezone database that kotlinx-datetime does
 * not ship on wasmJs — `TimeZone.of` throws there and crashes the web app.
 */
val APP_TIME_ZONE: TimeZone = FixedOffsetTimeZone(UtcOffset(hours = -3))

fun appToday(clock: Clock = Clock.System): LocalDate = clock.todayIn(APP_TIME_ZONE)

fun appNow(clock: Clock = Clock.System): LocalDateTime = clock.now().toLocalDateTime(APP_TIME_ZONE)
