package com.adamfoerster.tuavaga.feature.explore.presentation.common

import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.spot.TimeWindow
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.domain.BookingQuote
import com.adamfoerster.tuavaga.feature.explore.presentation.NOW
import com.adamfoerster.tuavaga.feature.explore.presentation.listing
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ExploreTextsTest {

    @Test
    fun money() {
        assertEquals("R$ 70,00", formatMoney(7000))
        assertEquals("R$ 8,05", formatMoney(805))
        assertEquals("R$ 0,50", formatMoney(50))
    }

    @Test
    fun units() {
        assertEquals("2 diárias × R$ 35,00", BookingQuote(BillingUnit.DAY, 2, 3500).breakdown())
        assertEquals("1 hora", BookingQuote(BillingUnit.HOUR, 1, 800).unitsLabel())
        assertEquals("3 semanas", BookingQuote(BillingUnit.WEEK, 3, 18000).unitsLabel())
    }

    @Test
    fun periods() {
        val weekend = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 11, 18, 0))
        assertEquals("34 h na vaga", weekend.durationLabel())
        assertEquals(
            "2 h 30 min na vaga",
            BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 10, 10, 30)).durationLabel(),
        )
        assertEquals("Sáb 10/10 → Dom 11/10", weekend.rangeLabel())
    }

    @Test
    fun weeklyRules() {
        val day = TimeWindow(8 * 60, 18 * 60)
        val allDay = TimeWindow(0, TimeWindow.MINUTES_PER_DAY)
        val weekdays = DayOfWeek.entries.take(5)
        assertEquals("livre em datas específicas", weeklySummary(emptyMap()))
        assertEquals("livre das 08:00 às 18:00", weeklySummary(weekdays.associateWith { day }))
        assertEquals("livre o fim de semana", weeklySummary(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).associateWith { allDay }))
        assertEquals("livre a qualquer hora", weeklySummary(DayOfWeek.entries.associateWith { allDay }))
        assertEquals("horários variados", weeklySummary(mapOf(DayOfWeek.MONDAY to day, DayOfWeek.TUESDAY to allDay)))
    }

    @Test
    fun ownerUsesTheFirstName() {
        assertEquals("Marina · Bloco A", listing("27").ownerLine())
        assertEquals("SUBSOLO 2 · SETOR B · PERTO DO ELEVADOR", listing("27").metaLine())
    }

    @Test
    fun periodValidation() {
        val ok = BookingPeriod(LocalDateTime(2026, 10, 8, 15, 0), LocalDateTime(2026, 10, 8, 17, 0))
        assertNull(validatePeriod(ok, NOW))
        // A few minutes late still counts as "now".
        assertNull(validatePeriod(BookingPeriod(LocalDateTime(2026, 10, 8, 14, 30), LocalDateTime(2026, 10, 8, 16, 0)), NOW))
        assertNotNull(validatePeriod(BookingPeriod(LocalDateTime(2026, 10, 8, 12, 0), LocalDateTime(2026, 10, 8, 16, 0)), NOW))
        assertNotNull(validatePeriod(BookingPeriod(ok.end, ok.start), NOW))
        assertNotNull(validatePeriod(BookingPeriod(LocalDateTime(2026, 12, 20, 8, 0), LocalDateTime(2026, 12, 20, 10, 0)), NOW))
    }
}
