package com.adamfoerster.tuavaga.feature.hosting.presentation.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.hosting.domain.HostingRepository
import com.adamfoerster.tuavaga.feature.hosting.domain.PRESET_RULES
import com.adamfoerster.tuavaga.feature.hosting.domain.Prices
import com.adamfoerster.tuavaga.feature.hosting.domain.RepeatFrequency
import com.adamfoerster.tuavaga.feature.hosting.domain.Spot
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotDraft
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotError
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotFormats
import com.adamfoerster.tuavaga.feature.hosting.domain.TimeWindow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Boards 13–15 · Cadastrar vaga (localização → preço e regras → disponibilidade).
 * One ViewModel for the three steps; [spotId] edits an existing spot, [condoId] preselects the
 * condominium of a new one. Nothing is saved until "Publicar vaga".
 */
class SpotWizardViewModel(
    private val spotId: String?,
    private val condoId: String?,
    private val hostingRepository: HostingRepository,
    private val condoRepository: CondoRepository,
    today: () -> LocalDate,
) : ViewModel() {

    private val _state = MutableStateFlow(
        today().let { now -> SpotWizardState(isEditing = spotId != null, today = now, month = now.firstOfMonth()) },
    )
    val state = _state.asStateFlow()

    private val eventChannel = Channel<SpotWizardEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: SpotWizardAction) {
        when (action) {
            SpotWizardAction.OnBackClick -> back()
            SpotWizardAction.OnNextClick -> next()
            SpotWizardAction.OnRetryLoad -> load()

            is SpotWizardAction.OnCondoSelect -> selectCondo(action.condoId)
            is SpotWizardAction.OnLevelSelect -> _state.update {
                if (it.levelId == action.levelId) it else it.copy(levelId = action.levelId, sectorId = null, levelError = null, sectorError = null)
            }
            is SpotWizardAction.OnSectorSelect -> _state.update { it.copy(sectorId = action.sectorId, sectorError = null) }
            is SpotWizardAction.OnNumberChange -> _state.update { it.copy(number = action.value.take(NUMBER_MAX), numberError = null) }
            is SpotWizardAction.OnSizeChange -> _state.update { it.copy(sizeLabel = action.value.take(SIZE_MAX)) }
            is SpotWizardAction.OnDescriptionChange -> _state.update { it.copy(description = action.value.take(DESCRIPTION_MAX)) }

            is SpotWizardAction.OnPriceHourChange -> _state.update { it.copy(priceHour = action.value, priceError = null) }
            is SpotWizardAction.OnPriceDayChange -> _state.update { it.copy(priceDay = action.value, priceError = null) }
            is SpotWizardAction.OnPriceWeekChange -> _state.update { it.copy(priceWeek = action.value, priceError = null) }
            is SpotWizardAction.OnMinPeriodSelect -> _state.update { it.copy(minPeriodMinutes = action.minutes) }
            is SpotWizardAction.OnCancelNoticeSelect -> _state.update { it.copy(cancelNoticeHours = action.hours) }
            is SpotWizardAction.OnApprovalSelect -> _state.update { it.copy(approval = action.mode) }
            is SpotWizardAction.OnRuleToggle -> _state.update {
                it.copy(rules = if (action.rule in it.rules) it.rules - action.rule else it.rules + action.rule)
            }
            is SpotWizardAction.OnCustomRuleChange -> _state.update { it.copy(customRule = action.value.take(CUSTOM_RULE_MAX)) }

            SpotWizardAction.OnPreviousMonth -> _state.update {
                val previous = it.month.minus(1, DateTimeUnit.MONTH)
                if (previous < it.today.firstOfMonth()) it else it.copy(month = previous)
            }
            SpotWizardAction.OnNextMonth -> _state.update {
                val next = it.month.plus(1, DateTimeUnit.MONTH)
                if (next > it.today.firstOfMonth().plus(MONTHS_AHEAD, DateTimeUnit.MONTH)) it else it.copy(month = next)
            }
            is SpotWizardAction.OnDayClick -> _state.update {
                when {
                    action.date < it.today -> it
                    action.date in it.selectedDays -> it.copy(selectedDays = it.selectedDays - action.date)
                    else -> it.copy(selectedDays = it.selectedDays + action.date, availabilityError = null)
                }
            }
            is SpotWizardAction.OnDayActionSelect -> _state.update { it.copy(dayAction = action.action) }
            SpotWizardAction.OnApplyToSelection -> applyToSelection()
            is SpotWizardAction.OnFrequencySelect -> _state.update {
                it.copy(frequency = action.frequency, repeatDays = action.frequency.days)
            }
            is SpotWizardAction.OnRepeatDayToggle -> _state.update {
                val days = if (action.day in it.repeatDays) it.repeatDays - action.day else it.repeatDays + action.day
                it.copy(repeatDays = days, frequency = RepeatFrequency.entries.firstOrNull { f -> f.days == days && f != RepeatFrequency.ONLY_SELECTED })
            }
            is SpotWizardAction.OnFromChange -> _state.update { it.copy(fromText = action.value, windowError = null) }
            is SpotWizardAction.OnToChange -> _state.update { it.copy(toText = action.value, windowError = null) }
            SpotWizardAction.OnApplyRepeat -> applyRepeat()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            val memberships = condoRepository.memberships.first()
            val condos = memberships.map { CondoChoice(it.condo.id, it.condo.name) }
            val spot = if (spotId != null) {
                when (val result = hostingRepository.spot(spotId)) {
                    is Result.Success -> result.data
                    is Result.Failure -> {
                        _state.update { it.copy(isLoading = false, loadError = result.error.toUiText()) }
                        return@launch
                    }
                }
            } else {
                null
            }
            val chosen = spot?.condoId ?: condoId?.takeIf { id -> condos.any { it.id == id } } ?: condos.firstOrNull()?.id
            _state.update { current -> (spot?.let { current.prefilledWith(it) } ?: current).copy(condos = condos, condoId = chosen) }
            if (chosen != null) loadGarage(chosen) else _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadGarage(condoId: String) {
        _state.update { it.copy(isLoading = true, loadError = null) }
        when (val result = condoRepository.garageOf(condoId)) {
            is Result.Success -> _state.update { current ->
                val levelStillThere = result.data.firstOrNull { it.id == current.levelId }
                current.copy(
                    garage = result.data,
                    levelId = levelStillThere?.id,
                    sectorId = current.sectorId?.takeIf { id -> levelStillThere?.sectors?.any { it.id == id } == true },
                    isLoading = false,
                )
            }
            is Result.Failure -> _state.update { it.copy(isLoading = false, loadError = result.error.toUiText()) }
        }
    }

    private fun selectCondo(condoId: String) {
        if (_state.value.condoId == condoId) return
        _state.update { it.copy(condoId = condoId, garage = emptyList(), levelId = null, sectorId = null) }
        viewModelScope.launch { loadGarage(condoId) }
    }

    private fun back() {
        val step = _state.value.step
        if (step > 1) {
            _state.update { it.copy(step = step - 1, error = null) }
        } else {
            viewModelScope.launch { eventChannel.send(SpotWizardEvent.Exit) }
        }
    }

    private fun next() {
        val current = _state.value
        if (current.isLoading || current.isSaving) return
        when (current.step) {
            1 -> if (validateLocation()) _state.update { it.copy(step = 2) }
            2 -> if (validatePrices() != null) _state.update { it.copy(step = 3) }
            else -> save()
        }
    }

    private fun validateLocation(): Boolean {
        val s = _state.value
        val level = s.selectedLevel
        val checked = s.copy(
            levelError = WizardTexts.levelRequired.takeIf { level == null },
            sectorError = WizardTexts.sectorRequired.takeIf { level != null && level.sectors.isNotEmpty() && s.sectorId == null },
            numberError = WizardTexts.numberRequired.takeIf { s.number.isBlank() },
        )
        _state.value = checked
        return s.condoId != null && checked.levelError == null && checked.sectorError == null && checked.numberError == null
    }

    /** The prices typed, or `null` (with the error shown) if none is valid. */
    private fun validatePrices(): Prices? {
        val s = _state.value
        val texts = listOf(s.priceHour, s.priceDay, s.priceWeek)
        val parsed = texts.map { SpotFormats.parsePriceCents(it) }
        val invalid = texts.zip(parsed).any { (text, cents) -> text.isNotBlank() && cents == null }
        val prices = Prices(parsed[0], parsed[1], parsed[2])
        val error = when {
            invalid -> WizardTexts.invalidPrice
            prices.isEmpty -> WizardTexts.priceRequired
            else -> null
        }
        _state.update { it.copy(priceError = error) }
        return prices.takeIf { error == null }
    }

    /** The "Das / Às" window, or `null` (with the error shown) if it is not a valid range. */
    private fun window(): TimeWindow? {
        val s = _state.value
        val from = SpotFormats.parseTime(s.fromText)
        val to = SpotFormats.parseTime(s.toText)
        val window = if (from != null && to != null && from < to && from < TimeWindow.MINUTES_PER_DAY) TimeWindow(from, to) else null
        if (window == null) _state.update { it.copy(windowError = WizardTexts.invalidWindow) }
        return window
    }

    private fun applyToSelection() {
        val s = _state.value
        if (s.selectedDays.isEmpty()) return
        val updated = when (s.dayAction) {
            DayAction.BLOCK -> s.availability.block(s.selectedDays)
            DayAction.OPEN -> s.availability.open(s.selectedDays, window() ?: return)
        }
        _state.update { it.copy(availability = updated, selectedDays = emptySet(), availabilityError = null) }
    }

    private fun applyRepeat() {
        val s = _state.value
        val days = if (s.frequency == RepeatFrequency.ONLY_SELECTED) emptySet() else s.repeatDays
        val window = window() ?: return
        _state.update { it.copy(availability = it.availability.withWeekly(days, window), availabilityError = null) }
    }

    private fun save() {
        val s = _state.value
        val prices = validatePrices() ?: run {
            _state.update { it.copy(step = 2) }
            return
        }
        if (s.availability.isEmpty) {
            _state.update { it.copy(availabilityError = WizardTexts.availabilityRequired) }
            return
        }
        val draft = SpotDraft(
            id = spotId,
            condoId = s.condoId ?: return,
            levelId = s.levelId ?: return,
            sectorId = s.sectorId,
            number = s.number.trim(),
            sizeLabel = s.sizeLabel.trim().ifEmpty { null },
            description = s.description.trim().ifEmpty { null },
            prices = prices,
            minPeriodMinutes = s.minPeriodMinutes,
            cancelNoticeHours = s.cancelNoticeHours,
            approval = s.approval,
            rules = PRESET_RULES.filter { it in s.rules } + listOfNotNull(s.customRule.trim().ifEmpty { null }),
            availability = s.availability.withoutPastOverrides(s.today),
        )
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            when (val result = hostingRepository.save(draft)) {
                is Result.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    eventChannel.send(SpotWizardEvent.Saved)
                }
                is Result.Failure -> _state.update {
                    when (val error = result.error) {
                        SpotError.DuplicateNumber -> it.copy(isSaving = false, step = 1, numberError = WizardTexts.duplicateNumber)
                        is SpotError.Remote -> it.copy(isSaving = false, error = error.error.toUiText())
                    }
                }
            }
        }
    }

    companion object {
        /** How far ahead the calendar goes. */
        const val MONTHS_AHEAD = 12
    }
}

