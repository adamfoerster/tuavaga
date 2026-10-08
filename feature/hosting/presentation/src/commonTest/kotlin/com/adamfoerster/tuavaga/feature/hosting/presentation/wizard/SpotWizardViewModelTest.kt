package com.adamfoerster.tuavaga.feature.hosting.presentation.wizard

import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.domain.spot.ApprovalMode
import com.adamfoerster.tuavaga.core.domain.spot.Availability
import com.adamfoerster.tuavaga.core.domain.spot.DayAvailability
import com.adamfoerster.tuavaga.core.domain.spot.DayOverride
import com.adamfoerster.tuavaga.core.domain.spot.RepeatFrequency
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotError
import com.adamfoerster.tuavaga.core.domain.spot.TimeWindow
import com.adamfoerster.tuavaga.feature.hosting.presentation.FakeCondos
import com.adamfoerster.tuavaga.feature.hosting.presentation.FakeHosting
import com.adamfoerster.tuavaga.feature.hosting.presentation.membership
import com.adamfoerster.tuavaga.feature.hosting.presentation.spot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SpotWizardViewModelTest {

    private val today = LocalDate(2026, 10, 8) // a Thursday
    private lateinit var condos: FakeCondos
    private lateinit var hosting: FakeHosting

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        condos = FakeCondos(membership("c1", "Residencial Alameda Verde"), membership("c2", "Edifício Santa Clara"))
        hosting = FakeHosting()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun wizard(spotId: String? = null, condoId: String? = "c1") =
        SpotWizardViewModel(spotId, condoId, hosting, condos, today = { today })

    private fun SpotWizardViewModel.fillLocation() {
        onAction(SpotWizardAction.OnLevelSelect("s2"))
        onAction(SpotWizardAction.OnSectorSelect("s2b"))
        onAction(SpotWizardAction.OnNumberChange("14"))
        onAction(SpotWizardAction.OnNextClick)
    }

    private fun SpotWizardViewModel.fillPrices() {
        onAction(SpotWizardAction.OnPriceHourChange("8,00"))
        onAction(SpotWizardAction.OnNextClick)
    }

    @Test
    fun preselectsTheCondoAndLoadsItsGarage() {
        val vm = wizard(condoId = "c2")

        assertEquals("c2", vm.state.value.condoId)
        assertEquals(listOf("c2"), condos.garageCalls)
        assertEquals(3, vm.state.value.garage.size)
        assertEquals(false, vm.state.value.isLoading)
    }

    @Test
    fun locationNeedsLevelSectorAndNumber() {
        val vm = wizard()
        vm.onAction(SpotWizardAction.OnNextClick)

        assertNotNull(vm.state.value.levelError)
        assertNotNull(vm.state.value.numberError)
        assertEquals(1, vm.state.value.step)

        vm.onAction(SpotWizardAction.OnLevelSelect("s2"))
        vm.onAction(SpotWizardAction.OnNumberChange("14"))
        vm.onAction(SpotWizardAction.OnNextClick)
        assertNotNull(vm.state.value.sectorError)
    }

    @Test
    fun levelWithoutSectorsNeedsNoSector() {
        val vm = wizard()
        vm.onAction(SpotWizardAction.OnLevelSelect("t"))
        vm.onAction(SpotWizardAction.OnNumberChange("1"))
        vm.onAction(SpotWizardAction.OnNextClick)

        assertEquals(2, vm.state.value.step)
    }

    @Test
    fun changingTheLevelClearsTheSector() {
        val vm = wizard()
        vm.onAction(SpotWizardAction.OnLevelSelect("s2"))
        vm.onAction(SpotWizardAction.OnSectorSelect("s2b"))
        vm.onAction(SpotWizardAction.OnLevelSelect("s1"))

        assertNull(vm.state.value.sectorId)
    }

    @Test
    fun descriptionIsCappedAt40Characters() {
        val vm = wizard()
        vm.onAction(SpotWizardAction.OnDescriptionChange("x".repeat(60)))

        assertEquals(DESCRIPTION_MAX, vm.state.value.description.length)
    }

    @Test
    fun invalidHeightBlocksTheFirstStep() {
        val vm = wizard()
        vm.onAction(SpotWizardAction.OnHeightChange("12"))
        vm.fillLocation()

        assertEquals(WizardTexts.invalidHeight, vm.state.value.heightError)
        assertEquals(1, vm.state.value.step)
    }

    @Test
    fun pricesNeedAtLeastOneValidValue() {
        val vm = wizard()
        vm.fillLocation()
        vm.onAction(SpotWizardAction.OnNextClick)
        assertEquals(WizardTexts.priceRequired, vm.state.value.priceError)

        vm.onAction(SpotWizardAction.OnPriceDayChange("abc"))
        vm.onAction(SpotWizardAction.OnNextClick)
        assertEquals(WizardTexts.invalidPrice, vm.state.value.priceError)
        assertEquals(2, vm.state.value.step)
    }

    @Test
    fun repeatAndCalendarBuildTheAvailability() {
        val vm = wizard()
        vm.fillLocation()
        vm.fillPrices()
        assertEquals(3, vm.state.value.step)

        // Weekdays 08:00–18:00 (the default frequency), then open Saturday 10/10 and block Monday 12/10.
        vm.onAction(SpotWizardAction.OnApplyRepeat)
        vm.onAction(SpotWizardAction.OnDayClick(LocalDate(2026, 10, 10)))
        vm.onAction(SpotWizardAction.OnApplyToSelection)
        vm.onAction(SpotWizardAction.OnDayActionSelect(DayAction.BLOCK))
        vm.onAction(SpotWizardAction.OnDayClick(LocalDate(2026, 10, 12)))
        vm.onAction(SpotWizardAction.OnApplyToSelection)

        val availability = vm.state.value.availability
        assertEquals(RepeatFrequency.WEEKDAYS.days, availability.weekly.keys)
        assertEquals(DayAvailability.Open(TimeWindow(480, 1080)), availability.on(LocalDate(2026, 10, 10)))
        assertEquals(DayAvailability.Blocked, availability.on(LocalDate(2026, 10, 12)))
        assertTrue(vm.state.value.selectedDays.isEmpty())
    }

    @Test
    fun pastDaysCannotBeSelected() {
        val vm = wizard()
        vm.onAction(SpotWizardAction.OnDayClick(LocalDate(2026, 10, 1)))

        assertTrue(vm.state.value.selectedDays.isEmpty())
    }

    @Test
    fun invalidWindowIsReported() {
        val vm = wizard()
        vm.onAction(SpotWizardAction.OnFromChange("18:00"))
        vm.onAction(SpotWizardAction.OnToChange("08:00"))
        vm.onAction(SpotWizardAction.OnApplyRepeat)

        assertEquals(WizardTexts.invalidWindow, vm.state.value.windowError)
        assertTrue(vm.state.value.availability.isEmpty)
    }

    @Test
    fun editingWeekdayChipsMakesTheFrequencyCustom() {
        val vm = wizard()
        vm.onAction(SpotWizardAction.OnRepeatDayToggle(DayOfWeek.SATURDAY))
        assertNull(vm.state.value.frequency)

        vm.onAction(SpotWizardAction.OnRepeatDayToggle(DayOfWeek.SATURDAY))
        assertEquals(RepeatFrequency.WEEKDAYS, vm.state.value.frequency)
    }

    @Test
    fun publishingNeedsSomeAvailability() {
        val vm = wizard()
        vm.fillLocation()
        vm.fillPrices()
        vm.onAction(SpotWizardAction.OnNextClick)

        assertEquals(WizardTexts.availabilityRequired, vm.state.value.availabilityError)
        assertTrue(hosting.saved.isEmpty())
    }

    @Test
    fun publishSavesTheDraft() = runTest {
        val vm = wizard()
        vm.onAction(SpotWizardAction.OnFeatureToggle(SpotFeature.ELECTRIC))
        vm.onAction(SpotWizardAction.OnFeatureToggle(SpotFeature.COVERED))
        vm.onAction(SpotWizardAction.OnHeightChange("2,10"))
        vm.onAction(SpotWizardAction.OnDirectionsChange("Desça a rampa"))
        vm.fillLocation()
        vm.onAction(SpotWizardAction.OnApprovalSelect(ApprovalMode.AUTO))
        vm.onAction(SpotWizardAction.OnRuleToggle("Respeitar horário"))
        vm.onAction(SpotWizardAction.OnRuleToggle("Sem caminhonete"))
        vm.onAction(SpotWizardAction.OnCustomRuleChange(" Avisar quando chegar "))
        vm.fillPrices()
        vm.onAction(SpotWizardAction.OnApplyRepeat)
        vm.onAction(SpotWizardAction.OnNextClick)

        assertEquals(SpotWizardEvent.Saved, vm.events.first())
        val draft = hosting.saved.single()
        assertNull(draft.id)
        assertEquals("c1", draft.condoId)
        assertEquals("s2b", draft.sectorId)
        assertEquals(800, draft.prices.hourCents)
        assertEquals(ApprovalMode.AUTO, draft.approval)
        assertEquals(setOf(SpotFeature.ELECTRIC, SpotFeature.COVERED), draft.features)
        assertEquals(210, draft.heightCm)
        assertEquals("Desça a rampa", draft.directions)
        // Presets keep the chip order; the custom rule goes last.
        assertEquals(listOf("Sem caminhonete", "Respeitar horário", "Avisar quando chegar"), draft.rules)
    }

    @Test
    fun duplicateNumberGoesBackToTheFirstStep() {
        hosting.saveResult = Result.Failure(SpotError.DuplicateNumber)
        val vm = wizard()
        vm.fillLocation()
        vm.fillPrices()
        vm.onAction(SpotWizardAction.OnApplyRepeat)
        vm.onAction(SpotWizardAction.OnNextClick)

        assertEquals(1, vm.state.value.step)
        assertEquals(WizardTexts.duplicateNumber, vm.state.value.numberError)
    }

    @Test
    fun editingPrefillsEverythingAndDropsPastExceptions() = runTest {
        val availability = Availability()
            .withWeekly(RepeatFrequency.WEEKENDS.days, TimeWindow(0, 1440))
            .block(listOf(LocalDate(2026, 9, 26), LocalDate(2026, 10, 17)))
        hosting.spots += spot(id = "spot1", availability = availability)
        val vm = wizard(spotId = "spot1", condoId = null)

        val state = vm.state.value
        assertTrue(state.isEditing)
        assertEquals("s2b", state.sectorId)
        assertEquals("8", state.priceHour)
        assertEquals("35", state.priceDay)
        assertEquals(setOf("Sem caminhonete"), state.rules)
        assertEquals("Avisar quando chegar", state.customRule)
        assertEquals(RepeatFrequency.WEEKENDS, state.frequency)
        assertEquals("24:00", state.toText)
        assertEquals("2,10", state.heightText)
        assertEquals(setOf(SpotFeature.COVERED), state.features)

        vm.onAction(SpotWizardAction.OnNextClick)
        vm.onAction(SpotWizardAction.OnNextClick)
        vm.onAction(SpotWizardAction.OnNextClick)

        assertEquals(SpotWizardEvent.Saved, vm.events.first())
        val draft = hosting.saved.single()
        assertEquals("spot1", draft.id)
        assertEquals(mapOf(LocalDate(2026, 10, 17) to DayOverride.Blocked), draft.availability.overrides)
    }

    @Test
    fun backOnTheFirstStepExits() = runTest {
        val vm = wizard()
        vm.onAction(SpotWizardAction.OnBackClick)

        assertEquals(SpotWizardEvent.Exit, vm.events.first())
    }

    @Test
    fun calendarStaysWithinTheCurrentMonthAndAYearAhead() {
        val vm = wizard()
        vm.onAction(SpotWizardAction.OnPreviousMonth)
        assertEquals(LocalDate(2026, 10, 1), vm.state.value.month)

        repeat(20) { vm.onAction(SpotWizardAction.OnNextMonth) }
        assertEquals(LocalDate(2027, 10, 1), vm.state.value.month)
    }
}
