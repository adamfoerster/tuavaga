package com.adamfoerster.tuavaga.core.domain.booking

import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.spot.Prices
import com.adamfoerster.tuavaga.core.domain.user.shortName
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookingPeriodTest {

    // Design example: Sáb 10/10 08:00 → Dom 11/10 18:00 = 34 h = 2 diárias × R$ 35 = R$ 70.
    private val weekend = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 11, 18, 0))
    private val prices = Prices(hourCents = 800, dayCents = 3500, weekCents = 18000)

    @Test
    fun designExampleCostsTwoDays() {
        assertEquals(34 * 60, weekend.durationMinutes)
        val quote = prices.quote(weekend, BillingUnit.DAY)!!
        assertEquals(2, quote.units)
        assertEquals(7000, quote.totalCents)
        assertEquals(34 * 800, prices.quote(weekend, BillingUnit.HOUR)!!.totalCents)
    }

    @Test
    fun unitNotOfferedHasNoQuote() {
        assertNull(Prices(hourCents = 800).quote(weekend, BillingUnit.DAY))
        assertNull(prices.quote(BookingPeriod(weekend.end, weekend.start), BillingUnit.HOUR))
    }

    @Test
    fun suggestedUnitFollowsTheLengthAndWhatIsOffered() {
        assertEquals(BillingUnit.DAY, prices.suggestedUnit(weekend))
        val twoHours = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 10, 10, 0))
        assertEquals(BillingUnit.HOUR, prices.suggestedUnit(twoHours))
        assertEquals(BillingUnit.DAY, Prices(dayCents = 3500).suggestedUnit(twoHours))
        assertNull(Prices().suggestedUnit(twoHours))
    }

    @Test
    fun daysTouchedEndExclusive() {
        assertEquals(listOf(LocalDate(2026, 10, 10), LocalDate(2026, 10, 11)), weekend.days())
        val untilMidnight = BookingPeriod(LocalDateTime(2026, 10, 10, 20, 0), LocalDateTime(2026, 10, 11, 0, 0))
        assertEquals(listOf(LocalDate(2026, 10, 10)), untilMidnight.days())
    }

    @Test
    fun overlapIsEndExclusive() {
        val after = BookingPeriod(weekend.end, LocalDateTime(2026, 10, 11, 20, 0))
        assertFalse(weekend.overlaps(after))
        assertTrue(weekend.overlaps(BookingPeriod(LocalDateTime(2026, 10, 11, 17, 0), LocalDateTime(2026, 10, 11, 19, 0))))
    }

    @Test
    fun defaultPeriodIsTheNextTwoHours() {
        assertEquals(
            BookingPeriod(LocalDateTime(2026, 10, 8, 15, 0), LocalDateTime(2026, 10, 8, 17, 0)),
            BookingPeriod.defaultFrom(LocalDateTime(2026, 10, 8, 14, 37)),
        )
        // Late at night it moves to tomorrow morning.
        assertEquals(
            BookingPeriod(LocalDateTime(2026, 10, 9, 8, 0), LocalDateTime(2026, 10, 9, 10, 0)),
            BookingPeriod.defaultFrom(LocalDateTime(2026, 10, 8, 23, 10)),
        )
        // 21:xx → 22:00–24:00 crosses midnight correctly.
        assertEquals(LocalDateTime(2026, 10, 9, 0, 0), BookingPeriod.defaultFrom(LocalDateTime(2026, 10, 8, 21, 5)).end)
    }

    @Test
    fun shortNamesLikeTheDesign() {
        assertEquals("Marina R.", shortName("Marina Ribeiro"))
        assertEquals("Paulo T.", shortName("  Paulo Henrique  Teixeira "))
        assertEquals("Rafael", shortName("Rafael"))
        assertNull(shortName(" "))
        assertNull(shortName(null))
    }
}
