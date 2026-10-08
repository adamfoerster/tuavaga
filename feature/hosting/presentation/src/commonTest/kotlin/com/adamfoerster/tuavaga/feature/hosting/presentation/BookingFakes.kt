package com.adamfoerster.tuavaga.feature.hosting.presentation

import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingQuote
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.Counterpart
import com.adamfoerster.tuavaga.core.domain.booking.RejectReason
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDateTime

/** A booking of the user's spot "spot1" (owner side): 2 diárias × R$ 35 by default. */
fun ownerBooking(
    id: String,
    status: BookingStatus = BookingStatus.CONFIRMED,
    start: LocalDateTime = LocalDateTime(2026, 10, 10, 8, 0),
    end: LocalDateTime = LocalDateTime(2026, 10, 11, 18, 0),
    spotId: String = "spot1",
    role: BookingRole = BookingRole.OWNER,
    respondBy: LocalDateTime? = null,
) = Booking(
    id = id, code = 4821, role = role, status = status, condoId = "c1", condoName = "Residencial Alameda Verde",
    spotId = spotId, levelName = "Subsolo 2", sectorName = "B", spotNumber = "14", directions = null, rules = emptyList(),
    cancelNoticeHours = 24, period = BookingPeriod(start, end), quote = BookingQuote(BillingUnit.DAY, 2, 3500), note = null,
    rejectReason = null, rejectMessage = null, respondBy = respondBy, cancelledByOwner = null, checkedInAt = null,
    checkedOutAt = null, counterpart = Counterpart("Rafael Souza", "C", "12"), vehicle = null, conflict = null,
)

class FakeBookings(vararg initial: Booking) : BookingRepository {
    override val bookings = MutableStateFlow(initial.toList())
    var actionError: BookingActionError? = null
    var refreshes = 0
    val calls = mutableListOf<String>()

    override suspend fun refresh(): EmptyResult<DataError.Remote> {
        refreshes++
        return Result.Success(Unit)
    }

    private fun act(name: String, id: String, status: BookingStatus): EmptyResult<BookingActionError> {
        calls += "$name:$id"
        actionError?.let { return Result.Failure(it) }
        bookings.value = bookings.value.map { if (it.id == id) it.copy(status = status) else it }
        return Result.Success(Unit)
    }

    override suspend fun approve(bookingId: String) = act("approve", bookingId, BookingStatus.CONFIRMED)

    override suspend fun reject(bookingId: String, reason: RejectReason, message: String?) =
        act("reject:${reason.key}:$message", bookingId, BookingStatus.REJECTED)

    override suspend fun cancel(bookingId: String) = act("cancel", bookingId, BookingStatus.CANCELLED)

    override suspend fun checkIn(bookingId: String) = act("checkIn", bookingId, BookingStatus.IN_PROGRESS)

    override suspend fun checkOut(bookingId: String) = act("checkOut", bookingId, BookingStatus.COMPLETED)

    override suspend fun extend(bookingId: String, newEnd: LocalDateTime) = act("extend", bookingId, BookingStatus.IN_PROGRESS)
}
