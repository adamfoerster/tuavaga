package com.adamfoerster.tuavaga.core.domain.booking

import com.adamfoerster.tuavaga.core.domain.spot.spotCode
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Error
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

/** Which side of the booking the signed-in user is. */
enum class BookingRole { RENTER, OWNER }

/** Mirror of the database's `reject_reason` (board 17 "Motivo da recusa"). */
enum class RejectReason(val key: String, val label: String) {
    VISIT("visita", "Vaga reservada para visita"),
    OWN_USE("uso", "Vou usar a vaga"),
    VEHICLE("veiculo", "Veículo não cabe"),
    OTHER("outro", "Outro motivo"),
    ;

    companion object {
        fun fromKey(key: String?): RejectReason? = entries.firstOrNull { it.key == key }
    }
}

/** The renter's vehicle as the owner sees it ("Onix preto · FGH2J34 · carro"). */
data class BookingVehicle(val plate: String, val model: String?, val color: String?, val type: VehicleType?)

/** The other party: the owner for the renter, the renter for the owner. */
data class Counterpart(val name: String?, val block: String?, val unit: String?)

/** A booking of the signed-in user, as renter or as owner, with what the screens show. */
data class Booking(
    val id: String,
    /** "Reserva 4821". */
    val code: Long,
    val role: BookingRole,
    val status: BookingStatus,
    val condoId: String,
    val condoName: String,
    val spotId: String,
    val levelName: String,
    val sectorName: String?,
    val spotNumber: String,
    val directions: String?,
    val rules: List<String>,
    val cancelNoticeHours: Int,
    val period: BookingPeriod,
    val quote: BookingQuote,
    val note: String?,
    val rejectReason: RejectReason?,
    val rejectMessage: String?,
    /** Deadline for the owner to answer a pending request. */
    val respondBy: LocalDateTime?,
    /** For cancelled bookings: `true` when the owner cancelled. */
    val cancelledByOwner: Boolean?,
    val checkedInAt: LocalDateTime?,
    val checkedOutAt: LocalDateTime?,
    val counterpart: Counterpart,
    val vehicle: BookingVehicle?,
    /** Pending requests only: a confirmed booking of the same spot overlapping this one (board 17). */
    val conflict: BookingPeriod?,
) {
    val spotCode: String get() = spotCode(levelName, sectorName, spotNumber)
    val totalCents: Int get() = quote.totalCents
}

sealed interface BookingActionError : Error {
    /** The booking changed meanwhile (answered, cancelled, expired…). */
    data object InvalidState : BookingActionError

    /** Approving would overlap a confirmed booking. */
    data object Conflict : BookingActionError

    /** Renter cancelling a confirmed booking after the notice period. */
    data object CancelWindowClosed : BookingActionError

    /** Check-in outside "30 min before the start" → "end". */
    data object CheckInClosed : BookingActionError

    /** Extension outside the availability or overlapping another booking. */
    data object SpotUnavailable : BookingActionError
    data object InvalidPeriod : BookingActionError
    data object NotFound : BookingActionError
    data class Remote(val error: DataError.Remote) : BookingActionError
}

/**
 * The user's bookings (renter and owner). [bookings] is the local cache, so the lists open offline;
 * [refresh] reloads it from the server, and every action refreshes it when it succeeds.
 */
interface BookingRepository {
    val bookings: Flow<List<Booking>>

    suspend fun refresh(): EmptyResult<DataError.Remote>

    suspend fun approve(bookingId: String): EmptyResult<BookingActionError>

    suspend fun reject(bookingId: String, reason: RejectReason, message: String?): EmptyResult<BookingActionError>

    suspend fun cancel(bookingId: String): EmptyResult<BookingActionError>

    suspend fun checkIn(bookingId: String): EmptyResult<BookingActionError>

    suspend fun checkOut(bookingId: String): EmptyResult<BookingActionError>

    suspend fun extend(bookingId: String, newEnd: LocalDateTime): EmptyResult<BookingActionError>
}