internal fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, month, 1)

private fun SpotWizardState.prefilledWith(spot: Spot): SpotWizardState = copy(
    levelId = spot.levelId,
    sectorId = spot.sectorId,
    number = spot.number,
    sizeLabel = spot.sizeLabel.orEmpty(),
    description = spot.description.orEmpty(),
    priceHour = spot.prices.hourCents?.let { SpotFormats.formatPriceInput(it) }.orEmpty(),
    priceDay = spot.prices.dayCents?.let { SpotFormats.formatPriceInput(it) }.orEmpty(),
    priceWeek = spot.prices.weekCents?.let { SpotFormats.formatPriceInput(it) }.orEmpty(),
    minPeriodMinutes = spot.minPeriodMinutes,
    cancelNoticeHours = spot.cancelNoticeHours,
    approval = spot.approval,
    rules = spot.rules.filter { it in PRESET_RULES }.toSet(),
    customRule = spot.rules.firstOrNull { it !in PRESET_RULES }.orEmpty(),
    availability = spot.availability,
    frequency = RepeatFrequency.entries.firstOrNull { it.days == spot.availability.weekly.keys && it.days.isNotEmpty() }
        ?: if (spot.availability.weekly.isEmpty()) RepeatFrequency.ONLY_SELECTED else null,
    repeatDays = spot.availability.weekly.keys,
    fromText = spot.availability.weekly.values.firstOrNull()?.let { SpotFormats.formatTime(it.startMinutes) } ?: fromText,
    toText = spot.availability.weekly.values.firstOrNull()?.let { SpotFormats.formatTime(it.endMinutes) } ?: toText,
)

internal object WizardTexts {
    val levelRequired = UiText.Dynamic("Escolha o subsolo ou andar.")
    val sectorRequired = UiText.Dynamic("Escolha o setor.")
    val numberRequired = UiText.Dynamic("Informe o número da vaga.")
    val duplicateNumber = UiText.Dynamic("Já existe uma vaga com esse número neste andar e setor.")
    val priceRequired = UiText.Dynamic("Informe pelo menos um valor: hora, dia ou semana.")
    val invalidPrice = UiText.Dynamic("Valor inválido. Use o formato 8,00.")
    val invalidWindow = UiText.Dynamic("Horário inválido. Use 08:00 a 18:00, com o início antes do fim.")
    val availabilityRequired = UiText.Dynamic("Libere pelo menos um dia ou aplique uma repetição.")
}
