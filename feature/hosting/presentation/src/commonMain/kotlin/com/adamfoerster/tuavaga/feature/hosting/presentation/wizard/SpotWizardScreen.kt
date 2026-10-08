package com.adamfoerster.tuavaga.feature.hosting.presentation.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbChip
import com.adamfoerster.tuavaga.core.designsystem.components.KbDayLegend
import com.adamfoerster.tuavaga.core.designsystem.components.KbDayState
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbField
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbMonthCalendar
import com.adamfoerster.tuavaga.core.designsystem.components.KbOption
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbPhotoPlaceholder
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbSelect
import com.adamfoerster.tuavaga.core.designsystem.components.KbStepHeader
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbToolbar
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import com.adamfoerster.tuavaga.core.presentation.leadingBlanks
import com.adamfoerster.tuavaga.core.presentation.monthShort
import com.adamfoerster.tuavaga.core.presentation.monthTitle
import com.adamfoerster.tuavaga.core.domain.spot.ApprovalMode
import com.adamfoerster.tuavaga.feature.hosting.domain.CANCEL_NOTICE_OPTIONS
import com.adamfoerster.tuavaga.feature.hosting.domain.MIN_PERIOD_OPTIONS
import com.adamfoerster.tuavaga.feature.hosting.domain.PRESET_RULES
import com.adamfoerster.tuavaga.core.domain.spot.RepeatFrequency
import com.adamfoerster.tuavaga.core.domain.spot.SpotFormats
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import com.adamfoerster.tuavaga.core.domain.spot.spotCode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SpotWizardRoot(
    spotId: String?,
    condoId: String?,
    onDone: () -> Unit,
    viewModel: SpotWizardViewModel = koinViewModel(key = "spot-wizard-$spotId-$condoId") { parametersOf(spotId, condoId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SpotWizardEvent.Saved, SpotWizardEvent.Exit -> onDone()
        }
    }
    SpotWizardScreen(state = state, onAction = viewModel::onAction)
}

private val stepNames = listOf("Localização", "Preço e regras", "Disponibilidade")

@Composable
fun SpotWizardScreen(
    state: SpotWizardState,
    onAction: (SpotWizardAction) -> Unit,
) {
    // Each step starts at the top (the screen's scroll state is recreated per step).
    key(state.step) { WizardStep(state, onAction) }
}

@Composable
private fun WizardStep(
    state: SpotWizardState,
    onAction: (SpotWizardAction) -> Unit,
) {
    KbScreen(
        header = {
            KbStepHeader(
                label = "Passo ${state.step} de 3",
                readout = stepNames[state.step - 1],
                step = state.step,
                steps = 3,
                onBack = { onAction(SpotWizardAction.OnBackClick) },
            )
        },
        bottomBar = {
            KbErrorText(state.error?.asString())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KbButton(
                    text = "Voltar",
                    onClick = { onAction(SpotWizardAction.OnBackClick) },
                    variant = KbButtonVariant.Ghost,
                    size = KbButtonSize.Large,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                )
                KbButton(
                    text = when {
                        state.step < 3 -> "Continuar"
                        state.isEditing -> "Salvar vaga"
                        else -> "Publicar vaga"
                    },
                    onClick = { onAction(SpotWizardAction.OnNextClick) },
                    size = KbButtonSize.Large,
                    isLoading = state.isSaving,
                    enabled = !state.isLoading && state.loadError == null,
                    modifier = Modifier.weight(2f),
                )
            }
        },
    ) {
        when {
            state.loadError != null -> LoadError(state, onAction)
            state.step == 1 -> LocationStep(state, onAction)
            state.step == 2 -> PriceStep(state, onAction)
            else -> AvailabilityStep(state, onAction)
        }
    }
}

