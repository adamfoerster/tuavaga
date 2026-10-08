package com.adamfoerster.tuavaga.feature.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbChip
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbIconButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbIcons
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbSectionHeader
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.designsystem.components.leftBar
import com.adamfoerster.tuavaga.core.presentation.hhmm
import com.adamfoerster.tuavaga.core.presentation.weekdayDayMonth
import com.adamfoerster.tuavaga.feature.notifications.domain.AppNotification
import com.adamfoerster.tuavaga.feature.notifications.domain.NotificationKind
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationsRoot(
    onBack: () -> Unit,
    onOpenBooking: (bookingId: String) -> Unit,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotificationsScreen(
        state = state,
        onAction = { action ->
            when (action) {
                NotificationsAction.OnBackClick -> onBack()
                is NotificationsAction.OnNotificationClick -> action.notification.bookingId?.let(onOpenBooking)
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

/** Tag of each kind: always a word, never only a color. */
fun NotificationKind.tag(): Pair<String, KbTone> = when (this) {
    NotificationKind.REQUEST -> "Solicitação" to KbTone.Caution
    NotificationKind.BOOKED -> "Nova reserva" to KbTone.Go
    NotificationKind.APPROVED -> "Aprovada" to KbTone.Go
    NotificationKind.REJECTED -> "Recusada" to KbTone.Danger
    NotificationKind.CANCELLED -> "Cancelada" to KbTone.Danger
    NotificationKind.EXPIRED -> "Sem resposta" to KbTone.Neutral
    NotificationKind.REMINDER -> "Lembrete" to KbTone.Info
    NotificationKind.LATE -> "Atraso" to KbTone.Caution
}

/** Board 26 · Notificações. */
@Composable
fun NotificationsScreen(
    state: NotificationsState,
    onAction: (NotificationsAction) -> Unit,
) {
    val typography = KerbTheme.typography
    KbScreen(
        header = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KbIconButton(KbIcons.Back, "Voltar", { onAction(NotificationsAction.OnBackClick) })
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    KbButton(
                        text = "Marcar como lidas",
                        onClick = { onAction(NotificationsAction.OnMarkAllRead) },
                        variant = KbButtonVariant.Ghost,
                        size = KbButtonSize.Small,
                        enabled = state.unread > 0,
                        isLoading = state.isMarking,
                    )
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KbText("Notificações", typography.lg, modifier = Modifier.weight(1f))
            if (state.unread > 0) KbTag(if (state.unread == 1) "1 nova" else "${state.unread} novas", tone = KbTone.Danger)
        }
        if (state.memberships.size > 1) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KbChip("Todos", selected = state.condoFilter == null, onClick = { onAction(NotificationsAction.OnFilterSelect(null)) })
                state.memberships.forEach { m ->
                    KbChip(m.condo.name, selected = state.condoFilter == m.condo.id, onClick = { onAction(NotificationsAction.OnFilterSelect(m.condo.id)) })
                }
            }
        }
        KbErrorText(state.error?.asString())
        when {
            state.notifications.isEmpty() && state.isLoading ->
                KbMeter(value = 0.4f, label = "Carregando notificações", segments = 24, redline = 1f)
            state.visible.isEmpty() -> KbPanel(title = "Nada por aqui") {
                KbText(
                    "Pedidos, aprovações, lembretes de check-in e atrasos aparecem aqui assim que acontecem.",
                    typography.body,
                    color = KerbTheme.colors.inkMuted,
                )
            }
            else -> state.groups.forEach { group ->
                KbSectionHeader(state.dayLabel(group.day) ?: group.day.weekdayDayMonth())
                group.items.forEach { NotificationRow(it, state.condoName(it.condoId), onAction) }
            }
        }
        KbText(
            "Cada aviso mostra o condomínio de origem.",
            typography.bodySmall,
            color = KerbTheme.colors.inkMuted,
        )
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, condoName: String?, onAction: (NotificationsAction) -> Unit) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    val (label, tone) = notification.kind.tag()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised)
            .let { if (notification.isRead) it else it.leftBar(colors.apex, 4.dp) }
            .clickable(enabled = notification.bookingId != null) { onAction(NotificationsAction.OnNotificationClick(notification)) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KbTag(label, tone = tone)
            KbText(
                condoName.orEmpty().uppercase(),
                typography.dataSmall,
                color = colors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            KbText(notification.at.hhmm(), typography.dataSmall, color = colors.inkMuted)
        }
        KbText(notification.title, typography.bodyLarge)
        KbText(notification.body, typography.body, color = colors.inkMuted)
        if (!notification.isRead) KbText("Nova", typography.label, color = colors.apexText)
    }
}
