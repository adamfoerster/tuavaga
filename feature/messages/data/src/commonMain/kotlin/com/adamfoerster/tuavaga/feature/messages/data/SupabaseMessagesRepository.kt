package com.adamfoerster.tuavaga.feature.messages.data

import com.adamfoerster.tuavaga.core.data.condo.signedInUserId
import com.adamfoerster.tuavaga.core.data.realtime.RealtimeChanges
import com.adamfoerster.tuavaga.core.data.realtime.liveQuery
import com.adamfoerster.tuavaga.core.data.util.remoteCall
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.messages.domain.ChatMessage
import com.adamfoerster.tuavaga.feature.messages.domain.Conversation
import com.adamfoerster.tuavaga.feature.messages.domain.MessagesRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalCoroutinesApi::class)
internal class SupabaseMessagesRepository(
    private val postgrest: Postgrest,
    private val realtime: RealtimeChanges,
    sessionRepository: SessionRepository,
    scope: CoroutineScope,
) : MessagesRepository {

    private val userId: Flow<String?> = sessionRepository.sessionState.signedInUserId()

    // Shared by the Mensagens tab and its badge; Realtime only hears messages the user can read (RLS).
    override val conversations: Flow<Result<List<Conversation>, DataError.Remote>> = userId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(Result.Success(emptyList()))
            } else {
                realtime.liveQuery("messages") {
                    remoteCall { postgrest.rpc("my_conversations").decodeList<ConversationDto>().map { it.toConversation() } }
                }
            }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    override fun messages(bookingId: String): Flow<Result<List<ChatMessage>, DataError.Remote>> = userId.flatMapLatest { id ->
        if (id == null) {
            flowOf(Result.Failure(DataError.Remote.UNAUTHORIZED))
        } else {
            realtime.liveQuery("messages", "booking_id", bookingId) {
                remoteCall {
                    postgrest.from("messages")
                        .select {
                            filter { eq("booking_id", bookingId) }
                            order("created_at", Order.ASCENDING)
                        }
                        .decodeList<MessageDto>()
                        .map { it.toMessage(id) }
                }
            }
        }
    }

    override suspend fun send(bookingId: String, body: String): EmptyResult<DataError.Remote> {
        val uid = userId.first() ?: return Result.Failure(DataError.Remote.UNAUTHORIZED)
        return remoteCall {
            postgrest.from("messages").insert(
                buildJsonObject {
                    put("booking_id", bookingId)
                    put("sender_id", uid)
                    put("body", body.trim())
                },
            )
            Unit
        }
    }

    override suspend fun markRead(bookingId: String): EmptyResult<DataError.Remote> = remoteCall {
        postgrest.rpc("mark_messages_read", buildJsonObject { put("p_booking", bookingId) })
        Unit
    }
}
