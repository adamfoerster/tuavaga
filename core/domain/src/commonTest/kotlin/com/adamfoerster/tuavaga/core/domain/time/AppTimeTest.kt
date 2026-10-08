package com.adamfoerster.tuavaga.core.domain.time

import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

class AppTimeTest {

    private val lateUtc = object : Clock {
        // 02:30 UTC on Oct 9 is still 23:30 on Oct 8 in Brasília.
        override fun now() = Instant.parse("2026-10-09T02:30:00Z")
    }

    /** A named zone (`TimeZone.of("America/Sao_Paulo")`) crashes the web app: no tz database on wasmJs. */
    @Test
    fun usesAFixedBrasiliaOffset() {
        val zone = assertIs<FixedOffsetTimeZone>(APP_TIME_ZONE)
        assertEquals(UtcOffset(hours = -3), zone.offset)
    }

    @Test
    fun todayAndNowFollowBrasilia() {
        assertEquals(LocalDate(2026, 10, 8), appToday(lateUtc))
        assertEquals(LocalDateTime(2026, 10, 8, 23, 30), appNow(lateUtc))
    }
}
