package com.adamfoerster.tuavaga.feature.bookings.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbAvatar
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbIconButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbIcons
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbReadout
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.designsystem.components.initialsOf
import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.canAnswer
import com.adamfoerster.tuavaga.core.domain.booking.canCancel
import com.adamfoerster.tuavaga.core.domain.booking.canCheckIn
import com.adamfoerster.tuavaga.core.domain.booking.cancelDeadline
import com.adamfoerster.tuavaga.core.domain.booking.checkInOpensAt
import com.adamfoerster.tuavaga.core.domain.booking.isPastCancelDeadline
import com.adamfoerster.tuavaga.core.domain.booking.minutesBetween
import com.adamfoerster.tuavaga.core.domain.booking.minutesLate
import com.adamfoerster.tuavaga.core.presentation.RejectForm
import com.adamfoerster.tuavaga.core.presentation.breakdown
import com.adamfoerster.tuavaga.core.presentation.counterpartPlace
import com.adamfoerster.tuavaga.core.presentation.counterpartShortName
import com.adamfoerster.tuavaga.core.presentation.dayMonth
import com.adamfoerster.tuavaga.core.presentation.formatMoney
import com.adamfoerster.tuavaga.core.presentation.full
import com.adamfoerster.tuavaga.core.presentation.hhmm
import com.adamfoerster.tuavaga.core.presentation.line
import com.adamfoerster.tuavaga.core.presentation.statusTag
import com.adamfoerster.tuavaga.core.presentation.weekdayDayMonth
import com.adamfoerster.tuavaga.feature.bookings.presentation.common.durationShort
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime

