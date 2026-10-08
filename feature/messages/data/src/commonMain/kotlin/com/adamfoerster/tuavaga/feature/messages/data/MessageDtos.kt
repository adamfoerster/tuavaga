package com.adamfoerster.tuavaga.feature.messages.data

import com.adamfoerster.tuavaga.core.data.util.fromDbTimestamp
import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.feature.messages.domain.ChatMessage
import com.adamfoerster.tuavaga.feature.messages.domain.Conversation
import com.adamfoerster.tuavaga.feature.messages.domain.LastMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row of `my_conversations` (supabase/migrations/20261012000000_messages_notifications.sql). */
@Serializable
internal data class ConversationDto(
    @SerialName("booking_id") val bookingId: String,
    val code: Long,
    val role: String,
    val status: String,
    @SerialName("condo_id") val condoId: String,
    @SerialName("condo_name") val condoName: String,
    @SerialName("spot_label") val spotLabel: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    @SerialName("counterpart_name") val counterpartName: String? = null,
    @SerialName("last_body") val lastBody: String? = null,
    @SerialName("last_kind") val lastKind: String? = null,
    @SerialName("last_mine") val lastMine: Boolean? = null,
    @SerialName("last_at") val lastAt: String? = null,
    val unread: Int = 0,
)

@Serializable
internal data class MessageDto(
    val id: String,
    @SerialName("sender_id") val senderId: String? = null,
    val kind: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
)

private fun statusFromDb(value: String): BookingStatus = when (value) {
    "pending" -> BookingStatus.PENDING
    "confirmed" -> BookingStatus.CONFIRMED
    "rejected" -> BookingStatus.REJECTED
    "cancelled" -> BookingStatus.CANCELLED
    "expired" -> BookingStatus.EXPIRED
    "in_progress" -> BookingStatus.IN_PROGRESS
    else -> BookingStatus.COMPLETED
}

internal fun ConversationDto.toConversation() = Conversation(
    bookingId = bookingId,
    code = code,
    role = if (role == "owner") BookingRole.OWNER else BookingRole.RENTER,
    status = statusFromDb(status),
    condoId = condoId,
    condoName = condoName,
    spotLabel = spotLabel,
    period = BookingPeriod(fromDbTimestamp(startsAt), fromDbTimestamp(endsAt)),
    counterpartName = counterpartName,
    last = if (lastBody != null && lastAt != null) {
        LastMessage(lastBody, isSystem = lastKind == "system", isMine = lastMine == true, at = fromDbTimestamp(lastAt))
    } else {
        null
    },
    unread = unread,
)

internal fun MessageDto.toMessage(userId: String) = ChatMessage(
    id = id,
    body = body,
    isSystem = kind == "system",
    isMine = senderId == userId,
    at = fromDbTimestamp(createdAt),
)
