package com.adamfoerster.tuavaga.feature.notifications.presentation

import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.feature.notifications.domain.AppNotification
import com.adamfoerster.tuavaga.feature.notifications.domain.unreadIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** A day of the list: "Hoje", "Ontem" or the date. */
data class NotificationGroup(val day: LocalDate, val items: List<AppNotification>)

data class NotificationsState(
    val today: LocalDate,
    val notifications: List<AppNotification> = emptyList(),
    val memberships: List<Membership> = emptyList(),
    /** `null` = Todos. */
    val condoFilter: String? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val isMarking: Boolean = false,
) {
    val visible: List<AppNotification>
        get() = notifications.filter { condoFilter == null || it.condoId == condoFilter }

    val unread: Int get() = notifications.unreadIn(condoFilter)

    /** Newest day first (board 26 "Hoje" / "Ontem"). */
    val groups: List<NotificationGroup>
        get() = visible.groupBy { it.at.date }.entries.sortedByDescending { it.key }.map { (day, items) ->
            NotificationGroup(day, items.sortedByDescending { it.at })
        }

    fun condoName(condoId: String): String? = memberships.firstOrNull { it.condo.id == condoId }?.condo?.name

    fun dayLabel(day: LocalDate): String? = when (day) {
        today -> "Hoje"
        today.minus(1, DateTimeUnit.DAY) -> "Ontem"
        else -> null
    }
}

sealed interface NotificationsAction {
    data class OnFilterSelect(val condoId: String?) : NotificationsAction
    data object OnMarkAllRead : NotificationsAction

    // Navigation, handled by the Root.
    data object OnBackClick : NotificationsAction
    data class OnNotificationClick(val notification: AppNotification) : NotificationsAction
}
