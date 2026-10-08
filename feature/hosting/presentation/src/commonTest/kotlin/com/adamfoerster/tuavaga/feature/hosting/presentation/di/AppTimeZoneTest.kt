package com.adamfoerster.tuavaga.feature.hosting.presentation.di

import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

class AppTimeZoneTest {

    /** A named zone (`TimeZone.of("America/Sao_Paulo")`) crashes the web app: no tz database on wasmJs. */
    @Test
    fun usesAFixedBrasiliaOffset() {
        val zone = assertIs<FixedOffsetTimeZone>(APP_TIME_ZONE)
        assertEquals(UtcOffset(hours = -3), zone.offset)
    }

    @Test
    fun todayChangesAtMidnightInBrasilia() {
        // 02:30 UTC on Oct 9 is still 23:30 on Oct 8 in Brasília.
        val clock = object : Clock {
            override fun now() = Instant.parse("2026-10-09T02:30:00Z")
        }
        assertEquals(LocalDate(2026, 10, 8), clock.todayIn(APP_TIME_ZONE))
    }
}
