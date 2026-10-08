package com.adamfoerster.tuavaga.feature.bookings.presentation

import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingQuote
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.BookingVehicle
import com.adamfoerster.tuavaga.core.domain.booking.Counterpart
import com.adamfoerster.tuavaga.core.domain.booking.RejectReason
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDateTime

/** Board 10: reserva 4821, Sáb 10/10 08:00 → 18:00, vaga B2-27 da Marina. */
fun booking(
    id: String = "b1",
    role: BookingRole = BookingRole.RENTER,
    status: BookingStatus = BookingStatus.CONFIRMED,
    start: LocalDateTime = LocalDateTime(2026, 10, 10, 8, 0),
    end: LocalDateTime = LocalDateTime(2026, 10, 10, 18, 0),
    condoId: String = "c1",
    unit: BillingUnit = BillingUnit.HOUR,
    unitPriceCents: Int = 800,
) = Booking(
    id = id, code = 4821, role = role, status = status, condoId = condoId, condoName = "Residencial Alameda Verde",
    spotId = "s27", levelName = "Subsolo 2", sectorName = "B", spotNumber = "27",
    directions = "Desça a rampa até o subsolo 2.", rules = listOf("Sem caminhonete", "Respeitar o horário", "Não lavar"),
    cancelNoticeHours = 24, period = BookingPeriod(start, end),
    quote = BookingQuote(unit, unit.unitsFor(((end.hour - start.hour) * 60).coerceAtLeast(60)), unitPriceCents),
    note = null, rejectReason = null, rejectMessage = null, respondBy = null, cancelledByOwner = null,
    checkedInAt = null, checkedOutAt = null, counterpart = Counterpart("Marina Ribeiro", "A", "31"),
    vehicle = BookingVehicle("ABC1D23", "Onix", "Preto", VehicleType.CAR), conflict = null,
)

/** In-memory bookings: actions change the list like the server would (and are recorded). */
class FakeBookings(vararg initial: Booking) : BookingRepository {
    override val bookings = MutableStateFlow(initial.toList())
    var refreshError: DataError.Remote? = null
    var actionError: BookingActionError? = null
    var refreshes = 0
    val calls = mutableListOf<String>()

    override suspend fun refresh(): EmptyResult<DataError.Remote> {
        refreshes++
        return refreshError?.let { Result.Failure(it) } ?: Result.Success(Unit)
    }

    private fun act(name: String, id: String, change: (Booking) -> Booking): EmptyResult<BookingActionError> {
        calls += "$name:$id"
        actionError?.let { return Result.Failure(it) }
        bookings.value = bookings.value.map { if (it.id == id) change(it) else it }
        return Result.Success(Unit)
    }

    override suspend fun approve(bookingId: String) = act("approve", bookingId) { it.copy(status = BookingStatus.CONFIRMED) }

    override suspend fun reject(bookingId: String, reason: RejectReason, message: String?) = act("reject", bookingId) {
        it.copy(status = BookingStatus.REJECTED, rejectReason = reason, rejectMessage = message)
    }

    override suspend fun cancel(bookingId: String) = act("cancel", bookingId) { it.copy(status = BookingStatus.CANCELLED) }

    override suspend fun checkIn(bookingId: String) = act("checkIn", bookingId) { it.copy(status = BookingStatus.IN_PROGRESS) }

    override suspend fun checkOut(bookingId: String) = act("checkOut", bookingId) { it.copy(status = BookingStatus.COMPLETED) }

    override suspend fun extend(bookingId: String, newEnd: LocalDateTime) = act("extend", bookingId) {
        it.copy(period = it.period.copy(end = newEnd))
    }
}
