package com.adamfoerster.tuavaga.core.data.booking

import com.adamfoerster.tuavaga.core.data.util.billingUnitFromDb
import com.adamfoerster.tuavaga.core.data.util.fromDbTimestamp
import com.adamfoerster.tuavaga.core.data.vehicle.vehicleTypeFromDb
import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingQuote
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.BookingVehicle
import com.adamfoerster.tuavaga.core.domain.booking.Counterpart
import com.adamfoerster.tuavaga.core.domain.booking.RejectReason
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row of `my_bookings` (supabase/migrations/20261011000000_booking_lifecycle.sql); also the cached JSON. */
@Serializable
internal data class BookingRowDto(
    val id: String,
    val code: Long,
    val role: String,
    val status: String,
    @SerialName("condo_id") val condoId: String,
    @SerialName("condo_name") val condoName: String,
    @SerialName("spot_id") val spotId: String,
    @SerialName("level_name") val levelName: String,
    @SerialName("sector_name") val sectorName: String? = null,
    @SerialName("spot_number") val spotNumber: String,
    val directions: String? = null,
    val rules: List<String> = emptyList(),
    @SerialName("cancel_notice_hours") val cancelNoticeHours: Int,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    @SerialName("billing_unit") val billingUnit: String,
    val units: Int,
    @SerialName("unit_price_cents") val unitPriceCents: Int,
    @SerialName("total_cents") val totalCents: Int,
    val note: String? = null,
    @SerialName("reject_reason") val rejectReason: String? = null,
    @SerialName("reject_message") val rejectMessage: String? = null,
    @SerialName("respond_by") val respondBy: String? = null,
    @SerialName("cancelled_by_owner") val cancelledByOwner: Boolean? = null,
    @SerialName("checked_in_at") val checkedInAt: String? = null,
    @SerialName("checked_out_at") val checkedOutAt: String? = null,
    @SerialName("counterpart_name") val counterpartName: String? = null,
    @SerialName("counterpart_block") val counterpartBlock: String? = null,
    @SerialName("counterpart_unit") val counterpartUnit: String? = null,
    @SerialName("vehicle_plate") val vehiclePlate: String? = null,
    @SerialName("vehicle_model") val vehicleModel: String? = null,
    @SerialName("vehicle_color") val vehicleColor: String? = null,
    @SerialName("vehicle_type") val vehicleType: String? = null,
    @SerialName("conflict_starts_at") val conflictStartsAt: String? = null,
    @SerialName("conflict_ends_at") val conflictEndsAt: String? = null,
)

internal fun bookingStatusFromDb(value: String): BookingStatus = when (value) {
    "pending" -> BookingStatus.PENDING
    "confirmed" -> BookingStatus.CONFIRMED
    "rejected" -> BookingStatus.REJECTED
    "cancelled" -> BookingStatus.CANCELLED
    "expired" -> BookingStatus.EXPIRED
    "in_progress" -> BookingStatus.IN_PROGRESS
    else -> BookingStatus.COMPLETED
}

internal fun BookingRowDto.toBooking() = Booking(
    id = id,
    code = code,
    role = if (role == "owner") BookingRole.OWNER else BookingRole.RENTER,
    status = bookingStatusFromDb(status),
    condoId = condoId,
    condoName = condoName,
    spotId = spotId,
    levelName = levelName,
    sectorName = sectorName,
    spotNumber = spotNumber,
    directions = directions,
    rules = rules,
    cancelNoticeHours = cancelNoticeHours,
    period = BookingPeriod(fromDbTimestamp(startsAt), fromDbTimestamp(endsAt)),
    quote = BookingQuote(billingUnitFromDb(billingUnit), units, unitPriceCents),
    note = note,
    rejectReason = RejectReason.fromKey(rejectReason),
    rejectMessage = rejectMessage,
    respondBy = respondBy?.let(::fromDbTimestamp),
    cancelledByOwner = cancelledByOwner,
    checkedInAt = checkedInAt?.let(::fromDbTimestamp),
    checkedOutAt = checkedOutAt?.let(::fromDbTimestamp),
    counterpart = Counterpart(counterpartName, counterpartBlock, counterpartUnit),
    vehicle = vehiclePlate?.let { BookingVehicle(it, vehicleModel, vehicleColor, vehicleType?.let(::vehicleTypeFromDb)) },
    conflict = conflictStartsAt?.let { start ->
        conflictEndsAt?.let { end -> BookingPeriod(fromDbTimestamp(start), fromDbTimestamp(end)) }
    },
)

/** The lifecycle RPCs raise stable messages (see the migration); anything else is a remote error. */
internal fun bookingActionErrorFromMessage(message: String?): BookingActionError? = when {
    message == null -> null
    "booking_not_found" in message -> BookingActionError.NotFound
    "invalid_state" in message -> BookingActionError.InvalidState
    "conflict" in message -> BookingActionError.Conflict
    "cancel_window_closed" in message -> BookingActionError.CancelWindowClosed
    "check_in_closed" in message -> BookingActionError.CheckInClosed
    "spot_unavailable" in message -> BookingActionError.SpotUnavailable
    "invalid_period" in message -> BookingActionError.InvalidPeriod
    else -> null
}
