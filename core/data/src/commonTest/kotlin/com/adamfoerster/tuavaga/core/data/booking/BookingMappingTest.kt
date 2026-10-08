package com.adamfoerster.tuavaga.core.data.booking

import com.adamfoerster.tuavaga.core.data.util.fromDbTimestamp
import com.adamfoerster.tuavaga.core.data.util.toDbTimestamp
import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.RejectReason
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookingMappingTest {

    // What PostgREST returns for one row of my_bookings (owner view of a pending request).
    private val row = """
        {"id":"b1","code":4821,"role":"owner","status":"pending","condo_id":"c1",
         "condo_name":"Residencial Alameda Verde","spot_id":"s1","level_name":"Subsolo 2","sector_name":"B",
         "spot_number":"14","directions":null,"rules":["Sem caminhonete"],"cancel_notice_hours":24,
         "starts_at":"2026-10-10T11:00:00+00:00","ends_at":"2026-10-11T21:00:00+00:00","billing_unit":"day",
         "units":2,"unit_price_cents":3500,"total_cents":7000,"note":"Chego cedo","reject_reason":null,
         "reject_message":null,"respond_by":"2026-10-09T02:10:00+00:00","cancelled_by_owner":null,
         "checked_in_at":null,"checked_out_at":null,"counterpart_name":"Rafael Souza","counterpart_block":"C",
         "counterpart_unit":"12","vehicle_plate":"FGH2J34","vehicle_model":"Onix","vehicle_color":"Preto",
         "vehicle_type":"carro","conflict_starts_at":"2026-10-11T17:00:00+00:00",
         "conflict_ends_at":"2026-10-11T19:00:00+00:00"}
    """.trimIndent()

    @Test
    fun rowBecomesBooking() {
        val booking = Json.decodeFromString<BookingRowDto>(row).toBooking()

        assertEquals(BookingRole.OWNER, booking.role)
        assertEquals(BookingStatus.PENDING, booking.status)
        assertEquals("B2-14", booking.spotCode)
        assertEquals(BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 11, 18, 0)), booking.period)
        assertEquals(BillingUnit.DAY, booking.quote.unit)
        assertEquals(7000, booking.totalCents)
        assertEquals(LocalDateTime(2026, 10, 8, 23, 10), booking.respondBy)
        assertEquals("Rafael Souza", booking.counterpart.name)
        assertEquals(VehicleType.CAR, booking.vehicle?.type)
        assertEquals(LocalDateTime(2026, 10, 11, 14, 0), booking.conflict?.start)
    }

    @Test
    fun cacheRoundTripAndUnreadableCache() {
        val dto = Json.decodeFromString<BookingRowDto>(row)
        val cached = Json.encodeToString(listOf(dto, dto.copy(id = "b2", status = "rejected", rejectReason = "visita")))

        val bookings = decodeBookings(cached)
        assertEquals(listOf("b1", "b2"), bookings.map { it.id })
        assertEquals(RejectReason.VISIT, bookings[1].rejectReason)
        assertTrue(decodeBookings("{not json").isEmpty())
    }

    @Test
    fun errors() {
        assertEquals(BookingActionError.Conflict, bookingActionErrorFromMessage("conflict"))
        assertEquals(BookingActionError.CancelWindowClosed, bookingActionErrorFromMessage("cancel_window_closed"))
        assertEquals(BookingActionError.InvalidState, bookingActionErrorFromMessage("invalid_state"))
        assertEquals(BookingActionError.CheckInClosed, bookingActionErrorFromMessage("check_in_closed"))
        assertEquals(BookingActionError.NotFound, bookingActionErrorFromMessage("booking_not_found"))
        assertNull(bookingActionErrorFromMessage("timeout"))
    }

    @Test
    fun timestamps() {
        assertEquals("2026-10-10T11:00:00Z", LocalDateTime(2026, 10, 10, 8, 0).toDbTimestamp())
        assertEquals(LocalDateTime(2026, 10, 10, 8, 0), fromDbTimestamp("2026-10-10T11:00:00+00:00"))
    }
}
