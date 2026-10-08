package com.adamfoerster.tuavaga.core.data.condo

import com.adamfoerster.tuavaga.core.data.util.remoteCall
import com.adamfoerster.tuavaga.core.data.util.toRemoteError
import com.adamfoerster.tuavaga.core.database.condo.MembershipDao
import com.adamfoerster.tuavaga.core.domain.condo.CondoError
import com.adamfoerster.tuavaga.core.domain.condo.CondoPreview
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.GarageLevel
import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.condo.NewCondominium
import com.adamfoerster.tuavaga.core.domain.condo.ResidentInfo
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal class SupabaseCondoRepository(
    private val postgrest: Postgrest,
    private val sessionRepository: SessionRepository,
    private val membershipDao: MembershipDao,
) : CondoRepository {

    private val userId: Flow<String?> = sessionRepository.sessionState.signedInUserId()

    override val memberships: Flow<List<Membership>> = observeMemberships(userId, membershipDao)

    override suspend fun refreshMemberships(): EmptyResult<DataError.Remote> {
        val uid = userId.first() ?: return Result.Failure(DataError.Remote.UNAUTHORIZED)
        return remoteCall {
            val rows = postgrest.from("memberships")
                .select(Columns.raw("block,unit,kind,condominiums(id,name,address,cep,blocks,invite_code)")) {
                    filter { eq("user_id", uid) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<MembershipDto>()
            membershipDao.replace(uid, rows.mapIndexed { index, dto -> dto.toEntity(uid, index) })
        }
    }

    override suspend fun search(query: String): Result<List<CondoPreview>, DataError.Remote> = remoteCall {
        postgrest.rpc("search_condominiums", buildJsonObject { put("p_query", query.trim()) })
            .decodeList<CondoPreviewDto>()
            .map { it.toPreview() }
    }

    override suspend fun findByInviteCode(code: String): Result<CondoPreview?, DataError.Remote> = remoteCall {
        postgrest.rpc("find_condominium_by_invite", buildJsonObject { put("p_code", code) })
            .decodeList<CondoPreviewDto>()
            .firstOrNull()
            ?.toPreview()
    }

    override suspend fun blocksOf(condoId: String): Result<List<String>, DataError.Remote> = remoteCall {
        val data = postgrest.rpc("condominium_blocks", buildJsonObject { put("p_condo", condoId) }).data
        Json.decodeFromString<List<String>?>(data).orEmpty()
    }

    override suspend fun garageOf(condoId: String): Result<List<GarageLevel>, DataError.Remote> = remoteCall {
        postgrest.from("condo_levels")
            .select(Columns.raw("id,name,position,condo_sectors(id,name,position)")) {
                filter { eq("condo_id", condoId) }
                order("position", Order.ASCENDING)
            }
            .decodeList<LevelDto>()
            .map { it.toGarageLevel() }
    }

    override suspend fun join(condoId: String, resident: ResidentInfo): EmptyResult<CondoError> = condoCall {
        postgrest.rpc(
            "join_condominium",
            buildJsonObject {
                put("p_condo", condoId)
                putResident(resident)
            },
        )
        Unit
    }

    override suspend fun create(condo: NewCondominium, resident: ResidentInfo): Result<String, CondoError> = condoCall {
        val data = postgrest.rpc(
            "create_condominium",
            buildJsonObject {
                put("p_name", condo.name.trim())
                put("p_address", condo.address.trim())
                put("p_cep", condo.cep)
                putJsonArray("p_blocks") { condo.blocks.forEach { add(it) } }
                putJsonArray("p_levels") {
                    condo.levels.forEach { level ->
                        add(
                            buildJsonObject {
                                put("name", level.name.trim())
                                putJsonArray("sectors") { level.sectors.forEach { add(it.trim()) } }
                            },
                        )
                    }
                }
                putResident(resident)
            },
        ).data
        Json.decodeFromString<String>(data)
    }

    override suspend fun leave(condoId: String): EmptyResult<DataError.Remote> {
        val uid = userId.first() ?: return Result.Failure(DataError.Remote.UNAUTHORIZED)
        val result = remoteCall {
            postgrest.from("memberships").delete {
                filter {
                    eq("user_id", uid)
                    eq("condo_id", condoId)
                }
            }
            Unit
        }
        if (result is Result.Success) refreshMemberships()
        return result
    }

    private fun JsonObjectBuilder.putResident(resident: ResidentInfo) {
        put("p_block", resident.block)
        put("p_unit", resident.unit.trim())
        put("p_kind", resident.kind.toDb())
        put("p_full_name", resident.fullName.trim())
        put("p_phone", resident.phone)
    }
}

/**
 * Id of the signed-in user, `null` when signed out. Skips [SessionState.Loading]: every new collector
 * of the session sees Loading first, and reading it as "no user" made `first()` callers get an empty
 * membership list (empty condominium dropdown) or fail with UNAUTHORIZED right after sign-in.
 */
internal fun Flow<SessionState>.signedInUserId(): Flow<String?> =
    filter { it != SessionState.Loading }
        .map { (it as? SessionState.SignedIn)?.user?.id }
        .distinctUntilChanged()

/** The cached memberships of [userId] (empty when signed out). */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeMemberships(userId: Flow<String?>, dao: MembershipDao): Flow<List<Membership>> =
    userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.observe(id).map { rows -> rows.map { it.toMembership() } }
    }

private inline fun <T> condoCall(block: () -> T): Result<T, CondoError> = try {
    Result.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    // 22023 = invalid_parameter_value, raised by join_condominium for a block outside the list.
    if (e is PostgrestRestException && e.code == "22023" && e.message?.contains("block") == true) {
        Result.Failure(CondoError.InvalidBlock)
    } else {
        Result.Failure(CondoError.Remote(e.toRemoteError()))
    }
}
