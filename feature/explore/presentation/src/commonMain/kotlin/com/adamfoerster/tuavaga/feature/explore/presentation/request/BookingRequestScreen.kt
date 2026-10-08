package com.adamfoerster.tuavaga.feature.explore.presentation.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbCheckRow
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbField
import com.adamfoerster.tuavaga.core.designsystem.components.KbHairline
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbOption
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbSelect
import com.adamfoerster.tuavaga.core.designsystem.components.KbStepHeader
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.designsystem.components.KbToolbar
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.spot.ApprovalMode
import com.adamfoerster.tuavaga.core.domain.spot.SpotFormats
import com.adamfoerster.tuavaga.core.domain.user.shortName
import com.adamfoerster.tuavaga.core.domain.vehicle.Vehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import com.adamfoerster.tuavaga.core.presentation.breakdown
import com.adamfoerster.tuavaga.core.presentation.durationLabel
import com.adamfoerster.tuavaga.core.presentation.formatMoney
import com.adamfoerster.tuavaga.core.presentation.full
import com.adamfoerster.tuavaga.core.presentation.label
import com.adamfoerster.tuavaga.core.presentation.unitsLabel
import com.adamfoerster.tuavaga.feature.explore.domain.SpotListing
import com.adamfoerster.tuavaga.feature.explore.presentation.common.PeriodFields
import com.adamfoerster.tuavaga.feature.explore.presentation.common.PeriodSheet

@Composable
fun BookingRequestRoot(
    viewModel: BookingRequestViewModel,
    onExit: () -> Unit,
    onFinished: () -> Unit,
    onViewBooking: (bookingId: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            BookingRequestEvent.Exit -> onExit()
            BookingRequestEvent.Finished -> onFinished()
            is BookingRequestEvent.ViewBooking -> onViewBooking(event.bookingId)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // Each step starts at the top.
        key(state.step) { BookingRequestScreen(state, viewModel::onAction) }
        if (state.isPeriodSheetOpen) {
            PeriodSheet(
                initial = state.period,
                now = viewModel.nowForSheet,
                onApply = { viewModel.onAction(BookingRequestAction.OnPeriodApply(it)) },
                onDismiss = { viewModel.onAction(BookingRequestAction.OnPeriodDismiss) },
            )
        }
    }
}

@Composable
fun BookingRequestScreen(
    state: BookingRequestState,
    onAction: (BookingRequestAction) -> Unit,
) {
    val spot = state.spot
    when {
        state.step == RequestStep.DONE -> DoneStep(state, onAction)
        spot == null -> KbScreen(
            header = { KbStepHeader("Passo 1 de 2", "Período e veículo", 1, 2, onBack = { onAction(BookingRequestAction.OnBackClick) }) },
        ) {
            if (state.isLoading) {
                KbMeter(value = 0.4f, label = "Carregando", segments = 24, redline = 1f)
            } else {
                KbErrorText(state.error?.asString())
                KbButton("Tentar de novo", { onAction(BookingRequestAction.OnRetry) }, Modifier.fillMaxWidth())
            }
        }
        state.step == RequestStep.FORM -> FormStep(state, spot, onAction)
        else -> SummaryStep(state, spot, onAction)
    }
}

private fun SpotListing.placeLine(): String =
    listOfNotNull("Vaga $code", levelName, sectorName?.let { "Setor $it" }).joinToString(" · ").uppercase()

private fun Vehicle.label(): String = "$plate · $model ${color.lowercase()}"

private fun VehicleType.hint(): String = when (this) {
    VehicleType.CAR -> "CARRO"
    VehicleType.MOTORCYCLE -> "MOTO"
    VehicleType.LARGE -> "GRANDE PORTE"
}

