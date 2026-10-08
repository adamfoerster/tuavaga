package com.adamfoerster.tuavaga.feature.hosting.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpotFormatsTest {

    @Test
    fun pricesInBrazilianFormat() {
        assertEquals(800, SpotFormats.parsePriceCents("8,00"))
        assertEquals(850, SpotFormats.parsePriceCents("8,5"))
        assertEquals(18000, SpotFormats.parsePriceCents("180"))
        assertEquals(123450, SpotFormats.parsePriceCents("1.234,50"))
        assertEquals(3500, SpotFormats.parsePriceCents("R$ 35"))
        assertEquals(850, SpotFormats.parsePriceCents("8.50"))
        assertEquals(123400, SpotFormats.parsePriceCents("1.234"))
    }

    @Test
    fun invalidPricesAreRejected() {
        assertNull(SpotFormats.parsePriceCents(""))
        assertNull(SpotFormats.parsePriceCents("0"))
        assertNull(SpotFormats.parsePriceCents("8,999"))
        assertNull(SpotFormats.parsePriceCents("abc"))
        assertNull(SpotFormats.parsePriceCents("-5"))
        assertNull(SpotFormats.parsePriceCents("999999999"))
    }

    @Test
    fun pricesAreShownLikeTheDesign() {
        assertEquals("8", SpotFormats.formatPriceInput(800))
        assertEquals("8,50", SpotFormats.formatPriceInput(850))
        assertEquals("R$ 35", SpotFormats.formatPrice(3500))
        assertEquals("R$ 8/h", SpotFormats.shortPrice(Prices(hourCents = 800, dayCents = 3500)))
        assertEquals("R$ 35/dia", SpotFormats.shortPrice(Prices(dayCents = 3500)))
        assertNull(SpotFormats.shortPrice(Prices()))
    }

    @Test
    fun timesAcceptCommonShapes() {
        assertEquals(480, SpotFormats.parseTime("08:00"))
        assertEquals(480, SpotFormats.parseTime("8"))
        assertEquals(1110, SpotFormats.parseTime("18h30"))
        assertEquals(1440, SpotFormats.parseTime("24:00"))
        assertNull(SpotFormats.parseTime("25:00"))
        assertNull(SpotFormats.parseTime("8:75"))
        assertEquals("08:00", SpotFormats.formatTime(480))
    }

    @Test
    fun minPeriodLabels() {
        assertEquals("2 h", SpotFormats.formatMinPeriod(120))
        assertEquals("1 dia", SpotFormats.formatMinPeriod(1440))
    }

    @Test
    fun spotCodesFollowTheDesign() {
        assertEquals("B2-27", spotCode("Subsolo 2", "B", "27"))
        assertEquals("A1-04", spotCode("Subsolo 1", "A", "4"))
        assertEquals("T-12", spotCode("Térreo", null, "12"))
    }
}
