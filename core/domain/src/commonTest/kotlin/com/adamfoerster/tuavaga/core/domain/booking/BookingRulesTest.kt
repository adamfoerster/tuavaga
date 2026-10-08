package com.adamfoerster.tuavaga.core.domain.booking

import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookingRulesTest {

    // Board 10: Sáb 10/10 08:00 → 18:00, cancelamento sem aviso até 09/10 às 08:00 (24 h antes).
    private val booking = Booking(
        id = "b1", code = 4821, role = BookingRole.RENTER, status = BookingStatus.CONFIRMED,
        condoId = "c1", condoName = "Residencial Alameda Verde", spotId = "s1", levelName = "Subsolo 2",
        sectorName = "B", spotNumber = "27", directions = null, rules = emptyList(), cancelNoticeHours = 24,
        period = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 10, 18, 0)),
        quote = BookingQuote(BillingUnit.HOUR, 10, 800), note = null, rejectReason = null, rejectMessage = null,
        respondBy = null, cancelledByOwner = null, checkedInAt = null, checkedOutAt = null,
        counterpart = Counterpart("Marina Ribeiro", "A", "31"), vehicle = null, conflict = null,
    )

    @Test
    fun cancellationNotice() {
        assertEquals(LocalDateTime(2026, 10, 9, 8, 0), booking.cancelDeadline)
        assertTrue(booking.canCancel(LocalDateTime(2026, 10, 9, 8, 0)))
        assertFalse(booking.canCancel(LocalDateTime(2026, 10, 9, 8, 1)))
        assertTrue(booking.isPastCancelDeadline(LocalDateTime(2026, 10, 9, 8, 1)))
        // A pending request can always be withdrawn.
        assertTrue(booking.copy(status = BookingStatus.PENDING).canCancel(LocalDateTime(2026, 10, 10, 7, 0)))
        // The owner cancels until the start.
        val owner = booking.copy(role = BookingRole.OWNER)
        assertTrue(owner.canCancel(LocalDateTime(2026, 10, 10, 7, 59)))
        assertFalse(owner.canCancel(LocalDateTime(2026, 10, 10, 8, 0)))
    }

    @Test
    fun checkInOpensHalfAnHourBefore() {
        assertFalse(booking.canCheckIn(LocalDateTime(2026, 10, 10, 7, 29)))
        assertTrue(booking.canCheckIn(LocalDateTime(2026, 10, 10, 7, 30)))
        assertTrue(booking.canCheckIn(LocalDateTime(2026, 10, 10, 17, 59)))
        assertFalse(booking.canCheckIn(LocalDateTime(2026, 10, 10, 18, 0)))
        assertFalse(booking.copy(role = BookingRole.OWNER).canCheckIn(LocalDateTime(2026, 10, 10, 9, 0)))
    }

    @Test
    fun parkedBookings() {
        val parked = booking.copy(status = BookingStatus.IN_PROGRESS)
        assertTrue(parked.canCheckOut)
        assertTrue(parked.canExtend)
        assertEquals(BookingTab.ONGOING, parked.tab)
        assertEquals(0, parked.minutesLate(LocalDateTime(2026, 10, 10, 17, 42)))
        assertEquals(42, parked.minutesLate(LocalDateTime(2026, 10, 10, 18, 42)))
    }

    @Test
    fun tabs() {
        assertEquals(BookingTab.UPCOMING, booking.tab)
        assertEquals(BookingTab.UPCOMING, booking.copy(status = BookingStatus.PENDING).tab)
        BookingStatus.entries.filterNot { it.isActive }.forEach {
            assertEquals(BookingTab.HISTORY, booking.copy(status = it).tab)
        }
    }

    @Test
    fun ownerEarningsAndRequests() {
        val owner = booking.copy(role = BookingRole.OWNER)
        val list = listOf(
            owner,
            owner.copy(id = "b2", status = BookingStatus.COMPLETED, spotId = "s2"),
            owner.copy(id = "b3", status = BookingStatus.CANCELLED),
            owner.copy(id = "b4", period = BookingPeriod(LocalDateTime(2026, 11, 1, 8, 0), LocalDateTime(2026, 11, 1, 9, 0))),
            booking.copy(id = "b5"),
            owner.copy(id = "p2", status = BookingStatus.PENDING, respondBy = LocalDateTime(2026, 10, 9, 20, 0)),
            owner.copy(id = "p1", status = BookingStatus.PENDING, respondBy = LocalDateTime(2026, 10, 9, 10, 0)),
        )
        assertEquals(listOf("b1", "b2"), list.ownerBookingsIn(2026, 10).map { it.id })
        assertEquals(listOf("b2"), list.ownerBookingsIn(2026, 10, spotId = "s2").map { it.id })
        assertEquals(listOf("p1", "p2"), list.pendingRequests().map { it.id })
    }
}
