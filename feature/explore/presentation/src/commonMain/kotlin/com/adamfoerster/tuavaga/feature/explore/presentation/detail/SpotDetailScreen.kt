package com.adamfoerster.tuavaga.feature.explore.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbAvatar
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbDayLegend
import com.adamfoerster.tuavaga.core.designsystem.components.KbDayState
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbHairline
import com.adamfoerster.tuavaga.core.designsystem.components.KbIconButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbIcons
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbMonthCalendar
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbPhotoPlaceholder
import com.adamfoerster.tuavaga.core.designsystem.components.KbReadout
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.designsystem.components.initialsOf
import com.adamfoerster.tuavaga.core.domain.spot.ApprovalMode
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import com.adamfoerster.tuavaga.core.domain.spot.SpotFormats
import com.adamfoerster.tuavaga.core.presentation.firstOfMonth
import com.adamfoerster.tuavaga.core.presentation.leadingBlanks
import com.adamfoerster.tuavaga.core.presentation.monthShort
import com.adamfoerster.tuavaga.core.presentation.monthTitle
import com.adamfoerster.tuavaga.feature.explore.domain.SpotListing
import com.adamfoerster.tuavaga.feature.explore.domain.shortName
import com.adamfoerster.tuavaga.feature.explore.presentation.common.formatMoney
import com.adamfoerster.tuavaga.feature.explore.presentation.common.rangeLabel
import com.adamfoerster.tuavaga.feature.explore.presentation.common.unitsLabel
import com.adamfoerster.tuavaga.feature.explore.presentation.common.vehicleKind
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SpotDetailRoot(
    viewModel: SpotDetailViewModel,
    onBack: () -> Unit,
    onRequest: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SpotDetailScreen(
        state = state,
        onAction = { action ->
            when (action) {
                SpotDetailAction.OnBackClick -> onBack()
                SpotDetailAction.OnRequestClick -> onRequest()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

/** Board 07 · Detalhe da vaga. */
@Composable
fun SpotDetailScreen(
    state: SpotDetailState,
    onAction: (SpotDetailAction) -> Unit,
) {
    val spot = state.spot
    KbScreen(
        // Short while photos are not supported, so the scrolling content keeps most of the screen.
        header = {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                KbPhotoPlaceholder("Foto · como chegar · em breve", Modifier.fillMaxWidth().height(120.dp))
                KbIconButton(
                    icon = KbIcons.Back,
                    contentDescription = "Voltar",
                    onClick = { onAction(SpotDetailAction.OnBackClick) },
                    modifier = Modifier.padding(16.dp),
                )
            }
        },
        bottomBar = if (spot != null) ({ BookingFooter(state, spot, onAction) }) else null,
    ) {
        when {
            state.isLoading && spot == null -> KbMeter(value = 0.4f, label = "Carregando a vaga", segments = 24, redline = 1f)
            spot == null -> {
                KbErrorText(state.error?.asString())
                KbButton("Tentar de novo", { onAction(SpotDetailAction.OnRetry) }, Modifier.fillMaxWidth())
            }
            else -> SpotDetailContent(state, spot, onAction)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpotDetailContent(state: SpotDetailState, spot: SpotListing, onAction: (SpotDetailAction) -> Unit) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (spot.available) KbTag("Livre no período", tone = KbTone.Go) else KbTag("Indisponível no período", tone = KbTone.Danger)
        SpotFeature.entries.filter { it in spot.features }.forEachIndexed { index, feature ->
            KbTag(feature.label, tone = if (index == 0) KbTone.Info else KbTone.Neutral)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        KbText("Vaga ${spot.code}", typography.lg)
        KbText(
            text = listOfNotNull(state.condoName, spot.levelName, spot.sectorName?.let { "Setor $it" }).joinToString(" · ").uppercase(),
            style = typography.dataSmall,
            color = colors.inkMuted,
        )
    }
    val ownerName = shortName(spot.ownerName)
    if (ownerName != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KbAvatar(initialsOf(spot.ownerName.orEmpty()))
            Column {
                KbText(ownerName, typography.sm)
                KbText(
                    text = listOfNotNull(spot.ownerBlock?.let { "Bloco $it" }, spot.ownerUnit?.let { "Unidade $it" })
                        .joinToString(" · ").uppercase(),
                    style = typography.dataSmall,
                    color = colors.inkMuted,
                )
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(BillingUnit.HOUR to ("Hora" to "R$/h"), BillingUnit.DAY to ("Dia" to "R$/dia")).forEach { (unit, labels) ->
            spot.prices.of(unit)?.let { cents ->
                KbReadout(label = labels.first, value = SpotFormats.formatPriceInput(cents), unit = labels.second, modifier = Modifier.weight(1f))
            }
        }
    }
    KbText(
        text = listOfNotNull(
            spot.prices.weekCents?.let { "Semana ${SpotFormats.formatPrice(it)}" },
            "Pagamento: acerto direto entre moradores",
        ).joinToString(" · ").uppercase(),
        style = typography.dataSmall,
        color = colors.inkMuted,
    )

    val about = listOfNotNull(
        spot.sizeLabel?.let { "Dimensões" to "$it m" },
        spot.heightCm?.let { "Pé-direito" to "${SpotFormats.formatHeight(it)} m" },
        spot.description?.let { "Descrição" to it },
        spot.directions?.let { "Como chegar" to it },
    )
    if (about.isNotEmpty()) {
        KbPanel(title = "Sobre a vaga", aside = "TIPO · ${spot.vehicleKind().uppercase()}") { InfoRows(about) }
    }
    KbPanel(title = "Condições da reserva", aside = "DEFINIDAS PELO LOCADOR") {
        InfoRows(
            listOf(
                "Período mínimo" to SpotFormats.formatMinPeriod(spot.minPeriodMinutes).replace(" h", " horas").replace("1 horas", "1 hora"),
                "Cancelamento sem aviso" to "Até ${spot.cancelNoticeHours} h antes da entrada. Depois, combine com o locador.",
            ),
        )
    }
    if (spot.rules.isNotEmpty()) {
        KbPanel(title = "Regras do locador", aside = if (spot.rules.size == 1) "1 REGRA" else "${spot.rules.size} REGRAS") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                spot.rules.forEachIndexed { index, rule ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KbText((index + 1).toString().padStart(2, '0'), typography.dataSmall, color = colors.apexText, modifier = Modifier.width(24.dp))
                        KbText(rule, typography.body, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
    val firstMonth = state.today.firstOfMonth()
    KbMonthCalendar(
        title = state.month.monthTitle(),
        leadingBlanks = state.month.leadingBlanks(),
        cells = detailCalendarCells(state.month, state.today, state.availability, state.busy, state.period),
        onDayClick = {},
        previousLabel = if (state.month > firstMonth) state.month.plus(-1, DateTimeUnit.MONTH).monthShort() else null,
        nextLabel = state.month.plus(1, DateTimeUnit.MONTH).monthShort(),
        onPrevious = { onAction(SpotDetailAction.OnPreviousMonth) },
        onNext = { onAction(SpotDetailAction.OnNextMonth) },
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KbDayLegend(KbDayState.Selected, "SEL · Seu período")
            KbDayLegend(KbDayState.Booked, "RES · Reservada")
            KbDayLegend(KbDayState.Blocked, "BLQ · Bloqueada")
        }
    }
}

@Composable
private fun InfoRows(rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) KbHairline()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KbText(label, KerbTheme.typography.label, color = KerbTheme.colors.inkMuted)
                KbText(value, KerbTheme.typography.body)
            }
        }
    }
}

@Composable
private fun BookingFooter(state: SpotDetailState, spot: SpotListing, onAction: (SpotDetailAction) -> Unit) {
    val typography = KerbTheme.typography
    KbHairline()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            KbText(
                text = listOfNotNull(state.period.rangeLabel(), state.quote?.unitsLabel()).joinToString(" · "),
                style = typography.label,
                color = KerbTheme.colors.inkMuted,
            )
            state.quote?.let { KbText(formatMoney(it.totalCents), typography.md) }
        }
        if (spot.approval == ApprovalMode.MANUAL) KbTag("Requer aprovação", tone = KbTone.Caution)
    }
    when {
        spot.isMine -> KbText("Esta vaga é sua.", typography.bodySmall, color = KerbTheme.colors.inkMuted)
        else -> KbButton(
            text = if (spot.available) "Solicitar reserva" else "Escolher outro período",
            onClick = { onAction(SpotDetailAction.OnRequestClick) },
            size = KbButtonSize.Large,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
