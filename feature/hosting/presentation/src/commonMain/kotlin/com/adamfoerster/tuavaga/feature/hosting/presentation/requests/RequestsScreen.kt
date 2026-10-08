package com.adamfoerster.tuavaga.feature.hosting.presentation.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbCard
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbIconButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbIcons
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.presentation.RejectForm
import com.adamfoerster.tuavaga.core.presentation.counterpartShortName
import com.adamfoerster.tuavaga.core.presentation.dayMonth
import com.adamfoerster.tuavaga.core.presentation.formatMoney
import com.adamfoerster.tuavaga.core.presentation.full
import com.adamfoerster.tuavaga.core.presentation.hhmm
import com.adamfoerster.tuavaga.core.presentation.line
import com.adamfoerster.tuavaga.core.presentation.spotAndPeriodLine
import com.adamfoerster.tuavaga.core.presentation.unitsLabel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RequestsRoot(
    onBack: () -> Unit,
    onOpenBooking: (bookingId: String) -> Unit,
    viewModel: RequestsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RequestsScreen(
        state = state,
        onAction = { action ->
            when (action) {
                RequestsAction.OnBackClick -> onBack()
                is RequestsAction.OnOpenClick -> onOpenBooking(action.bookingId)
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

/** Board 17 · Solicitações. */
@Composable
fun RequestsScreen(
    state: RequestsState,
    onAction: (RequestsAction) -> Unit,
) {
    val typography = KerbTheme.typography
    KbScreen(
        header = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KbIconButton(KbIcons.Back, "Voltar", { onAction(RequestsAction.OnBackClick) })
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KbText("Solicitações", typography.lg, modifier = Modifier.weight(1f))
            if (state.requests.isNotEmpty()) {
                KbTag(if (state.requests.size == 1) "1 nova" else "${state.requests.size} novas", tone = KbTone.Danger)
            }
        }
        if (state.error != null) {
            KbErrorText(state.error.asString())
            KbButton("Tentar de novo", { onAction(RequestsAction.OnRefresh) }, variant = KbButtonVariant.Ghost, modifier = Modifier.fillMaxWidth())
        }
        when {
            state.requests.isEmpty() && state.isRefreshing ->
                KbMeter(value = 0.4f, label = "Carregando solicitações", segments = 24, redline = 1f)
            state.requests.isEmpty() -> KbPanel(title = "Tudo respondido") {
                KbText(
                    "Quando um vizinho pedir uma vaga sua que precisa de aprovação, o pedido aparece aqui.",
                    typography.body,
                    color = KerbTheme.colors.inkMuted,
                )
            }
            else -> state.requests.forEach { RequestCard(it, state, onAction) }
        }
    }
}

@Composable
private fun RequestCard(booking: Booking, state: RequestsState, onAction: (RequestsAction) -> Unit) {
    val typography = KerbTheme.typography
    val muted = KerbTheme.colors.inkMuted
    val draft = state.rejecting?.takeIf { it.bookingId == booking.id }
    val working = state.workingId == booking.id
    KbCard(onClick = { onAction(RequestsAction.OnOpenClick(booking.id)) }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                draft != null -> KbTag("Recusando", tone = KbTone.Caution)
                booking.conflict != null -> KbTag("Conflito de horário", tone = KbTone.Danger)
                else -> KbTag("Aguardando você", tone = KbTone.Caution)
            }
            KbText(booking.counterpartShortName, typography.md)
            booking.vehicle?.let { KbText(it.line(), typography.body) }
            KbText(booking.spotAndPeriodLine().uppercase(), typography.dataSmall, color = muted)
            KbText("${booking.quote.unitsLabel()} · ${formatMoney(booking.totalCents)}", typography.body)
            booking.note?.let { KbText("“$it”", typography.bodySmall, color = muted) }
            booking.conflict?.let { conflict ->
                KbText(
                    "Já existe reserva confirmada em ${conflict.start.date.dayMonth()}, das ${conflict.start.hhmm()} às ${conflict.end.hhmm()}.",
                    typography.bodySmall,
                    color = muted,
                )
            }
            booking.respondBy?.let { KbText("Responda até ${it.full()}.", typography.bodySmall, color = muted) }
            state.actionError?.takeIf { it.first == booking.id }?.let { KbErrorText(it.second.asString()) }
            if (draft != null) {
                RejectForm(
                    draft = draft,
                    isWorking = working,
                    onReason = { onAction(RequestsAction.OnRejectReason(it)) },
                    onMessage = { onAction(RequestsAction.OnRejectMessage(it)) },
                    onBack = { onAction(RequestsAction.OnRejectDismiss) },
                    onConfirm = { onAction(RequestsAction.OnRejectConfirm) },
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KbButton(
                        "Recusar",
                        { onAction(RequestsAction.OnRejectClick(booking.id)) },
                        Modifier.weight(1f),
                        variant = KbButtonVariant.Ghost,
                        size = KbButtonSize.Medium,
                        enabled = state.workingId == null,
                    )
                    KbButton(
                        "Aceitar",
                        { onAction(RequestsAction.OnAcceptClick(booking.id)) },
                        Modifier.weight(1f),
                        variant = KbButtonVariant.Volt,
                        isLoading = working,
                        enabled = state.workingId == null || working,
                    )
                }
            }
        }
    }
}
