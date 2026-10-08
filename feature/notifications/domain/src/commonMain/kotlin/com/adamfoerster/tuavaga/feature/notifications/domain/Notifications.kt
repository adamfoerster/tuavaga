package com.adamfoerster.tuavaga.feature.notifications.domain

import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

/** Mirror of the database's `notification_kind`. */
enum class NotificationKind(val key: String) {
    REQUEST("request"),
    BOOKED("booked"),
    APPROVED("approved"),
    REJECTED("rejected"),
    CANCELLED("cancelled"),
    EXPIRED("expired"),
    REMINDER("reminder"),
    LATE("late"),
    ;

    companion object {
        fun fromKey(key: String): NotificationKind? = entries.firstOrNull { it.key == key }
    }
}

/** A notification of board 26 (named so it does not clash with platform types). */
data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val condoId: String,
    val bookingId: String?,
    val title: String,
    val body: String,
    val at: LocalDateTime,
    val isRead: Boolean,
)

fun List<AppNotification>.unreadIn(condoId: String? = null): Int =
    count { !it.isRead && (condoId == null || it.condoId == condoId) }

interface NotificationsRepository {
    /** The user's notifications, newest first, kept current by Realtime. */
    val notifications: Flow<Result<List<AppNotification>, DataError.Remote>>

    /** "Marcar como lidas": all, or only those of [condoId]. */
    suspend fun markRead(condoId: String?): EmptyResult<DataError.Remote>
}
