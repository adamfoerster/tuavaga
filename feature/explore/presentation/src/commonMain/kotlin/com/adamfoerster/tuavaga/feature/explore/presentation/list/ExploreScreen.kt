package com.adamfoerster.tuavaga.feature.explore.presentation.list

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbCard
import com.adamfoerster.tuavaga.core.designsystem.components.KbChip
import com.adamfoerster.tuavaga.core.designsystem.components.KbGarageSpot
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbOption
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbSpotLegend
import com.adamfoerster.tuavaga.core.designsystem.components.KbSpotState
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.designsystem.components.KbToolbar
import com.adamfoerster.tuavaga.core.designsystem.components.KbZebraStripe
import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.spot.ApprovalMode
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import com.adamfoerster.tuavaga.core.presentation.dayMonth
import com.adamfoerster.tuavaga.core.presentation.formatMoney
import com.adamfoerster.tuavaga.core.presentation.short
import com.adamfoerster.tuavaga.core.presentation.suffix
import com.adamfoerster.tuavaga.feature.explore.domain.ExploreFilters
import com.adamfoerster.tuavaga.feature.explore.domain.SpotListing
import com.adamfoerster.tuavaga.feature.explore.presentation.common.PeriodFields
import com.adamfoerster.tuavaga.feature.explore.presentation.common.PeriodSheet
import com.adamfoerster.tuavaga.feature.explore.presentation.common.headlinePrice
import com.adamfoerster.tuavaga.feature.explore.presentation.common.metaLine
import com.adamfoerster.tuavaga.feature.explore.presentation.common.ownerLine
import com.adamfoerster.tuavaga.feature.explore.presentation.common.weeklySummary
import kotlinx.datetime.LocalDateTime
import org.koin.compose.viewmodel.koinViewModel

