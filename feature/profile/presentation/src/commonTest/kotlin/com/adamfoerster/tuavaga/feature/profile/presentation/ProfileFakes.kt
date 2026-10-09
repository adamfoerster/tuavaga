package com.adamfoerster.tuavaga.feature.profile.presentation

import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingQuote
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.Counterpart
import com.adamfoerster.tuavaga.core.domain.booking.RejectReason
import com.adamfoerster.tuavaga.core.domain.condo.ActiveCondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.CondoError
import com.adamfoerster.tuavaga.core.domain.condo.CondoPreview
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.Condominium
import com.adamfoerster.tuavaga.core.domain.condo.GarageLevel
import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.condo.MembershipKind
import com.adamfoerster.tuavaga.core.domain.condo.NewCondominium
import com.adamfoerster.tuavaga.core.domain.condo.ResidentInfo
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.user.User
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.domain.vehicle.NewVehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.Vehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleError
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleRepository
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import com.adamfoerster.tuavaga.feature.profile.domain.AccountError
import com.adamfoerster.tuavaga.feature.profile.domain.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDateTime

class FakeSession : SessionRepository {
    override val sessionState = MutableStateFlow<SessionState>(
        SessionState.SignedIn(User("u1", "adam@condominio.com", "Adam Foerster")),
    )
    var signOuts = 0

    override suspend fun signOut(): EmptyResult<DataError.Remote> {
        signOuts++
        return Result.Success(Unit)
    }
}

fun membership(id: String, name: String, kind: MembershipKind = MembershipKind.RESIDENT, block: String? = "B", unit: String = "142") =
    Membership(Condominium(id, name, "Rua", null, emptyList(), "AV-4K7Q"), block, unit, kind)

class FakeCondos(vararg memberships: Membership) : CondoRepository {
    override val memberships = MutableStateFlow(memberships.toList())
    var leaveError: CondoError? = null
    val left = mutableListOf<String>()
    override suspend fun refreshMemberships(): EmptyResult<DataError.Remote> = Result.Success(Unit)
    override suspend fun search(query: String): Result<List<CondoPreview>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun findByInviteCode(code: String): Result<CondoPreview?, DataError.Remote> = Result.Success(null)
    override suspend fun blocksOf(condoId: String): Result<List<String>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun garageOf(condoId: String): Result<List<GarageLevel>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun join(condoId: String, resident: ResidentInfo): EmptyResult<CondoError> = Result.Success(Unit)
    override suspend fun create(condo: NewCondominium, resident: ResidentInfo): Result<String, CondoError> = Result.Success("x")
    override suspend fun leave(condoId: String): EmptyResult<CondoError> {
        left += condoId
        leaveError?.let { return Result.Failure(it) }
        memberships.value = memberships.value.filterNot { it.condo.id == condoId }
        return Result.Success(Unit)
    }
}

class FakeActiveCondo(initial: String? = null) : ActiveCondoRepository {
    override val activeCondoId = MutableStateFlow(initial)
    override suspend fun setActiveCondo(condoId: String) {
        activeCondoId.value = condoId
    }
}

class FakeVehicles(vararg initial: Vehicle) : VehicleRepository {
    val vehicles = initial.toMutableList()
    var error: VehicleError? = null
    override suspend fun list(): Result<List<Vehicle>, DataError.Remote> = Result.Success(vehicles.toList())
    override suspend fun add(vehicle: NewVehicle): Result<Vehicle, VehicleError> {
        error?.let { return Result.Failure(it) }
        val saved = Vehicle("v${vehicles.size + 1}", vehicle.plate, vehicle.model, vehicle.color, vehicle.type)
        vehicles += saved
        return Result.Success(saved)
    }
    override suspend fun update(vehicle: Vehicle): Result<Vehicle, VehicleError> {
        error?.let { return Result.Failure(it) }
        vehicles.indexOfFirst { it.id == vehicle.id }.takeIf { it >= 0 }?.let { vehicles[it] = vehicle }
        return Result.Success(vehicle)
    }
    override suspend fun remove(vehicleId: String): EmptyResult<VehicleError> {
        error?.let { return Result.Failure(it) }
        vehicles.removeAll { it.id == vehicleId }
        return Result.Success(Unit)
    }
}

class FakeAccount : AccountRepository {
    var error: AccountError? = null
    var deletions = 0
    override suspend fun deleteAccount(): EmptyResult<AccountError> {
        deletions++
        return error?.let { Result.Failure(it) } ?: Result.Success(Unit)
    }
}

fun booking(id: String, status: BookingStatus, role: BookingRole = BookingRole.RENTER) = Booking(
    id = id, code = 1, role = role, status = status, condoId = "c1", condoName = "Alameda", spotId = "s1",
    levelName = "Subsolo 2", sectorName = "B", spotNumber = "27", directions = null, rules = emptyList(), cancelNoticeHours = 24,
    period = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 10, 18, 0)),
    quote = BookingQuote(BillingUnit.HOUR, 10, 800), note = null, rejectReason = null, rejectMessage = null, respondBy = null,
    cancelledByOwner = null, checkedInAt = null, checkedOutAt = null, counterpart = Counterpart(null, null, null),
    vehicle = null, conflict = null,
)

class FakeBookings(vararg initial: Booking) : BookingRepository {
    override val bookings = MutableStateFlow(initial.toList())
    override suspend fun refresh(): EmptyResult<DataError.Remote> = Result.Success(Unit)
    private val ok: EmptyResult<BookingActionError> = Result.Success(Unit)
    override suspend fun approve(bookingId: String) = ok
    override suspend fun reject(bookingId: String, reason: RejectReason, message: String?) = ok
    override suspend fun cancel(bookingId: String) = ok
    override suspend fun checkIn(bookingId: String) = ok
    override suspend fun checkOut(bookingId: String) = ok
    override suspend fun extend(bookingId: String, newEnd: LocalDateTime) = ok
}

val onix = Vehicle("v1", "ABC1D23", "Onix", "Preto", VehicleType.CAR)