/** Board 08 · Montar reserva. */
@Composable
private fun FormStep(state: BookingRequestState, spot: SpotListing, onAction: (BookingRequestAction) -> Unit) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    KbScreen(
        header = { KbStepHeader("Passo 1 de 2", "Período e veículo", 1, 2, onBack = { onAction(BookingRequestAction.OnBackClick) }) },
        bottomBar = {
            KbErrorText(state.error?.asString())
            Row(verticalAlignment = Alignment.CenterVertically) {
                KbText("Valor combinado", typography.label, color = colors.inkMuted, modifier = Modifier.weight(1f))
                KbText(state.quote?.let { formatMoney(it.totalCents) } ?: "—", typography.md)
            }
            KbButton(
                text = "Revisar reserva",
                onClick = { onAction(BookingRequestAction.OnReviewClick) },
                size = KbButtonSize.Large,
                isLoading = state.isChecking,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.condoName.isNotEmpty()) KbTag(state.condoName, tone = KbTone.Info)
            KbText("Montar reserva", typography.lg)
            KbText(spot.placeLine(), typography.dataSmall, color = colors.inkMuted)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KbText("Cobrança", typography.label, color = colors.inkMuted)
            val unit = state.unit
            if (unit != null) {
                KbToolbar(
                    options = spot.prices.offeredUnits.map { KbOption(it, it.label()) },
                    selected = unit,
                    onSelect = { onAction(BookingRequestAction.OnUnitSelect(it)) },
                )
            }
        }
        PeriodFields(state.period, onClick = { onAction(BookingRequestAction.OnPeriodClick) })
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KbText(
                text = listOfNotNull(state.quote?.unitsLabel(), state.period.durationLabel()).joinToString(" · ").uppercase(),
                style = typography.dataSmall,
                modifier = Modifier.weight(1f),
            )
            when {
                state.isChecking -> KbTag("Conferindo", tone = KbTone.Neutral)
                spot.available -> KbTag("Livre", tone = KbTone.Go)
                else -> KbTag("Indisponível", tone = KbTone.Danger)
            }
        }
        KbText(
            text = "Período mínimo desta vaga: ${SpotFormats.formatMinPeriod(spot.minPeriodMinutes)}. " +
                "Cancelamento sem aviso até ${spot.cancelNoticeHours} h antes da entrada.",
            style = typography.bodySmall,
            color = colors.inkMuted,
        )
        if (state.vehicles.isEmpty()) {
            KbText(
                text = "Você não tem veículo cadastrado. Dá para pedir assim mesmo e combinar com o locador.",
                style = typography.bodySmall,
                color = colors.inkMuted,
            )
        } else {
            KbSelect(
                label = "Veículo",
                options = state.vehicles.map { KbOption(it.id, it.label(), it.type.hint()) },
                selected = state.vehicleId,
                onSelect = { onAction(BookingRequestAction.OnVehicleSelect(it)) },
                error = state.vehicleError?.asString(),
            )
        }
        KbField(
            value = state.note,
            onValueChange = { onAction(BookingRequestAction.OnNoteChange(it)) },
            label = "Observações ao locador",
            placeholder = "Chego sábado cedo, saio domingo à noite.",
            singleLine = false,
        )
        if (spot.rules.isNotEmpty()) {
            val rules = if (spot.rules.size == 1) "a regra" else "as ${spot.rules.size} regras"
            val owner = shortName(spot.ownerName)?.let { "de $it" } ?: "do locador"
            KbCheckRow(
                checked = state.rulesAccepted,
                onCheckedChange = { onAction(BookingRequestAction.OnRulesChange(it)) },
                // "Li as 3 regras de Marina R. e vou cumprir o horário combinado." (board 08)
                text = "Li $rules $owner e vou cumprir o horário combinado.",
            )
            KbErrorText(state.rulesError?.asString())
        }
    }
}

