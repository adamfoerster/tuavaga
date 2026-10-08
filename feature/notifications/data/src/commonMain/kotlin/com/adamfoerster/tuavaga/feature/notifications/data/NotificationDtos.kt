package com.adamfoerster.tuavaga.feature.notifications.data

import com.adamfoerster.tuavaga.core.data.util.fromDbTimestamp
import com.adamfoerster.tuavaga.feature.notifications.domain.AppNotification
import com.adamfoerster.tuavaga.feature.notifications.domain.NotificationKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row of `notifications` (supabase/migrations/20261012000000_messages_notifications.sql). */
@Serializable
internal data class NotificationDto(
    val id: String,
    val kind: String,
    @SerialName("condo_id") val condoId: String,
    @SerialName("booking_id") val bookingId: String? = null,
    val title: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("read_at") val readAt: String? = null,
)

/** Unknown kinds (a newer database) are left out instead of breaking the list. */
internal fun NotificationDto.toNotification(): AppNotification? = NotificationKind.fromKey(kind)?.let { k ->
    AppNotification(
        id = id,
        kind = k,
        condoId = condoId,
        bookingId = bookingId,
        title = title,
        body = body,
        at = fromDbTimestamp(createdAt),
        isRead = readAt != null,
    )
}
