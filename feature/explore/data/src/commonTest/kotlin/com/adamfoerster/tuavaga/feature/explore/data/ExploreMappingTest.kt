package com.adamfoerster.tuavaga.feature.explore.data

import com.adamfoerster.tuavaga.core.data.util.toDbTimestamp
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.spot.ApprovalMode
import com.adamfoerster.tuavaga.core.domain.spot.DayOverride
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import com.adamfoerster.tuavaga.core.domain.spot.TimeWindow
import com.adamfoerster.tuavaga.feature.explore.domain.BookingError
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExploreMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun searchRowBecomesAListing() {
        val dto = json.decodeFromString<ListingDto>(
            """
            {"id":"s1","level_id":"l2","level_name":"Subsolo 2","level_position":2,"sector_name":"B","number":"27",
             "size_label":"2,5 × 5,0","description":"Perto do elevador","features":["coberta","eletrica"],"height_cm":210,
             "directions":"Desça a rampa","price_hour_cents":800,"price_day_cents":3500,"price_week_cents":null,
             "min_period_minutes":120,"cancel_notice_hours":24,"approval":"manual","rules":["Sem caminhonete"],
             "owner_name":"Marina Ribeiro","owner_block":"A","owner_unit":"31","is_mine":false,"available":true,
             "weekly":[{"weekday":1,"start":"08:00","end":"18:00"},{"weekday":7,"start":"00:00","end":"24:00"}]}
            """,
        )

        val listing = dto.toListing()

        assertEquals("B2-27", listing.code)
        assertEquals(setOf(SpotFeature.COVERED, SpotFeature.ELECTRIC), listing.features)
        assertEquals(ApprovalMode.MANUAL, listing.approval)
        assertEquals(TimeWindow(480, 1080), listing.weekly[DayOfWeek.MONDAY])
        assertEquals(TimeWindow(0, 1440), listing.weekly[DayOfWeek.SUNDAY])
        assertEquals(true, listing.isBookable)
    }

    @Test
    fun availabilityRowsFromPostgres() {
        val availability = availabilityOf(
            weekly = listOf(WeeklyRowDto(2, "08:00:00", "18:00:00")),
            overrides = listOf(OverrideRowDto("2026-10-12", "blocked"), OverrideRowDto("2026-10-17", "open", "09:00:00", "12:00:00")),
        )

        assertEquals(TimeWindow(480, 1080), availability.weekly[DayOfWeek.TUESDAY])
        assertEquals(DayOverride.Blocked, availability.overrides[LocalDate(2026, 10, 12)])
        assertEquals(DayOverride.Open(TimeWindow(540, 720)), availability.overrides[LocalDate(2026, 10, 17)])
    }

    @Test
    fun timestampsTravelAsInstantsInBrasiliaTime() {
        assertEquals("2026-10-10T11:00:00Z", LocalDateTime(2026, 10, 10, 8, 0).toDbTimestamp())
        val period = RangeDto("2026-10-10T11:00:00+00:00", "2026-10-11T21:00:00+00:00").toPeriod()
        assertEquals(LocalDateTime(2026, 10, 10, 8, 0), period.start)
        assertEquals(LocalDateTime(2026, 10, 11, 18, 0), period.end)
    }

    @Test
    fun bookingResultAndErrors() {
        assertEquals(BookingStatus.CONFIRMED, BookingResultDto("b1", 1001, "confirmed", 7000).toConfirmation().status)
        assertEquals(BookingStatus.PENDING, BookingResultDto("b1", 1002, "pending", 2000).toConfirmation().status)
        assertEquals(BookingError.SpotUnavailable, bookingErrorFromMessage("spot_unavailable"))
        assertEquals(BookingError.BelowMinimum, bookingErrorFromMessage("ERROR: below_minimum"))
        assertNull(bookingErrorFromMessage("something else"))
    }
}
