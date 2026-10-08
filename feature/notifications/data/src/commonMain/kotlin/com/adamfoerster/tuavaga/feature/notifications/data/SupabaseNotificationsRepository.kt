package com.adamfoerster.tuavaga.feature.notifications.data

import com.adamfoerster.tuavaga.core.data.condo.signedInUserId
import com.adamfoerster.tuavaga.core.data.realtime.RealtimeChanges
import com.adamfoerster.tuavaga.core.data.realtime.liveQuery
import com.adamfoerster.tuavaga.core.data.util.remoteCall
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.notifications.domain.AppNotification
import com.adamfoerster.tuavaga.feature.notifications.domain.NotificationsRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalCoroutinesApi::class)
internal class SupabaseNotificationsRepository(
    private val postgrest: Postgrest,
    realtime: RealtimeChanges,
    sessionRepository: SessionRepository,
    scope: CoroutineScope,
) : NotificationsRepository {

    // Shared by the bell badge, the condominium switcher and the screen (board 26).
    override val notifications: Flow<Result<List<AppNotification>, DataError.Remote>> =
        sessionRepository.sessionState.signedInUserId()
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf(Result.Success(emptyList()))
                } else {
                    realtime.liveQuery("notifications", "user_id", id) {
                        remoteCall {
                            postgrest.from("notifications")
                                .select {
                                    order("created_at", Order.DESCENDING)
                                    limit(LIMIT)
                                }
                                .decodeList<NotificationDto>()
                                .mapNotNull { it.toNotification() }
                        }
                    }
                }
            }
            .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    override suspend fun markRead(condoId: String?): EmptyResult<DataError.Remote> = remoteCall {
        postgrest.rpc("mark_notifications_read", buildJsonObject { put("p_condo", condoId) })
        Unit
    }

    private companion object {
        const val LIMIT = 100L
    }
}
