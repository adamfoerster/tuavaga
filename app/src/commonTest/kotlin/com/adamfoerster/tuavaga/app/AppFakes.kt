package com.adamfoerster.tuavaga.app

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
import com.adamfoerster.tuavaga.core.domain.prefs.AppPreferencesRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.user.User
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow

fun membership(id: String, name: String = "Condo $id") = Membership(
    condo = Condominium(id, name, "Rua $id", null, listOf("A", "B"), "AV-4K7Q"),
    block = "B",
    unit = "142",
    kind = MembershipKind.RESIDENT,
)

fun signedIn(id: String = "u1") = SessionState.SignedIn(User(id, "$id@condominio.com", "Morador"))

class FakeSession(initial: SessionState = SessionState.SignedOut) : SessionRepository {
    override val sessionState = MutableStateFlow(initial)

    override suspend fun signOut(): EmptyResult<DataError.Remote> {
        sessionState.value = SessionState.SignedOut
        return Result.Success(Unit)
    }
}

class FakePreferences(introSeen: Boolean = true) : AppPreferencesRepository {
    override val introSeen = MutableStateFlow(introSeen)
    override suspend fun markIntroSeen() {
        introSeen.value = true
    }
}

/** Membership cache + a scripted backend: [serverMemberships] is what a refresh loads. */
class FakeCondos : CondoRepository {
    override val memberships = MutableStateFlow<List<Membership>>(emptyList())
    var serverMemberships: List<Membership> = emptyList()
    var refreshError: DataError.Remote? = null
    var refreshCalls = 0

    override suspend fun refreshMemberships(): EmptyResult<DataError.Remote> {
        refreshCalls++
        refreshError?.let { return Result.Failure(it) }
        memberships.value = serverMemberships
        return Result.Success(Unit)
    }

    override suspend fun search(query: String): Result<List<CondoPreview>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun findByInviteCode(code: String): Result<CondoPreview?, DataError.Remote> = Result.Success(null)
    override suspend fun blocksOf(condoId: String): Result<List<String>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun garageOf(condoId: String): Result<List<GarageLevel>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun join(condoId: String, resident: ResidentInfo): EmptyResult<CondoError> = Result.Success(Unit)
    override suspend fun create(condo: NewCondominium, resident: ResidentInfo): Result<String, CondoError> =
        Result.Success("new")
    override suspend fun leave(condoId: String): EmptyResult<DataError.Remote> = Result.Success(Unit)
}

class FakeActiveCondo(initial: String? = null) : ActiveCondoRepository {
    override val activeCondoId = MutableStateFlow(initial)
    override suspend fun setActiveCondo(condoId: String) {
        activeCondoId.value = condoId
    }
}
