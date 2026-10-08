package com.adamfoerster.tuavaga.feature.messages.domain

import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

/** One conversation per booking (aba Mensagens). */
data class Conversation(
    val bookingId: String,
    val code: Long,
    val role: BookingRole,
    val status: BookingStatus,
    val condoId: String,
    val condoName: String,
    /** "B2-27". */
    val spotLabel: String,
    val period: BookingPeriod,
    val counterpartName: String?,
    val last: LastMessage?,
    /** Text messages from the other party not read yet. */
    val unread: Int,
)

data class LastMessage(val body: String, val isSystem: Boolean, val isMine: Boolean, val at: LocalDateTime)

/** A message of the booking chat (board 23). System messages ("CHECK-IN · 08:02") have no sender. */
data class ChatMessage(
    val id: String,
    val body: String,
    val isSystem: Boolean,
    val isMine: Boolean,
    val at: LocalDateTime,
)

/** Quick replies of board 23. */
val QUICK_REPLIES = listOf("Cheguei", "Pode liberar a vaga?", "Saindo agora", "Preciso de mais 30 min")

const val MESSAGE_MAX = 1000

interface MessagesRepository {
    /**
     * The user's conversations, kept current by Realtime (a new message anywhere reloads the list).
     * Emits a failure when a load fails; the flow keeps going.
     */
    val conversations: Flow<Result<List<Conversation>, DataError.Remote>>

    /** Messages of one booking, oldest first, kept current by Realtime. */
    fun messages(bookingId: String): Flow<Result<List<ChatMessage>, DataError.Remote>>

    suspend fun send(bookingId: String, body: String): EmptyResult<DataError.Remote>

    /** Marks the other party's messages of the booking as read. */
    suspend fun markRead(bookingId: String): EmptyResult<DataError.Remote>
}
