package com.adamfoerster.tuavaga.feature.hosting.presentation.myspots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbCard
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbOption
import com.adamfoerster.tuavaga.core.designsystem.components.KbReadout
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.designsystem.components.KbToolbar
import com.adamfoerster.tuavaga.core.domain.spot.SpotFormats
import com.adamfoerster.tuavaga.feature.hosting.domain.Spot
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotStatus
import org.koin.compose.viewmodel.koinViewModel

/** Content of the "Minhas vagas" tab (inside the app shell). */
@Composable
fun MySpotsRoot(
    onCreateSpot: (condoId: String?) -> Unit,
    onEditSpot: (spotId: String) -> Unit,
    onAgenda: (spotId: String) -> Unit,
    onRequests: () -> Unit,
    onWantSpot: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MySpotsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Reloads every time the tab is shown again (e.g. back from the wizard).
    LaunchedEffect(Unit) { viewModel.onAction(MySpotsAction.OnRefresh) }
    MySpotsScreen(
        state = state,
        modifier = modifier,
        onAction = { action ->
            when (action) {
                is MySpotsAction.OnCreateSpot -> onCreateSpot(action.condoId)
                is MySpotsAction.OnEditSpot -> onEditSpot(action.spotId)
                is MySpotsAction.OnAgendaClick -> onAgenda(action.spotId)
                MySpotsAction.OnRequestsClick -> onRequests()
                MySpotsAction.OnWantSpotClick -> onWantSpot()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

private enum class UseMode { WANT, HAVE }

/** Board 12 · Minhas vagas. */
@Composable
fun MySpotsScreen(
    state: MySpotsState,
    onAction: (MySpotsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KbToolbar(
            options = listOf(KbOption(UseMode.WANT, "Quero uma vaga"), KbOption(UseMode.HAVE, "Tenho uma vaga")),
            selected = UseMode.HAVE,
            onSelect = { if (it == UseMode.WANT) onAction(MySpotsAction.OnWantSpotClick) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KbReadout(label = "Ganhos do mês", value = reais(state.monthEarningsCents), unit = "R$", modifier = Modifier.weight(1f))
            KbReadout(label = "Reservas", value = "${state.monthBookingCount}", unit = "no mês", modifier = Modifier.weight(1f))
        }
        if (state.pendingCount > 0) {
            KbButton(
                text = if (state.pendingCount == 1) "1 solicitação aguardando" else "${state.pendingCount} solicitações aguardando",
                onClick = { onAction(MySpotsAction.OnRequestsClick) },
                variant = KbButtonVariant.Volt,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        KbButton(
            text = "Cadastrar vaga",
            onClick = { onAction(MySpotsAction.OnCreateSpot(null)) },
            size = KbButtonSize.Large,
            modifier = Modifier.fillMaxWidth(),
        )
        KbErrorText(state.error?.asString())
        if (state.error != null && state.groups.isEmpty()) {
            KbButton(
                text = "Tentar de novo",
                onClick = { onAction(MySpotsAction.OnRefresh) },
                variant = KbButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.isLoading && state.groups.isEmpty()) {
            KbMeter(value = 0.4f, label = "Carregando suas vagas", segments = 24, redline = 1f)
        }
        state.groups.forEach { group -> CondoGroup(group, state, onAction) }
    }
}

@Composable
private fun CondoGroup(group: CondoSpots, state: MySpotsState, onAction: (MySpotsAction) -> Unit) {
    val colors = KerbTheme.colors
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
        KbText(group.condoName, KerbTheme.typography.sm, modifier = Modifier.weight(1f))
        KbText(
            text = if (group.spots.size == 1) "1 VAGA" else "${group.spots.size} VAGAS",
            style = KerbTheme.typography.dataSmall,
            color = colors.inkMuted,
        )
    }
    if (group.spots.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceRaised).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KbText("Nenhuma vaga sua neste condomínio.", KerbTheme.typography.body, color = colors.inkMuted)
            KbButton(
                text = "Anunciar neste condomínio",
                onClick = { onAction(MySpotsAction.OnCreateSpot(group.condoId)) },
                variant = KbButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    group.spots.forEach { spot ->
        SpotCard(spot, isUpdating = spot.id == state.updatingSpotId, pending = state.pendingFor(spot.id), onAction = onAction)
    }
}

/** "SUBSOLO 2 · SETOR B · R$ 8/H" */
internal fun Spot.metaLine(): String = listOfNotNull(
    levelName,
    sectorName?.let { "Setor $it" },
    SpotFormats.shortPrice(prices),
).joinToString(" · ").uppercase()

@Composable
private fun SpotCard(spot: Spot, isUpdating: Boolean, pending: Int, onAction: (MySpotsAction) -> Unit) {
    val active = spot.status == SpotStatus.ACTIVE
    KbCard(onClick = { onAction(MySpotsAction.OnEditSpot(spot.id)) }) {
        KbText(
            text = "Vaga ${spot.code}".uppercase(),
            style = KerbTheme.typography.md.copy(fontWeight = FontWeight.ExtraBold),
        )
        KbText(spot.metaLine(), KerbTheme.typography.dataSmall, color = KerbTheme.colors.inkMuted)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KbTag(if (active) "Ativa" else "Pausada", tone = if (active) KbTone.Go else KbTone.Caution)
            if (pending > 0) KbTag(if (pending == 1) "1 pedido" else "$pending pedidos", tone = KbTone.Caution)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KbButton(
                text = "Agenda",
                onClick = { onAction(MySpotsAction.OnAgendaClick(spot.id)) },
                variant = KbButtonVariant.Ghost,
                modifier = Modifier.weight(1f),
            )
            KbButton(
                text = if (active) "Pausar" else "Reativar",
                onClick = { onAction(MySpotsAction.OnToggleStatus(spot.id)) },
                variant = KbButtonVariant.Ghost,
                isLoading = isUpdating,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** "70" or "70,50" for the earnings readout (the unit "R$" is shown apart). */
internal fun reais(cents: Int): String =
    if (cents % 100 == 0) "${cents / 100}" else "${cents / 100},${(cents % 100).toString().padStart(2, '0')}"
