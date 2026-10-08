package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.create

import com.adamfoerster.tuavaga.core.domain.condo.NewLevel
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoOnboardingSession
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CreateCondoViewModelTest {

    private lateinit var session: CondoOnboardingSession
    private lateinit var viewModel: CreateCondoViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        session = CondoOnboardingSession()
        viewModel = CreateCondoViewModel(session)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun addLevel(name: String) {
        viewModel.onAction(CreateCondoAction.OnLevelInputChange(name))
        viewModel.onAction(CreateCondoAction.OnAddLevel)
    }

    @Test
    fun garageNeedsAtLeastOneLevel() {
        viewModel.onAction(CreateCondoAction.OnNameChange("Residencial Alameda Verde"))
        viewModel.onAction(CreateCondoAction.OnAddressChange("Rua das Figueiras, 410"))
        viewModel.onAction(CreateCondoAction.OnSubmit)

        assertEquals(CreateTexts.levelRequired, viewModel.state.value.levelsError)
        assertNull(session.target)
    }

    @Test
    fun levelsGetLetteredSectorsAndCanBeReordered() {
        addLevel("Subsolo 1")
        addLevel("Subsolo 2")
        viewModel.onAction(CreateCondoAction.OnAddSector(1))
        viewModel.onAction(CreateCondoAction.OnAddSector(1))
        viewModel.onAction(CreateCondoAction.OnAddSector(1))
        viewModel.onAction(CreateCondoAction.OnRemoveSector(1, 0))
        viewModel.onAction(CreateCondoAction.OnAddSector(1))
        viewModel.onAction(CreateCondoAction.OnMoveLevelUp(1))

        val levels = viewModel.state.value.levels
        assertEquals(listOf("Subsolo 2", "Subsolo 1"), levels.map { it.name })
        // "A" was removed, so the next free letter is "A" again.
        assertEquals(listOf("B", "C", "A"), levels[0].sectors)
        assertEquals("", viewModel.state.value.levelInput)
    }

    @Test
    fun repeatedLevelIsRejected() {
        addLevel("Térreo")
        addLevel("térreo")

        assertEquals(1, viewModel.state.value.levels.size)
        assertEquals(CreateTexts.duplicateLevel, viewModel.state.value.levelsError)
    }

    @Test
    fun invalidCepIsReported() {
        viewModel.onAction(CreateCondoAction.OnCepChange("1310"))
        viewModel.onAction(CreateCondoAction.OnSubmit)

        assertNotNull(viewModel.state.value.cepError)
        assertNotNull(viewModel.state.value.nameError)
        assertNotNull(viewModel.state.value.addressError)
    }

    @Test
    fun validFormStoresTheDraftAndContinues() = runTest {
        viewModel.onAction(CreateCondoAction.OnNameChange(" Residencial Alameda Verde "))
        viewModel.onAction(CreateCondoAction.OnAddressChange("Rua das Figueiras, 410"))
        viewModel.onAction(CreateCondoAction.OnCepChange("01310100"))
        viewModel.onAction(CreateCondoAction.OnBlocksChange("A, B, C"))
        addLevel("Subsolo 1")
        viewModel.onAction(CreateCondoAction.OnAddSector(0))
        viewModel.onAction(CreateCondoAction.OnSubmit)

        assertEquals(CreateCondoEvent.GoToResidentData, viewModel.events.first())
        val draft = assertIs<CondoTarget.New>(session.target).condo
        assertEquals("Residencial Alameda Verde", draft.name)
        assertEquals("01310-100", draft.cep)
        assertEquals(listOf("A", "B", "C"), draft.blocks)
        assertEquals(listOf(NewLevel("Subsolo 1", listOf("A"))), draft.levels)
    }

    @Test
    fun nextSectorSkipsUsedLetters() {
        assertEquals("A", nextSectorName(emptyList()))
        assertEquals("C", nextSectorName(listOf("A", "b")))
    }
}