@Composable
private fun LoadError(state: SpotWizardState, onAction: (SpotWizardAction) -> Unit) {
    KbErrorText(state.loadError?.asString())
    KbButton(text = "Tentar de novo", onClick = { onAction(SpotWizardAction.OnRetryLoad) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun Label(text: String) {
    KbText(text, KerbTheme.typography.label, color = KerbTheme.colors.inkMuted)
}

@Composable
private fun Hint(text: String) {
    KbText(text, KerbTheme.typography.bodySmall, color = KerbTheme.colors.inkMuted)
}

// --- Step 1 · board 13 -----------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocationStep(state: SpotWizardState, onAction: (SpotWizardAction) -> Unit) {
    KbText(if (state.isEditing) "Editar vaga" else "Onde fica a vaga", KerbTheme.typography.lg)
    KbSelect(
        label = "Condomínio",
        options = state.condos.map { KbOption(it.id, it.name) },
        selected = state.condoId,
        onSelect = { onAction(SpotWizardAction.OnCondoSelect(it)) },
        enabled = !state.isEditing,
    )
    if (state.isLoading) {
        KbMeter(value = 0.4f, label = "Carregando a garagem", segments = 24, redline = 1f)
    } else {
        val level = state.selectedLevel
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KbSelect(
                label = "Subsolo / andar",
                options = state.garage.map { KbOption(it.id, it.name) },
                selected = state.levelId,
                onSelect = { onAction(SpotWizardAction.OnLevelSelect(it)) },
                error = state.levelError?.asString(),
                modifier = Modifier.weight(1f),
            )
            if (level == null || level.sectors.isNotEmpty()) {
                KbSelect(
                    label = "Setor",
                    options = level?.sectors.orEmpty().map { KbOption(it.id, it.name) },
                    selected = state.sectorId,
                    onSelect = { onAction(SpotWizardAction.OnSectorSelect(it)) },
                    error = state.sectorError?.asString(),
                    enabled = level != null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    KbField(
        value = state.number,
        onValueChange = { onAction(SpotWizardAction.OnNumberChange(it)) },
        label = "Número da vaga",
        placeholder = "14",
        hint = "Como aparece pintado no chão.",
        error = state.numberError?.asString(),
    )
    KbField(
        value = state.sizeLabel,
        onValueChange = { onAction(SpotWizardAction.OnSizeChange(it)) },
        label = "Tamanho",
        placeholder = "2,5 × 5,0",
        unit = "m",
        hint = "Largura × comprimento da vaga.",
    )
    KbField(
        value = state.description,
        onValueChange = { onAction(SpotWizardAction.OnDescriptionChange(it)) },
        label = "Descrição",
        placeholder = "Ex.: perto do elevador",
        unit = "${state.description.length}/$DESCRIPTION_MAX",
        hint = "Máximo de $DESCRIPTION_MAX caracteres.",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label("Características")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpotFeature.entries.forEach { feature ->
                KbChip(
                    text = feature.label,
                    selected = feature in state.features,
                    onClick = { onAction(SpotWizardAction.OnFeatureToggle(feature)) },
                )
            }
        }
        Hint("Aparecem como etiquetas e filtros para quem procura vaga. \"Moto\" indica vaga de moto.")
    }
    KbField(
        value = state.heightText,
        onValueChange = { onAction(SpotWizardAction.OnHeightChange(it)) },
        label = "Pé-direito",
        placeholder = "2,10",
        unit = "m",
        hint = "Opcional. Altura livre até o teto.",
        error = state.heightError?.asString(),
        keyboardType = KeyboardType.Decimal,
    )
    KbField(
        value = state.directions,
        onValueChange = { onAction(SpotWizardAction.OnDirectionsChange(it)) },
        label = "Como chegar",
        placeholder = "Desça a rampa até o subsolo 2…",
        unit = "${state.directions.length}/$DIRECTIONS_MAX",
        singleLine = false,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label("Fotos de como chegar · em breve")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { KbPhotoPlaceholder(if (it == 0) "Foto 1" else "+", Modifier.weight(1f).height(104.dp)) }
        }
        Hint("Mostre a rampa, o elevador e a vaga. O envio de fotos chega numa próxima versão.")
    }
}

// --- Step 2 · board 14 -----------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PriceStep(state: SpotWizardState, onAction: (SpotWizardAction) -> Unit) {
    KbText("Preço e regras", KerbTheme.typography.lg)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label("Valor combinado")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KbField(state.priceHour, { onAction(SpotWizardAction.OnPriceHourChange(it)) }, "Hora", Modifier.weight(1f), placeholder = "8,00", unit = "R$", keyboardType = KeyboardType.Decimal)
            KbField(state.priceDay, { onAction(SpotWizardAction.OnPriceDayChange(it)) }, "Dia", Modifier.weight(1f), placeholder = "35,00", unit = "R$", keyboardType = KeyboardType.Decimal)
            KbField(state.priceWeek, { onAction(SpotWizardAction.OnPriceWeekChange(it)) }, "Semana", Modifier.weight(1f), placeholder = "180", unit = "R$", keyboardType = KeyboardType.Decimal)
        }
        KbErrorText(state.priceError?.asString())
        Hint("Deixe em branco o período que você não oferece.")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label("Período mínimo de aluguel")
        KbToolbar(
            options = MIN_PERIOD_OPTIONS.map { KbOption(it, SpotFormats.formatMinPeriod(it)) },
            selected = state.minPeriodMinutes,
            onSelect = { onAction(SpotWizardAction.OnMinPeriodSelect(it)) },
        )
        Hint("Pedidos menores que isso não são aceitos. Quem reserva vê o mínimo antes de pedir.")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label("Cancelamento sem aviso até")
        KbToolbar(
            options = CANCEL_NOTICE_OPTIONS.map { KbOption(it, "$it h antes") },
            selected = state.cancelNoticeHours,
            onSelect = { onAction(SpotWizardAction.OnCancelNoticeSelect(it)) },
        )
        Hint("Antes desse prazo, o locatário cancela sozinho. Depois, precisa combinar com você pelo chat.")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label("Como aceitar pedidos")
        KbToolbar(
            options = listOf(KbOption(ApprovalMode.MANUAL, "Aprovar cada pedido"), KbOption(ApprovalMode.AUTO, "Reserva imediata")),
            selected = state.approval,
            onSelect = { onAction(SpotWizardAction.OnApprovalSelect(it)) },
        )
        Hint("Você vê perfil, veículo e período antes de aceitar. Quem pede espera até 12 h.")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label("Regras da vaga")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESET_RULES.forEach { rule ->
                KbChip(text = rule, selected = rule in state.rules, onClick = { onAction(SpotWizardAction.OnRuleToggle(rule)) })
            }
        }
    }
    KbField(
        value = state.customRule,
        onValueChange = { onAction(SpotWizardAction.OnCustomRuleChange(it)) },
        label = "Outra regra",
        placeholder = "Ex.: avisar quando chegar",
        hint = "Aparece para o vizinho antes de reservar.",
    )
}

