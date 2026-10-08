package com.adamfoerster.tuavaga.feature.bookings.presentation.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbBottomSheet
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbCheckRow
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbIconButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbIcons
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbOption
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbPhotoPlaceholder
import com.adamfoerster.tuavaga.core.designsystem.components.KbReadout
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbSelect
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.canCheckIn
import com.adamfoerster.tuavaga.core.domain.booking.minutesBetween
import com.adamfoerster.tuavaga.core.domain.booking.minutesLate
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import com.adamfoerster.tuavaga.core.presentation.counterpartShortName
import com.adamfoerster.tuavaga.core.presentation.formatMoney
import com.adamfoerster.tuavaga.core.presentation.full
import com.adamfoerster.tuavaga.core.presentation.hhmm
import com.adamfoerster.tuavaga.core.presentation.unitsLabel
import kotlinx.coroutines.delay

@Composable
fun CheckRoot(
    viewModel: CheckViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            CheckEvent.Done -> onBack()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            viewModel.onAction(CheckAction.OnTick)
        }
    }
    CheckScreen(
        state = state,
        onAction = { action ->
            if (action == CheckAction.OnBackClick) onBack()
            viewModel.onAction(action)
        },
    )
}

/** The three confirmations of each board. */
// Names may end in an abbreviation ("Marina R."), so a sentence never ends on one.
private fun confirmations(kind: CheckKind, booking: Booking): List<String> = when (kind) {
    CheckKind.IN -> listOf(
        "Estou na vaga ${booking.spotCode}, no ${booking.levelName.lowercase()}.",
        booking.vehicle?.let { v ->
            "Meu veículo é o ${listOfNotNull(v.model, v.color?.lowercase()).joinToString(" ")}, placa ${v.plate}."
        } ?: "Meu veículo cabe na vaga.",
        if (booking.rules.isEmpty()) {
            "Combinei com ${booking.counterpartShortName} como usar a vaga."
        } else {
            "Li as ${booking.rules.size} regras de ${booking.counterpartShortName} para esta vaga."
        },
    )
    CheckKind.OUT -> listOf(
        "Tirei meu veículo e meus pertences.",
        "A vaga está limpa e livre.",
        "Não deixei nenhum dano na vaga.",
    )
}

/** Board 24 · Check-in and board 25 · Check-out. */
@Composable
fun CheckScreen(
    state: CheckState,
    onAction: (CheckAction) -> Unit,
) {
    val booking = state.booking
    val typography = KerbTheme.typography
    val muted = KerbTheme.colors.inkMuted
    Box(modifier = Modifier.fillMaxSize()) {
        KbScreen(
            header = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KbIconButton(KbIcons.Back, "Voltar", { onAction(CheckAction.OnBackClick) })
                }
            },
            bottomBar = if (booking == null) null else ({
                KbErrorText(state.error?.asString())
                KbButton(
                    text = if (state.kind == CheckKind.IN) "Confirmar check-in" else "Confirmar check-out",
                    onClick = { onAction(CheckAction.OnConfirmClick) },
                    size = KbButtonSize.Large,
                    enabled = state.canConfirm && (state.kind == CheckKind.OUT || booking.canCheckIn(state.now)),
                    isLoading = state.isWorking && !state.isExtendOpen,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.kind == CheckKind.OUT) {
                    KbButton(
                        text = "Preciso de mais tempo",
                        onClick = { onAction(CheckAction.OnExtendClick) },
                        variant = KbButtonVariant.Ghost,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }),
        ) {
            if (booking == null) {
                KbMeter(value = 0.4f, label = "Carregando a reserva", segments = 24, redline = 1f)
                return@KbScreen
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.kind == CheckKind.OUT) KbTag("Em andamento", tone = KbTone.Go)
                KbTag(booking.condoName, tone = KbTone.Info)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KbText(if (state.kind == CheckKind.IN) "Check-in" else "Check-out", typography.lg)
                KbText(
                    "Reserva ${booking.code} · Vaga ${booking.spotCode} · ${booking.levelName}".uppercase(),
                    typography.dataSmall,
                    color = muted,
                )
            }
            if (state.kind == CheckKind.OUT) {
                val start = booking.checkedInAt ?: booking.period.start
                val total = minutesBetween(start, booking.period.end).coerceAtLeast(1)
                KbMeter(
                    value = (minutesBetween(start, state.now).toFloat() / total).coerceIn(0f, 1f),
                    label = "Tempo na vaga",
                    segments = 24,
                    redline = 0.9f,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.kind == CheckKind.IN) {
                    KbReadout(label = "Entrada", value = state.now.hhmm(), tone = KbTone.Go, modifier = Modifier.weight(1f))
                    KbReadout(label = "Saída combinada", value = booking.period.end.hhmm(), modifier = Modifier.weight(1f))
                } else {
                    val late = booking.minutesLate(state.now) > 0
                    KbReadout(label = "Saída combinada", value = booking.period.end.hhmm(), modifier = Modifier.weight(1f))
                    KbReadout(
                        label = "Agora",
                        value = state.now.hhmm(),
                        tone = if (late) KbTone.Danger else KbTone.Go,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (state.kind == CheckKind.IN && !booking.canCheckIn(state.now)) {
                KbText("O check-in libera 30 min antes da entrada e vai até a saída.", typography.bodySmall, color = muted)
            }
            KbPanel(title = "Confirme") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    confirmations(state.kind, booking).forEachIndexed { index, text ->
                        KbCheckRow(
                            checked = index in state.checked,
                            onCheckedChange = { onAction(CheckAction.OnToggle(index)) },
                            text = text,
                        )
                    }
                }
            }
            KbPanel(title = if (state.kind == CheckKind.IN) "Foto da vaga ao entrar" else "Foto da vaga ao sair", aside = "OPCIONAL") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KbPhotoPlaceholder("Fotos · em breve", Modifier.fillMaxWidth().height(96.dp))
                    if (state.kind == CheckKind.IN) {
                        KbText("A foto protege os dois lados: mostra como a vaga estava quando você chegou.", typography.bodySmall, color = muted)
                    }
                }
            }
        }
        if (state.isExtendOpen && booking != null) ExtendSheet(state, booking, onAction)
    }
}

/** Board 18 "Ajustar horário": a later exit, recalculated in the same billing unit. */
@Composable
private fun ExtendSheet(state: CheckState, booking: Booking, onAction: (CheckAction) -> Unit) {
    val typography = KerbTheme.typography
    KbBottomSheet(onDismiss = { onAction(CheckAction.OnExtendDismiss) }) {
        KbText("Mais tempo", typography.lg)
        KbText(
            "Se a vaga estiver livre, a saída muda na hora e ${booking.counterpartShortName} vê na agenda.",
            typography.body,
            color = KerbTheme.colors.inkMuted,
        )
        KbSelect(
            label = "Saída",
            options = state.extendOptions.map { KbOption(it, it.full()) },
            selected = state.extendTo,
            onSelect = { onAction(CheckAction.OnExtendSelect(it)) },
        )
        state.extendTo?.let(state::quoteFor)?.let { quote ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                KbText("Novo valor · ${quote.unitsLabel()}", typography.body, color = KerbTheme.colors.inkMuted, modifier = Modifier.weight(1f))
                KbText(formatMoney(quote.totalCents), typography.md)
            }
        }
        KbErrorText(state.error?.asString())
        KbButton(
            text = "Ajustar horário",
            onClick = { onAction(CheckAction.OnExtendConfirm) },
            isLoading = state.isWorking,
            enabled = state.extendTo != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
