package com.adamfoerster.tuavaga.feature.explore.presentation

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
import com.adamfoerster.tuavaga.core.domain.spot.ApprovalMode
import com.adamfoerster.tuavaga.core.domain.spot.Availability
import com.adamfoerster.tuavaga.core.domain.spot.Prices
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.domain.vehicle.NewVehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.Vehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleError
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleRepository
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import com.adamfoerster.tuavaga.feature.explore.domain.BookingConfirmation
import com.adamfoerster.tuavaga.feature.explore.domain.BookingError
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.domain.BookingRequest
import com.adamfoerster.tuavaga.feature.explore.domain.BookingStatus
import com.adamfoerster.tuavaga.feature.explore.domain.ExploreRepository
import com.adamfoerster.tuavaga.feature.explore.domain.SpotListing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDateTime

/** Thursday 08/10/2026 14:37, Brasília. */
val NOW = LocalDateTime(2026, 10, 8, 14, 37)

fun membership(id: String, name: String = "Condo $id") = Membership(
    condo = Condominium(id, name, "Rua $id", null, emptyList(), "AV-4K7Q"),
    block = "B",
    unit = "142",
    kind = MembershipKind.RESIDENT,
)

fun listing(
    id: String,
    levelId: String = "s2",
    levelName: String = "Subsolo 2",
    levelPosition: Int = 2,
    features: Set<SpotFeature> = emptySet(),
    prices: Prices = Prices(hourCents = 800, dayCents = 3500),
    approval: ApprovalMode = ApprovalMode.MANUAL,
    available: Boolean = true,
    isMine: Boolean = false,
    rules: List<String> = listOf("Sem caminhonete", "Respeitar horário"),
    minPeriodMinutes: Int = 120,
) = SpotListing(
    id = id, levelId = levelId, levelName = levelName, levelPosition = levelPosition, sectorName = "B", number = id.takeLast(2),
    sizeLabel = "2,5 × 5,0", description = "Perto do elevador", features = features, heightCm = 210, directions = null,
    prices = prices, minPeriodMinutes = minPeriodMinutes, cancelNoticeHours = 24, approval = approval, rules = rules,
    ownerName = "Marina Ribeiro", ownerBlock = "A", ownerUnit = "31", isMine = isMine, available = available, weekly = emptyMap(),
)

class FakeCondos(vararg memberships: Membership) : CondoRepository {
    override val memberships = MutableStateFlow(memberships.toList())
    override suspend fun refreshMemberships(): EmptyResult<DataError.Remote> = Result.Success(Unit)
    override suspend fun search(query: String): Result<List<CondoPreview>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun findByInviteCode(code: String): Result<CondoPreview?, DataError.Remote> = Result.Success(null)
    override suspend fun blocksOf(condoId: String): Result<List<String>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun garageOf(condoId: String): Result<List<GarageLevel>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun join(condoId: String, resident: ResidentInfo): EmptyResult<CondoError> = Result.Success(Unit)
    override suspend fun create(condo: NewCondominium, resident: ResidentInfo): Result<String, CondoError> = Result.Success("x")
    override suspend fun leave(condoId: String): EmptyResult<DataError.Remote> = Result.Success(Unit)
}

class FakeActiveCondo(initial: String? = null) : ActiveCondoRepository {
    override val activeCondoId = MutableStateFlow(initial)
    override suspend fun setActiveCondo(condoId: String) {
        activeCondoId.value = condoId
    }
}

class FakeVehicles(vararg vehicles: Vehicle) : VehicleRepository {
    private val list = vehicles.toList()
    override suspend fun list(): Result<List<Vehicle>, DataError.Remote> = Result.Success(list)
    override suspend fun add(vehicle: NewVehicle): Result<Vehicle, VehicleError> = Result.Failure(VehicleError.DuplicatePlate)
}

val onix = Vehicle("v1", "ABC1D23", "Onix", "Preto", VehicleType.CAR)

/** Listings per condominium; [availableIn] decides availability per period when set. */
class FakeExplore : ExploreRepository {
    val listings = mutableMapOf<String, List<SpotListing>>()
    var availableIn: ((BookingPeriod) -> Boolean)? = null
    var searchError: DataError.Remote? = null
    val searches = mutableListOf<Pair<String, BookingPeriod>>()
    val requests = mutableListOf<BookingRequest>()
    var requestResult: Result<BookingConfirmation, BookingError> =
        Result.Success(BookingConfirmation("b1", 1001, BookingStatus.PENDING, 7000))
    var busy: List<BookingPeriod> = emptyList()

    override suspend fun search(condoId: String, period: BookingPeriod): Result<List<SpotListing>, DataError.Remote> {
        searches += condoId to period
        searchError?.let { return Result.Failure(it) }
        val rows = listings[condoId].orEmpty()
        return Result.Success(availableIn?.let { rule -> rows.map { it.copy(available = rule(period)) } } ?: rows)
    }

    var spotAvailability = Availability()
    val busyRanges = mutableListOf<BookingPeriod>()

    override suspend fun availability(spotId: String): Result<Availability, DataError.Remote> = Result.Success(spotAvailability)

    override suspend fun busyPeriods(spotId: String, range: BookingPeriod): Result<List<BookingPeriod>, DataError.Remote> {
        busyRanges += range
        return Result.Success(busy)
    }

    override suspend fun requestBooking(request: BookingRequest): Result<BookingConfirmation, BookingError> {
        requests += request
        return requestResult
    }
}
