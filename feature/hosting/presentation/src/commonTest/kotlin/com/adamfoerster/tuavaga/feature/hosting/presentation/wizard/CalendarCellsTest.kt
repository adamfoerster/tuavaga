package com.adamfoerster.tuavaga.feature.hosting.presentation.wizard

import com.adamfoerster.tuavaga.core.designsystem.components.KbDayState
import com.adamfoerster.tuavaga.feature.hosting.domain.Availability
import com.adamfoerster.tuavaga.feature.hosting.domain.RepeatFrequency
import com.adamfoerster.tuavaga.feature.hosting.domain.TimeWindow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CalendarCellsTest {

    private val today = LocalDate(2026, 10, 5) // Monday, like the design's "HOJE"
    private val month = LocalDate(2026, 10, 1)
    private val availability = Availability()
        .withWeekly(RepeatFrequency.WEEKDAYS.days, TimeWindow(480, 1080))
        .block(listOf(LocalDate(2026, 10, 12)))

    private val cells = calendarCells(month, today, availability, selected = setOf(LocalDate(2026, 10, 20)))

    @Test
    fun october2026StartsOnThursday() {
        assertEquals(3, month.leadingBlanks())
        assertEquals(31, cells.size)
        assertEquals("Outubro 2026", month.monthTitle())
        assertEquals("SET", LocalDate(2026, 9, 1).monthShort())
    }

    @Test
    fun statesFollowAvailabilityAndSelection() {
        fun cell(day: Int) = cells[day - 1]

        assertEquals(KbDayState.Past, cell(1).state)
        assertFalse(cell(1).enabled)
        assertEquals(KbDayState.Free to "HOJE", cell(5).state to cell(5).mark)
        assertEquals(KbDayState.Blocked to "BLQ", cell(12).state to cell(12).mark)
        assertEquals(KbDayState.Selected to "SEL", cell(20).state to cell(20).mark)
        assertEquals(KbDayState.Closed, cell(10).state) // Saturday, outside the weekly rule
        assertEquals("6 de outubro, livre das 08:00 às 18:00", cell(6).description)
    }
}
