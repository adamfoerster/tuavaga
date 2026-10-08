package com.adamfoerster.tuavaga.feature.hosting.presentation

import com.adamfoerster.tuavaga.core.domain.condo.CondoError
import com.adamfoerster.tuavaga.core.domain.condo.CondoPreview
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.Condominium
import com.adamfoerster.tuavaga.core.domain.condo.GarageLevel
import com.adamfoerster.tuavaga.core.domain.condo.GarageSector
import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.condo.MembershipKind
import com.adamfoerster.tuavaga.core.domain.condo.NewCondominium
import com.adamfoerster.tuavaga.core.domain.condo.ResidentInfo
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.hosting.domain.ApprovalMode
import com.adamfoerster.tuavaga.feature.hosting.domain.Availability
import com.adamfoerster.tuavaga.feature.hosting.domain.HostingRepository
import com.adamfoerster.tuavaga.feature.hosting.domain.Prices
import com.adamfoerster.tuavaga.feature.hosting.domain.Spot
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotDraft
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotError
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotStatus
import kotlinx.coroutines.flow.MutableStateFlow

fun membership(id: String, name: String) = Membership(
    condo = Condominium(id, name, "Rua $id", null, emptyList(), "AV-4K7Q"),
    block = null,
    unit = "1",
    kind = MembershipKind.RESIDENT,
)

/** Alameda Verde: Subsolo 1 (A, B), Subsolo 2 (A, B, C), Térreo (no sectors). */
val alamedaGarage = listOf(
    GarageLevel("s1", "Subsolo 1", listOf(GarageSector("s1a", "A"), GarageSector("s1b", "B"))),
    GarageLevel("s2", "Subsolo 2", listOf(GarageSector("s2a", "A"), GarageSector("s2b", "B"), GarageSector("s2c", "C"))),
    GarageLevel("t", "Térreo", emptyList()),
)

fun spot(
    id: String = "spot1",
    condoId: String = "c1",
    status: SpotStatus = SpotStatus.ACTIVE,
    availability: Availability = Availability(),
) = Spot(
    id = id, condoId = condoId, levelId = "s2", levelName = "Subsolo 2", sectorId = "s2b", sectorName = "B",
    number = "14", sizeLabel = "2,5 × 5,0", description = "Perto do elevador", prices = Prices(hourCents = 800, dayCents = 3500),
    minPeriodMinutes = 120, cancelNoticeHours = 24, approval = ApprovalMode.MANUAL,
    rules = listOf("Sem caminhonete", "Avisar quando chegar"), status = status, availability = availability,
)

class FakeCondos(vararg memberships: Membership) : CondoRepository {
    override val memberships = MutableStateFlow(memberships.toList())
    val garageCalls = mutableListOf<String>()
    var garage: Result<List<GarageLevel>, DataError.Remote> = Result.Success(alamedaGarage)

    override suspend fun garageOf(condoId: String): Result<List<GarageLevel>, DataError.Remote> {
        garageCalls += condoId
        return garage
    }

    override suspend fun refreshMemberships(): EmptyResult<DataError.Remote> = Result.Success(Unit)
    override suspend fun search(query: String): Result<List<CondoPreview>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun findByInviteCode(code: String): Result<CondoPreview?, DataError.Remote> = Result.Success(null)
    override suspend fun blocksOf(condoId: String): Result<List<String>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun join(condoId: String, resident: ResidentInfo): EmptyResult<CondoError> = Result.Success(Unit)
    override suspend fun create(condo: NewCondominium, resident: ResidentInfo): Result<String, CondoError> = Result.Success("x")
    override suspend fun leave(condoId: String): EmptyResult<DataError.Remote> = Result.Success(Unit)
}

class FakeHosting(spots: List<Spot> = emptyList()) : HostingRepository {
    val spots = spots.toMutableList()
    val saved = mutableListOf<SpotDraft>()
    var saveResult: Result<String, SpotError>? = null
    var listError: DataError.Remote? = null
    val statusChanges = mutableListOf<Pair<String, SpotStatus>>()

    override suspend fun mySpots(): Result<List<Spot>, DataError.Remote> =
        listError?.let { Result.Failure(it) } ?: Result.Success(spots.toList())

    override suspend fun spot(id: String): Result<Spot, DataError.Remote> =
        spots.firstOrNull { it.id == id }?.let { Result.Success(it) } ?: Result.Failure(DataError.Remote.UNKNOWN)

    override suspend fun save(draft: SpotDraft): Result<String, SpotError> {
        saved += draft
        return saveResult ?: Result.Success(draft.id ?: "new-spot")
    }

    override suspend fun setStatus(spotId: String, status: SpotStatus): EmptyResult<DataError.Remote> {
        statusChanges += spotId to status
        return Result.Success(Unit)
    }
}
