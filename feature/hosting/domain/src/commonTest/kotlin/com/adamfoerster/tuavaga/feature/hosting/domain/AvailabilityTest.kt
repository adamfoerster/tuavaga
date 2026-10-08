package com.adamfoerster.tuavaga.feature.hosting.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvailabilityTest {

    private val business = TimeWindow(8 * 60, 18 * 60)
    private val morning = TimeWindow(8 * 60, 12 * 60)
    private val monday = LocalDate(2026, 10, 12)
    private val saturday = LocalDate(2026, 10, 17)

    private val weekdays = Availability().withWeekly(RepeatFrequency.WEEKDAYS.days, business)

    @Test
    fun weeklyRuleOpensWeekdaysOnly() {
        assertEquals(DayAvailability.Open(business), weekdays.on(monday))
        assertEquals(DayAvailability.Closed, weekdays.on(saturday))
    }

    @Test
    fun blockingWinsOverTheWeeklyRule() {
        val blocked = weekdays.block(listOf(monday))

        assertEquals(DayAvailability.Blocked, blocked.on(monday))
    }

    @Test
    fun openingADayOutsideTheRuleUsesTheGivenWindow() {
        val opened = weekdays.open(listOf(saturday), morning)

        assertEquals(DayAvailability.Open(morning), opened.on(saturday))
    }

    @Test
    fun reopeningABlockedWeekdayWithTheSameWindowRemovesTheException() {
        val reopened = weekdays.block(listOf(monday)).open(listOf(monday), business)

        assertEquals(DayAvailability.Open(business), reopened.on(monday))
        assertTrue(reopened.overrides.isEmpty())
    }

    @Test
    fun newWeeklyRuleAbsorbsMatchingOpenedDatesButKeepsBlocks() {
        val edited = Availability()
            .open(listOf(saturday), business)
            .block(listOf(monday))
            .withWeekly(RepeatFrequency.EVERY_DAY.days, business)

        assertEquals(mapOf(monday to DayOverride.Blocked), edited.overrides)
        assertEquals(7, edited.weekly.size)
    }

    @Test
    fun onlySelectedDaysMeansNoWeeklyRule() {
        val custom = weekdays.withWeekly(RepeatFrequency.ONLY_SELECTED.days, business)

        assertTrue(custom.weekly.isEmpty())
        assertTrue(custom.isEmpty)
        assertFalse(custom.open(listOf(saturday), morning).isEmpty)
    }

    @Test
    fun pastExceptionsAreDropped() {
        val withPast = weekdays.block(listOf(LocalDate(2026, 10, 1), monday))

        assertEquals(setOf(monday), withPast.withoutPastOverrides(LocalDate(2026, 10, 8)).overrides.keys)
    }

    @Test
    fun weekendsPresetHasSaturdayAndSunday() {
        assertEquals(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), RepeatFrequency.WEEKENDS.days)
    }
}
