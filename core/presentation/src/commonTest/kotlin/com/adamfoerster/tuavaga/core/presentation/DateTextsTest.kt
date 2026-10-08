package com.adamfoerster.tuavaga.core.presentation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTextsTest {

    private val saturday = LocalDateTime(2026, 10, 10, 8, 5)

    @Test
    fun dates() {
        assertEquals("Sáb 10/10 · 08:05", saturday.full())
        assertEquals("10/10 · 08:05", saturday.short())
        assertEquals("Dom 04/01", LocalDate(2026, 1, 4).weekdayDayMonth())
    }

    @Test
    fun months() {
        val october = LocalDate(2026, 10, 17)
        assertEquals(LocalDate(2026, 10, 1), october.firstOfMonth())
        assertEquals("Outubro 2026", october.monthTitle())
        assertEquals("OUT", october.monthShort())
        // 01/10/2026 is a Thursday: three blank cells (Mon–Wed) before it.
        assertEquals(3, october.leadingBlanks())
    }
}