@Composable
fun BookingDetailRoot(
    viewModel: BookingDetailViewModel,
    onBack: () -> Unit,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onExplore: () -> Unit,
    onMessage: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            viewModel.onAction(BookingDetailAction.OnTick)
        }
    }
    BookingDetailScreen(
        state = state,
        onAction = { action ->
            when (action) {
                BookingDetailAction.OnBackClick -> onBack()
                BookingDetailAction.OnCheckInClick -> onCheckIn()
                BookingDetailAction.OnCheckOutClick -> onCheckOut()
                BookingDetailAction.OnExploreClick -> onExplore()
                BookingDetailAction.OnMessageClick -> onMessage()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

/** Board 10 · Detalhe da reserva, with the edge states of board 18. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingDetailScreen(
    state: BookingDetailState,
    onAction: (BookingDetailAction) -> Unit,
) {
    val booking = state.booking
    KbScreen(
        header = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KbIconButton(KbIcons.Back, "Voltar", { onAction(BookingDetailAction.OnBackClick) })
            }
        },
    ) {
        when {
            booking == null && state.isLoading -> KbMeter(value = 0.4f, label = "Carregando a reserva", segments = 24, redline = 1f)
            booking == null -> {
                KbErrorText(state.error?.asString() ?: "Esta reserva não está mais disponível.")
                if (state.error != null) {
                    KbButton("Tentar de novo", { onAction(BookingDetailAction.OnRetry) }, Modifier.fillMaxWidth())
                }
            }
            else -> {
                val typography = KerbTheme.typography
                val (status, tone) = booking.statusTag()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KbTag(status, tone = tone)
                    KbTag(booking.condoName, tone = KbTone.Info)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    KbText("Reserva ${booking.code}", typography.lg)
                    KbText(
                        text = listOfNotNull("Vaga ${booking.spotCode}", booking.levelName, booking.sectorName?.let { "Setor $it" })
                            .joinToString(" · ").uppercase(),
                        style = typography.dataSmall,
                        color = KerbTheme.colors.inkMuted,
                    )
                }
                StatusPanel(booking, state, onAction)
                KbErrorText(state.error?.asString())
                if (booking.role == BookingRole.RENTER && booking.directions != null && booking.status.isActive) {
                    KbPanel(title = "Como chegar") {
                        KbText(booking.directions!!, typography.body)
                    }
                }
                CounterpartPanel(booking, onMessage = { onAction(BookingDetailAction.OnMessageClick) })
                ValuePanel(booking)
                CancelSection(booking, state, onAction)
            }
        }
    }
}

@Composable
private fun StatusPanel(booking: Booking, state: BookingDetailState, onAction: (BookingDetailAction) -> Unit) {
    val typography = KerbTheme.typography
    val muted = KerbTheme.colors.inkMuted
    val now = state.now
    val who = booking.counterpartShortName
    val mine = booking.role == BookingRole.RENTER
    when (booking.status) {
        BookingStatus.PENDING -> if (booking.canAnswer) {
            OwnerAnswer(booking, state, onAction)
        } else {
            KbPanel(title = "Aguardando aprovação") {
                KbText(
                    text = "$who responde até ${booking.respondBy?.full() ?: "a entrada"}.",
                    style = typography.body,
                )
            }
        }
        BookingStatus.CONFIRMED -> {
            PeriodReadouts(booking.period.start, booking.period.end, startLabel = "Entrada", endLabel = "Saída")
            if (mine) {
                KbText(
                    "Ao chegar, toque em “Fazer check-in” para registrar a entrada. Ao sair, faça o check-out no mesmo lugar.",
                    typography.body,
                    color = muted,
                )
                val canCheckIn = booking.canCheckIn(now)
                KbButton(
                    text = "Fazer check-in",
                    onClick = { onAction(BookingDetailAction.OnCheckInClick) },
                    size = KbButtonSize.Large,
                    enabled = canCheckIn,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (canCheckIn) {
                    KbText("Foto da vaga ao entrar é opcional.", typography.bodySmall, color = muted)
                } else {
                    val left = minutesBetween(now, booking.period.start).coerceAtLeast(0)
                    KbMeter(
                        // Fills along the last day before the entrance.
                        value = (1f - left / (24f * 60f)).coerceIn(0f, 1f),
                        label = "Começa em",
                        readout = durationShort(left),
                        segments = 24,
                        redline = 1f,
                    )
                    KbText(
                        "O check-in libera ${booking.checkInOpensAt.date.weekdayDayMonth()} às ${booking.checkInOpensAt.hhmm()}.",
                        typography.bodySmall,
                        color = muted,
                    )
                }
            } else {
                KbText("$who usa a vaga nesse período. O check-in aparece aqui quando acontecer.", typography.body, color = muted)
            }
        }
        BookingStatus.IN_PROGRESS -> {
            PeriodReadouts(booking.checkedInAt ?: booking.period.start, booking.period.end, startLabel = "Check-in", endLabel = "Saída combinada")
            val late = booking.minutesLate(now)
            if (late > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    KbReadout(label = "Passou do horário", value = "${late / 60}:${(late % 60).toString().padStart(2, '0')}", tone = KbTone.Danger, modifier = Modifier.weight(1f))
                    KbTag("Atraso", tone = KbTone.Caution)
                }
                KbText(
                    if (mine) "Libere a vaga e faça o check-out, ou peça mais tempo." else "$who passou ${durationShort(late)} do horário. Fale com o vizinho.",
                    typography.body,
                    color = muted,
                )
            }
            if (mine) {
                KbButton(
                    text = "Fazer check-out",
                    onClick = { onAction(BookingDetailAction.OnCheckOutClick) },
                    size = KbButtonSize.Large,
                    variant = if (late > 0) KbButtonVariant.Volt else KbButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        BookingStatus.REJECTED -> EndedPanel(
            text = buildString {
                append(if (mine) "$who recusou" else "Você recusou")
                booking.rejectReason?.let { append(": ${it.label.lowercase()}") }
                append(".")
                booking.rejectMessage?.let { append(" “$it”") }
            },
            showExplore = mine,
            onAction = onAction,
        )
        BookingStatus.CANCELLED -> EndedPanel(
            text = when {
                booking.cancelledByOwner == true && mine -> "$who cancelou a vaga ${booking.spotCode}. Sua reserva de ${booking.period.start.date.weekdayDayMonth()} não vale mais."
                booking.cancelledByOwner == true -> "Você cancelou esta reserva."
                mine -> "Você cancelou esta reserva."
                else -> "$who cancelou esta reserva."
            },
            showExplore = mine,
            onAction = onAction,
        )
        BookingStatus.EXPIRED -> EndedPanel(
            text = if (mine) "$who não respondeu a tempo. O pedido não vale mais." else "O pedido venceu sem resposta.",
            showExplore = mine,
            onAction = onAction,
        )
        BookingStatus.COMPLETED -> {
            PeriodReadouts(
                booking.checkedInAt ?: booking.period.start,
                booking.checkedOutAt ?: booking.period.end,
                startLabel = if (booking.checkedInAt != null) "Check-in" else "Entrada",
                endLabel = if (booking.checkedOutAt != null) "Check-out" else "Saída",
            )
            KbText("Reserva concluída.", typography.body, color = muted)
        }
    }
}

@Composable
private fun PeriodReadouts(start: LocalDateTime, end: LocalDateTime, startLabel: String, endLabel: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KbReadout(label = startLabel, value = start.hhmm(), delta = start.date.weekdayDayMonth().uppercase(), modifier = Modifier.weight(1f))
        KbReadout(label = endLabel, value = end.hhmm(), delta = end.date.weekdayDayMonth().uppercase(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EndedPanel(text: String, showExplore: Boolean, onAction: (BookingDetailAction) -> Unit) {
    KbPanel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KbText(text, KerbTheme.typography.body)
            if (showExplore) {
                KbButton("Buscar outra vaga", { onAction(BookingDetailAction.OnExploreClick) }, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Board 17 card, for one request opened from the agenda or the notification. */
@Composable
private fun OwnerAnswer(booking: Booking, state: BookingDetailState, onAction: (BookingDetailAction) -> Unit) {
    val typography = KerbTheme.typography
    KbPanel(title = "Solicitação", aside = booking.respondBy?.let { "RESPONDA ATÉ ${it.hhmm()}" }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PeriodReadouts(booking.period.start, booking.period.end, startLabel = "Entrada", endLabel = "Saída")
            booking.vehicle?.let { KbText(it.line(), typography.body) }
            booking.note?.let { KbText("“$it”", typography.body, color = KerbTheme.colors.inkMuted) }
            booking.conflict?.let { conflict ->
                KbTag("Conflito de horário", tone = KbTone.Danger)
                KbText(
                    "Já existe reserva confirmada em ${conflict.start.date.dayMonth()}, das ${conflict.start.hhmm()} às ${conflict.end.hhmm()}.",
                    typography.bodySmall,
                    color = KerbTheme.colors.inkMuted,
                )
            }
            val draft = state.rejecting
            if (draft != null) {
                RejectForm(
                    draft = draft,
                    isWorking = state.isWorking,
                    onReason = { onAction(BookingDetailAction.OnRejectReason(it)) },
                    onMessage = { onAction(BookingDetailAction.OnRejectMessage(it)) },
                    onBack = { onAction(BookingDetailAction.OnRejectDismiss) },
                    onConfirm = { onAction(BookingDetailAction.OnRejectConfirm) },
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KbButton(
                        "Recusar",
                        { onAction(BookingDetailAction.OnRejectClick) },
                        Modifier.weight(1f),
                        variant = KbButtonVariant.Ghost,
                        enabled = !state.isWorking,
                    )
                    KbButton(
                        "Aceitar",
                        { onAction(BookingDetailAction.OnApproveClick) },
                        Modifier.weight(1f),
                        variant = KbButtonVariant.Volt,
                        isLoading = state.isWorking,
                    )
                }
            }
        }
    }
}

@Composable
private fun CounterpartPanel(booking: Booking, onMessage: () -> Unit) {
    val typography = KerbTheme.typography
    KbPanel(title = if (booking.role == BookingRole.RENTER) "Locador" else "Locatário") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                KbAvatar(initialsOf(booking.counterpart.name ?: booking.counterpartShortName))
                Column(modifier = Modifier.weight(1f)) {
                    KbText(booking.counterpartShortName, typography.md)
                    if (booking.counterpartPlace.isNotEmpty()) {
                        KbText(booking.counterpartPlace, typography.dataSmall, color = KerbTheme.colors.inkMuted)
                    }
                }
                KbButton("Mensagem", onMessage, variant = KbButtonVariant.Ghost, size = KbButtonSize.Small)
            }
            if (booking.role == BookingRole.OWNER) booking.vehicle?.let { KbText(it.line(), typography.body) }
        }
    }
}