/** Board 09 · Resumo. */
@Composable
private fun SummaryStep(state: BookingRequestState, spot: SpotListing, onAction: (BookingRequestAction) -> Unit) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    val quote = state.quote
    val owner = shortName(spot.ownerName)
    KbScreen(
        header = { KbStepHeader("Passo 2 de 2", "Resumo", 2, 2, onBack = { onAction(BookingRequestAction.OnBackClick) }) },
        bottomBar = {
            KbErrorText(state.error?.asString())
            KbButton(
                text = if (spot.approval == ApprovalMode.AUTO) "Reservar agora" else "Enviar solicitação",
                onClick = { onAction(BookingRequestAction.OnSendClick) },
                size = KbButtonSize.Large,
                isLoading = state.isSending,
                modifier = Modifier.fillMaxWidth(),
            )
            KbButton(
                text = "Voltar e editar",
                onClick = { onAction(BookingRequestAction.OnBackClick) },
                variant = KbButtonVariant.Ghost,
                enabled = !state.isSending,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.condoName.isNotEmpty()) KbTag(state.condoName, tone = KbTone.Info)
            KbText("Resumo", typography.lg)
        }
        KbPanel(title = "Reserva", aside = "VAGA ${spot.code}") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryRow("Entrada", state.period.start.full())
                SummaryRow("Saída", state.period.end.full())
                state.selectedVehicle?.let { SummaryRow("Veículo", "${it.plate} · ${it.model}") }
                if (owner != null) SummaryRow("Locador", listOfNotNull(owner, spot.ownerBlock?.let { "Bl. $it" }).joinToString(" · "))
            }
        }
        if (quote != null) {
            KbPanel(title = "Valor", aside = "COMBINADO") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KbText(quote.breakdown(), typography.body, color = colors.inkMuted, modifier = Modifier.weight(1f))
                        KbText(formatMoney(quote.totalCents), typography.md)
                    }
                    KbHairline()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KbText("Pagamento", typography.label, color = colors.inkMuted, modifier = Modifier.weight(1f))
                        KbTag("Em breve", tone = KbTone.Caution)
                    }
                    KbText("Acerto direto entre moradores. Nenhuma cobrança acontece no app.", typography.bodySmall, color = colors.inkMuted)
                }
            }
        }
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (spot.approval == ApprovalMode.AUTO) {
                KbTag("Imediata", tone = KbTone.Go)
                KbText("A vaga fica reservada assim que você confirmar.", typography.bodySmall, modifier = Modifier.weight(1f))
            } else {
                KbTag("Aprovação", tone = KbTone.Caution)
                KbText(
                    text = "${owner ?: "O locador"} tem até 12 h para responder. Você recebe aviso no app.",
                    style = typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        KbText(label, KerbTheme.typography.label, color = KerbTheme.colors.inkMuted, modifier = Modifier.weight(1f))
        KbText(value, KerbTheme.typography.data)
    }
}

/** Board 17 "Confirmação": pending approval or confirmed. */
@Composable
private fun DoneStep(state: BookingRequestState, onAction: (BookingRequestAction) -> Unit) {
    val confirmation = state.confirmation ?: return
    val typography = KerbTheme.typography
    val owner = shortName(state.spot?.ownerName)
    KbScreen(
        showZebra = true,
        bottomBar = {
            KbButton(
                text = "Ver reserva",
                onClick = { onAction(BookingRequestAction.OnViewBookingClick) },
                size = KbButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            )
            KbButton(
                text = "Voltar ao Explorar",
                onClick = { onAction(BookingRequestAction.OnDoneClick) },
                variant = KbButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        KbText(if (confirmation.status == BookingStatus.CONFIRMED) "Reserva confirmada" else "Pedido enviado", typography.lg)
        KbPanel(title = "Reserva ${confirmation.code}", aside = formatMoney(confirmation.totalCents)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (confirmation.status == BookingStatus.CONFIRMED) {
                    KbTag("Confirmada", tone = KbTone.Go)
                    KbText(
                        "Reserva ${confirmation.code} pronta. O check-in libera no dia da reserva.",
                        typography.body,
                    )
                } else {
                    KbTag("Aguardando aprovação", tone = KbTone.Caution)
                    KbText(
                        // The name may end in an abbreviation ("Marina R."), so it never closes the sentence.
                        "${owner ?: "O locador"} tem até 12 h para responder ao pedido.",
                        typography.body,
                    )
                }
                KbText("${state.period.start.full()} → ${state.period.end.full()}", typography.dataSmall, color = KerbTheme.colors.inkMuted)
            }
        }
        KbText(
            "A reserva fica na aba Reservas, com check-in, check-out e cancelamento.",
            typography.bodySmall,
            color = KerbTheme.colors.inkMuted,
        )
    }
}
