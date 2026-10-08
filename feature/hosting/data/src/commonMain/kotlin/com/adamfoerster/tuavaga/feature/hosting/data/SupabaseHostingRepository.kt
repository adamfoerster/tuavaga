package com.adamfoerster.tuavaga.feature.hosting.data

import com.adamfoerster.tuavaga.core.data.util.remoteCall
import com.adamfoerster.tuavaga.core.data.util.toRemoteError
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.hosting.domain.HostingRepository
import com.adamfoerster.tuavaga.feature.hosting.domain.Spot
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotDraft
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotError
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class SupabaseHostingRepository(
    private val postgrest: Postgrest,
    private val sessionRepository: SessionRepository,
) : HostingRepository {

    override suspend fun mySpots(): Result<List<Spot>, DataError.Remote> {
        val uid = (sessionRepository.sessionState.first { it != SessionState.Loading } as? SessionState.SignedIn)?.user?.id
            ?: return Result.Failure(DataError.Remote.UNAUTHORIZED)
        return remoteCall {
            postgrest.from("spots")
                .select(Columns.raw(SPOT_COLUMNS)) {
                    filter { eq("owner_id", uid) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<SpotDto>()
                .map { it.toSpot() }
        }
    }

    override suspend fun spot(id: String): Result<Spot, DataError.Remote> = remoteCall {
        postgrest.from("spots")
            .select(Columns.raw(SPOT_COLUMNS)) { filter { eq("id", id) } }
            .decodeSingle<SpotDto>()
            .toSpot()
    }

    override suspend fun save(draft: SpotDraft): Result<String, SpotError> = try {
        val data = postgrest.rpc("save_spot", draft.toSaveParams()).data
        Result.Success(Json.decodeFromString<String>(data))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        // 23505 = unique_violation on (level, sector, number).
        if (e is PostgrestRestException && e.code == "23505") {
            Result.Failure(SpotError.DuplicateNumber)
        } else {
            Result.Failure(SpotError.Remote(e.toRemoteError()))
        }
    }

    override suspend fun setStatus(spotId: String, status: SpotStatus): EmptyResult<DataError.Remote> = remoteCall {
        postgrest.rpc(
            "set_spot_status",
            buildJsonObject {
                put("p_spot", spotId)
                put("p_status", status.toDb())
            },
        )
        Unit
    }
}