@Composable
private fun ValuePanel(booking: Booking) {
    KbPanel(title = "Valor", aside = "ACERTO DIRETO") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KbText(booking.quote.breakdown(), KerbTheme.typography.body, color = KerbTheme.colors.inkMuted, modifier = Modifier.weight(1f))
            KbText(formatMoney(booking.totalCents), KerbTheme.typography.md)
        }
    }
}

@Composable
private fun CancelSection(booking: Booking, state: BookingDetailState, onAction: (BookingDetailAction) -> Unit) {
    val typography = KerbTheme.typography
    val muted = KerbTheme.colors.inkMuted
    val now = state.now
    val mine = booking.role == BookingRole.RENTER
    if (mine && booking.status == BookingStatus.CONFIRMED) {
        val deadline = booking.cancelDeadline
        KbText(
            text = if (booking.isPastCancelDeadline(now)) {
                "O cancelamento sem aviso terminou em ${deadline.date.dayMonth()} às ${deadline.hhmm()}. Agora, ${booking.counterpartShortName} precisa concordar com o cancelamento."
            } else {
                "Cancelamento sem aviso até ${deadline.date.dayMonth()} às ${deadline.hhmm()} (${booking.cancelNoticeHours} h antes da entrada). " +
                    "Depois disso, ${booking.counterpartShortName} precisa concordar."
            },
            style = typography.bodySmall,
            color = muted,
        )
    }
    if (!booking.canCancel(now)) return
    if (state.isConfirmingCancel) {
        KbPanel(title = if (booking.status == BookingStatus.PENDING) "Cancelar o pedido?" else "Cancelar a reserva?") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KbText(
                    if (mine) "${booking.counterpartShortName} fica sabendo e a vaga volta a ficar livre." else "${booking.counterpartShortName} perde a reserva. Use só se for mesmo preciso.",
                    typography.body,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KbButton("Voltar", { onAction(BookingDetailAction.OnCancelDismiss) }, Modifier.weight(1f), variant = KbButtonVariant.Ghost)
                    KbButton("Cancelar", { onAction(BookingDetailAction.OnCancelConfirm) }, Modifier.weight(1f), isLoading = state.isWorking)
                }
            }
        }
    } else {
        KbButton(
            text = if (booking.status == BookingStatus.PENDING) "Cancelar pedido" else "Cancelar reserva",
            onClick = { onAction(BookingDetailAction.OnCancelClick) },
            variant = KbButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