/** Content of the Explorar tab (inside the app shell). */
@Composable
fun ExploreRoot(
    onOpenSpot: (condoId: String, spotId: String, period: BookingPeriod) -> Unit,
    onListSpot: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Back from a spot or a booking, availability may have changed.
    LaunchedEffect(Unit) { viewModel.onAction(ExploreAction.OnRetry) }
    ExploreScreen(
        state = state,
        now = viewModel.nowForSheet,
        modifier = modifier,
        onAction = { action ->
            when (action) {
                is ExploreAction.OnOpenSpot -> state.condo?.condo?.id?.let { onOpenSpot(it, action.spotId, state.period) }
                ExploreAction.OnListSpotClick -> onListSpot()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

/** Boards 04 (list) and 06 (garage map). */
@Composable
fun ExploreScreen(
    state: ExploreState,
    now: LocalDateTime,
    onAction: (ExploreAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isCondoEmpty) {
                FirstSpotPanel(state, onAction)
                return@Column
            }
            PeriodFields(state.period, onClick = { onAction(ExploreAction.OnPeriodClick) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                KbToolbar(
                    options = listOf(KbOption(ExploreMode.LIST, "Lista"), KbOption(ExploreMode.MAP, "Mapa")),
                    selected = state.mode,
                    onSelect = { onAction(ExploreAction.OnModeSelect(it)) },
                    fill = false,
                )
                KbText(
                    text = "${state.visible.size} ${if (state.visible.size == 1) "LIVRE" else "LIVRES"}",
                    style = KerbTheme.typography.dataSmall,
                    color = KerbTheme.colors.inkMuted,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }
            when {
                state.isLoading && state.listings.isEmpty() ->
                    KbMeter(value = 0.4f, label = "Buscando vagas", segments = 24, redline = 1f)
                state.error != null -> ErrorPanel(state, onAction)
                state.mode == ExploreMode.LIST -> ListContent(state, onAction)
                else -> MapContent(state, onAction)
            }
        }
        if (state.isPeriodSheetOpen) {
            PeriodSheet(
                initial = state.period,
                now = now,
                onApply = { onAction(ExploreAction.OnPeriodApply(it)) },
                onDismiss = { onAction(ExploreAction.OnPeriodDismiss) },
            )
        }
    }
}

@Composable
private fun FilterChips(filters: ExploreFilters, onAction: (ExploreAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(SpotFeature.COVERED, SpotFeature.WIDE, SpotFeature.ELECTRIC, SpotFeature.NEAR_ELEVATOR, SpotFeature.MOTORCYCLE)
            .forEach { feature ->
                KbChip(feature.label, feature in filters.features, { onAction(ExploreAction.OnFeatureToggle(feature)) })
            }
        KbChip("Até R$ 10/h", filters.maxHourCents != null, { onAction(ExploreAction.OnCheapToggle) })
    }
}

@Composable
private fun ListContent(state: ExploreState, onAction: (ExploreAction) -> Unit) {
    FilterChips(state.filters, onAction)
    if (state.visible.isEmpty()) {
        KbPanel(title = "Nenhuma vaga livre", aside = "EXPLORAR") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KbText(
                    text = "Nada disponível de ${state.period.start.short()} a ${state.period.end.short()}. " +
                        if (state.filters.isEmpty) "Tente outras datas." else "Tente outras datas ou tire um filtro.",
                    style = KerbTheme.typography.body,
                    color = KerbTheme.colors.inkMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KbButton("Mudar datas", { onAction(ExploreAction.OnPeriodClick) }, Modifier.weight(1f), variant = KbButtonVariant.Ghost)
                    if (!state.filters.isEmpty) {
                        KbButton("Limpar filtros", { onAction(ExploreAction.OnClearFilters) }, Modifier.weight(1f), variant = KbButtonVariant.Ghost)
                    }
                }
            }
        }
    }
    state.visible.forEach { spot -> SpotCard(spot, onOpen = { onAction(ExploreAction.OnOpenSpot(spot.id)) }) }
}

internal fun SpotListing.title(): String =
    "Vaga $code${if (SpotFeature.MOTORCYCLE in features) " · Moto" else ""}".uppercase()

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpotTags(spot: SpotListing, availableLabel: String = "Livre") {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            !spot.available -> KbTag("Indisponível", tone = KbTone.Danger)
            spot.approval == ApprovalMode.MANUAL -> KbTag("Requer aprovação", tone = KbTone.Caution)
            else -> KbTag(availableLabel, tone = KbTone.Go)
        }
        SpotFeature.entries.filter { it in spot.features }.forEachIndexed { index, feature ->
            KbTag(feature.label, tone = if (index == 0) KbTone.Info else KbTone.Neutral)
        }
    }
}

@Composable
internal fun PriceText(spot: SpotListing) {
    val (cents, unit) = spot.headlinePrice() ?: return
    Text(
        text = buildAnnotatedString {
            append(formatMoney(cents))
            withStyle(SpanStyle(color = KerbTheme.colors.inkMuted, fontSize = KerbTheme.typography.dataSmall.fontSize)) {
                append(unit.suffix())
            }
        },
        style = KerbTheme.typography.data,
        color = KerbTheme.colors.ink,
    )
}