// --- Step 3 · board 15 -----------------------------------------------------------------------

private val weekdayChips = listOf(
    DayOfWeek.MONDAY to ("S" to "Segunda"),
    DayOfWeek.TUESDAY to ("T" to "Terça"),
    DayOfWeek.WEDNESDAY to ("Q" to "Quarta"),
    DayOfWeek.THURSDAY to ("Q" to "Quinta"),
    DayOfWeek.FRIDAY to ("S" to "Sexta"),
    DayOfWeek.SATURDAY to ("S" to "Sábado"),
    DayOfWeek.SUNDAY to ("D" to "Domingo"),
)

private val frequencyOptions = listOf(
    KbOption(RepeatFrequency.WEEKDAYS, "Todo dia útil"),
    KbOption(RepeatFrequency.WEEKENDS, "Todo fim de semana"),
    KbOption(RepeatFrequency.EVERY_DAY, "Todo dia"),
    KbOption(RepeatFrequency.ONLY_SELECTED, "Só nestes dias"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvailabilityStep(state: SpotWizardState, onAction: (SpotWizardAction) -> Unit) {
    val level = state.selectedLevel
    val sector = level?.sectors?.firstOrNull { it.id == state.sectorId }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KbText("Disponibilidade", KerbTheme.typography.lg)
        if (level != null) {
            KbText(
                text = "Vaga ${spotCode(level.name, sector?.name, state.number)} · ${state.condoName}".uppercase(),
                style = KerbTheme.typography.dataSmall,
                color = KerbTheme.colors.inkMuted,
            )
        }
    }
    KbToolbar(
        options = listOf(KbOption(DayAction.OPEN, "Liberar"), KbOption(DayAction.BLOCK, "Bloquear")),
        selected = state.dayAction,
        onSelect = { onAction(SpotWizardAction.OnDayActionSelect(it)) },
    )
    val today = state.today
    val firstMonth = LocalDate(today.year, today.month, 1)
    KbMonthCalendar(
        title = state.month.monthTitle(),
        leadingBlanks = state.month.leadingBlanks(),
        cells = calendarCells(state.month, today, state.availability, state.selectedDays),
        onDayClick = { day -> onAction(SpotWizardAction.OnDayClick(LocalDate(state.month.year, state.month.month, day))) },
        previousLabel = if (state.month > firstMonth) state.month.plus(-1, DateTimeUnit.MONTH).monthShort() else null,
        nextLabel = state.month.plus(1, DateTimeUnit.MONTH).monthShort(),
        onPrevious = { onAction(SpotWizardAction.OnPreviousMonth) },
        onNext = { onAction(SpotWizardAction.OnNextMonth) },
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KbDayLegend(KbDayState.Selected, "SEL · Em edição")
            KbDayLegend(KbDayState.Free, "Livre")
            KbDayLegend(KbDayState.Closed, "Fechada")
            KbDayLegend(KbDayState.Blocked, "BLQ · Bloqueada")
        }
        if (state.selectedDays.isNotEmpty()) {
            val count = state.selectedDays.size
            val days = if (count == 1) "1 dia" else "$count dias"
            KbButton(
                text = if (state.dayAction == DayAction.OPEN) "Liberar $days" else "Bloquear $days",
                onClick = { onAction(SpotWizardAction.OnApplyToSelection) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.dayAction == DayAction.OPEN) Hint("Os dias liberados usam o horário de \"Das\" e \"Às\" abaixo.")
        } else {
            Hint("Toque nos dias e escolha Liberar ou Bloquear. Dias passados não mudam.")
        }
    }
    KbPanel(title = "Repetir", aside = "RECORRÊNCIA") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            KbSelect(
                label = "Frequência",
                options = frequencyOptions,
                selected = state.frequency,
                placeholder = "Personalizada",
                onSelect = { onAction(SpotWizardAction.OnFrequencySelect(it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KbField(state.fromText, { onAction(SpotWizardAction.OnFromChange(it)) }, "Das", Modifier.weight(1f), placeholder = "08:00")
                KbField(state.toText, { onAction(SpotWizardAction.OnToChange(it)) }, "Às", Modifier.weight(1f), placeholder = "18:00")
            }
            KbErrorText(state.windowError?.asString())
            if (state.frequency != RepeatFrequency.ONLY_SELECTED) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    weekdayChips.forEach { (day, labels) ->
                        KbChip(
                            text = labels.first,
                            selected = day in state.repeatDays,
                            onClick = { onAction(SpotWizardAction.OnRepeatDayToggle(day)) },
                            modifier = Modifier.weight(1f).semantics { contentDescription = labels.second },
                        )
                    }
                }
            }
            KbButton(
                text = "Aplicar ao calendário",
                onClick = { onAction(SpotWizardAction.OnApplyRepeat) },
                variant = KbButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    KbErrorText(state.availabilityError?.asString())
}
