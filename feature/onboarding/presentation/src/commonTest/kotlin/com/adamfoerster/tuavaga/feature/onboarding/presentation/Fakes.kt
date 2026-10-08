package com.adamfoerster.tuavaga.feature.onboarding.presentation

import com.adamfoerster.tuavaga.core.domain.condo.ActiveCondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.CondoError
import com.adamfoerster.tuavaga.core.domain.condo.CondoPreview
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.condo.NewCondominium
import com.adamfoerster.tuavaga.core.domain.condo.ResidentInfo
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.user.User
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.domain.vehicle.NewVehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.Vehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleError
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow

fun preview(
    id: String = "c1",
    name: String = "Residencial Alameda Verde",
    isMember: Boolean = false,
    blocks: List<String>? = null,
) = CondoPreview(
    id = id,
    name = name,
    address = "Rua das Figueiras, 410",
    blocksCount = blocks?.size ?: 3,
    listedSpots = 12,
    isMember = isMember,
    blocks = blocks,
)

class FakeCondoRepository : CondoRepository {
    override val memberships = MutableStateFlow<List<Membership>>(emptyList())

    val searches = mutableListOf<String>()
    var searchResult: Result<List<CondoPreview>, DataError.Remote> = Result.Success(listOf(preview()))
    val inviteLookups = mutableListOf<String>()
    var inviteResult: Result<CondoPreview?, DataError.Remote> = Result.Success(null)
    var blocksResult: Result<List<String>, DataError.Remote> = Result.Success(listOf("A", "B", "C"))
    val joins = mutableListOf<Pair<String, ResidentInfo>>()
    var joinResult: EmptyResult<CondoError> = Result.Success(Unit)
    val creates = mutableListOf<Pair<NewCondominium, ResidentInfo>>()
    var createResult: Result<String, CondoError> = Result.Success("new-condo")
    var refreshCalls = 0

    override suspend fun refreshMemberships(): EmptyResult<DataError.Remote> {
        refreshCalls++
        return Result.Success(Unit)
    }

    override suspend fun search(query: String): Result<List<CondoPreview>, DataError.Remote> {
        searches += query
        return searchResult
    }

    override suspend fun findByInviteCode(code: String): Result<CondoPreview?, DataError.Remote> {
        inviteLookups += code
        return inviteResult
    }

    override suspend fun blocksOf(condoId: String) = blocksResult

    override suspend fun join(condoId: String, resident: ResidentInfo): EmptyResult<CondoError> {
        joins += condoId to resident
        return joinResult
    }

    override suspend fun create(condo: NewCondominium, resident: ResidentInfo): Result<String, CondoError> {
        creates += condo to resident
        return createResult
    }

    override suspend fun leave(condoId: String): EmptyResult<DataError.Remote> = Result.Success(Unit)
}

class FakeActiveCondoRepository : ActiveCondoRepository {
    override val activeCondoId = MutableStateFlow<String?>(null)

    override suspend fun setActiveCondo(condoId: String) {
        activeCondoId.value = condoId
    }
}

class FakeVehicleRepository(existing: List<Vehicle> = emptyList()) : VehicleRepository {
    val vehicles = existing.toMutableList()
    val added = mutableListOf<NewVehicle>()
    var failNextWith: VehicleError? = null

    override suspend fun list(): Result<List<Vehicle>, DataError.Remote> = Result.Success(vehicles.toList())

    override suspend fun add(vehicle: NewVehicle): Result<Vehicle, VehicleError> {
        failNextWith?.let {
            failNextWith = null
            return Result.Failure(it)
        }
        added += vehicle
        val saved = Vehicle("v${added.size}", vehicle.plate, vehicle.model, vehicle.color, vehicle.type)
        vehicles += saved
        return Result.Success(saved)
    }
}

class FakeSessionRepository(fullName: String? = "Adam Foerster") : SessionRepository {
    override val sessionState = MutableStateFlow<SessionState>(
        SessionState.SignedIn(User(id = "u1", email = "adam@condominio.com", fullName = fullName)),
    )

    override suspend fun signOut(): EmptyResult<DataError.Remote> {
        sessionState.value = SessionState.SignedOut
        return Result.Success(Unit)
    }
}