@Composable
private fun SpotCard(spot: SpotListing, onOpen: () -> Unit) {
    KbCard(onClick = onOpen) {
        KbText(spot.title(), KerbTheme.typography.md.copy(fontWeight = FontWeight.ExtraBold))
        KbText(spot.metaLine(), KerbTheme.typography.dataSmall, color = KerbTheme.colors.inkMuted)
        SpotTags(spot)
        PriceText(spot)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KbText(
                text = listOfNotNull(spot.ownerLine(), weeklySummary(spot.weekly)).joinToString("\n"),
                style = KerbTheme.typography.bodySmall,
                color = KerbTheme.colors.inkMuted,
                modifier = Modifier.weight(1f),
            )
            KbButton("Ver vaga", onOpen, variant = KbButtonVariant.Ghost)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MapContent(state: ExploreState, onAction: (ExploreAction) -> Unit) {
    if (state.levels.size > 1) {
        KbToolbar(
            options = state.levels.map { KbOption(it.id, it.name) },
            selected = state.mapLevelId ?: return,
            onSelect = { onAction(ExploreAction.OnLevelSelect(it)) },
        )
    }
    val spots = state.mapSpots
    val levelName = state.levels.firstOrNull { it.id == state.mapLevelId }?.name.orEmpty()
    val free = spots.count { it.isBookable }
    KbPanel(title = levelName, aside = "$free ${if (free == 1) "LIVRE" else "LIVRES"} · ${state.period.start.date.dayMonth()}") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                spots.forEach { spot ->
                    val spotState = when {
                        spot.id == state.selectedSpotId -> KbSpotState.Selected
                        spot.isMine -> KbSpotState.Mine
                        spot.available -> KbSpotState.Free
                        else -> KbSpotState.Busy
                    }
                    KbGarageSpot(
                        label = "${spot.sectorName.orEmpty()}${spot.number}",
                        state = spotState,
                        onClick = { onAction(ExploreAction.OnMapSpotClick(spot.id)) },
                        description = "Vaga ${spot.code}, " + when (spotState) {
                            KbSpotState.Free -> "livre"
                            KbSpotState.Busy -> "ocupada"
                            KbSpotState.Selected -> "selecionada"
                            KbSpotState.Mine -> "sua vaga"
                        },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KbSpotLegend(KbSpotState.Free, "Livre")
                KbSpotLegend(KbSpotState.Busy, "Ocupada")
                KbSpotLegend(KbSpotState.Selected, "Selecionada")
                KbSpotLegend(KbSpotState.Mine, "Sua vaga")
            }
        }
    }
    state.selectedSpot?.let { spot ->
        KbCard(selected = true) {
            KbText(spot.title(), KerbTheme.typography.md.copy(fontWeight = FontWeight.ExtraBold))
            KbText(
                text = listOfNotNull(spot.description, spot.sizeLabel?.let { "$it m" }).joinToString(" · ").uppercase(),
                style = KerbTheme.typography.dataSmall,
                color = KerbTheme.colors.inkMuted,
            )
            SpotTags(spot)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { PriceText(spot) }
                if (!spot.isMine) KbButton("Ver vaga", { onAction(ExploreAction.OnOpenSpot(spot.id)) })
            }
        }
    }
}

@Composable
private fun ErrorPanel(state: ExploreState, onAction: (ExploreAction) -> Unit) {
    KbPanel(title = "Sem conexão") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KbTag("Sem conexão", tone = KbTone.Danger)
            KbText("Não deu para carregar as vagas.", KerbTheme.typography.body, color = KerbTheme.colors.inkMuted)
            KbText(state.error?.asString().orEmpty(), KerbTheme.typography.bodySmall, color = KerbTheme.colors.inkMuted)
            KbButton("Tentar de novo", { onAction(ExploreAction.OnRetry) }, Modifier.fillMaxWidth())
        }
    }
}

/** Edge state "Condomínio sem vagas" (board 17). */
@Composable
private fun FirstSpotPanel(state: ExploreState, onAction: (ExploreAction) -> Unit) {
    val typography = KerbTheme.typography
    val colors = KerbTheme.colors
    KbPanel(title = "Condomínio sem vagas", aside = "EXPLORAR · NOVO") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KbZebraStripe(Modifier.height(12.dp))
            KbText("Seja o primeiro", typography.md)
            KbText(
                text = "Ninguém anunciou vaga no ${state.condo?.condo?.name.orEmpty()} ainda. Anuncie a sua e convide os vizinhos.",
                style = typography.body,
                color = colors.inkMuted,
            )
            Text(
                text = buildAnnotatedString {
                    append("CÓDIGO DE CONVITE · ")
                    withStyle(SpanStyle(color = colors.telemetry)) { append(state.condo?.condo?.inviteCode.orEmpty()) }
                },
                style = typography.data,
                color = colors.ink,
            )
            KbButton("Anunciar minha vaga", { onAction(ExploreAction.OnListSpotClick) }, Modifier.fillMaxWidth())
        }
    }
}
