package com.adamfoerster.tuavaga.core.presentation

import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingQuote
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class BookingTextsTest {

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
}
