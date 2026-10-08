package com.adamfoerster.tuavaga.feature.hosting.presentation.wizard

import com.adamfoerster.tuavaga.core.domain.condo.GarageLevel
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.feature.hosting.domain.ApprovalMode
import com.adamfoerster.tuavaga.feature.hosting.domain.Availability
import com.adamfoerster.tuavaga.feature.hosting.domain.RepeatFrequency
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** What tapping "Aplicar" does to the selected days (board 15 toolbar). */
enum class DayAction { OPEN, BLOCK }

data class CondoChoice(val id: String, val name: String)

data class SpotWizardState(
    val step: Int = 1,
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    // Step 1 · Localização
    val condos: List<CondoChoice> = emptyList(),
    val condoId: String? = null,
    val garage: List<GarageLevel> = emptyList(),
    val levelId: String? = null,
    val sectorId: String? = null,
    val number: String = "",
    val sizeLabel: String = "",
    val description: String = "",
    val levelError: UiText? = null,
    val sectorError: UiText? = null,
    val numberError: UiText? = null,
    // Step 2 · Preço e regras
    val priceHour: String = "",
    val priceDay: String = "",
    val priceWeek: String = "",
    val minPeriodMinutes: Int = 120,
    val cancelNoticeHours: Int = 24,
    val approval: ApprovalMode = ApprovalMode.MANUAL,
    val rules: Set<String> = emptySet(),
    val customRule: String = "",
    val priceError: UiText? = null,
    // Step 3 · Disponibilidade
    val availability: Availability = Availability(),
    val today: LocalDate,
    /** First day of the month shown in the calendar. */
    val month: LocalDate,
    val selectedDays: Set<LocalDate> = emptySet(),
    val dayAction: DayAction = DayAction.OPEN,
    /** `null` = the weekday chips were edited by hand ("Personalizada"). */
    val frequency: RepeatFrequency? = RepeatFrequency.WEEKDAYS,
    val repeatDays: Set<DayOfWeek> = RepeatFrequency.WEEKDAYS.days,
    val fromText: String = "08:00",
    val toText: String = "18:00",
    val windowError: UiText? = null,
    val availabilityError: UiText? = null,
    // Saving
    val isSaving: Boolean = false,
    val error: UiText? = null,
) {
    val selectedLevel: GarageLevel? get() = garage.firstOrNull { it.id == levelId }
    val condoName: String get() = condos.firstOrNull { it.id == condoId }?.name.orEmpty()
}

sealed interface SpotWizardAction {
    data object OnBackClick : SpotWizardAction
    data object OnNextClick : SpotWizardAction
    data object OnRetryLoad : SpotWizardAction

    data class OnCondoSelect(val condoId: String) : SpotWizardAction
    data class OnLevelSelect(val levelId: String) : SpotWizardAction
    data class OnSectorSelect(val sectorId: String) : SpotWizardAction
    data class OnNumberChange(val value: String) : SpotWizardAction
    data class OnSizeChange(val value: String) : SpotWizardAction
    data class OnDescriptionChange(val value: String) : SpotWizardAction

    data class OnPriceHourChange(val value: String) : SpotWizardAction
    data class OnPriceDayChange(val value: String) : SpotWizardAction
    data class OnPriceWeekChange(val value: String) : SpotWizardAction
    data class OnMinPeriodSelect(val minutes: Int) : SpotWizardAction
    data class OnCancelNoticeSelect(val hours: Int) : SpotWizardAction
    data class OnApprovalSelect(val mode: ApprovalMode) : SpotWizardAction
    data class OnRuleToggle(val rule: String) : SpotWizardAction
    data class OnCustomRuleChange(val value: String) : SpotWizardAction

    data object OnPreviousMonth : SpotWizardAction
    data object OnNextMonth : SpotWizardAction
    data class OnDayClick(val date: LocalDate) : SpotWizardAction
    data class OnDayActionSelect(val action: DayAction) : SpotWizardAction
    data object OnApplyToSelection : SpotWizardAction
    data class OnFrequencySelect(val frequency: RepeatFrequency) : SpotWizardAction
    data class OnRepeatDayToggle(val day: DayOfWeek) : SpotWizardAction
    data class OnFromChange(val value: String) : SpotWizardAction
    data class OnToChange(val value: String) : SpotWizardAction
    data object OnApplyRepeat : SpotWizardAction
}

sealed interface SpotWizardEvent {
    data object Saved : SpotWizardEvent
    data object Exit : SpotWizardEvent
}

const val DESCRIPTION_MAX = 40
const val SIZE_MAX = 20
const val NUMBER_MAX = 6
const val CUSTOM_RULE_MAX = 60
