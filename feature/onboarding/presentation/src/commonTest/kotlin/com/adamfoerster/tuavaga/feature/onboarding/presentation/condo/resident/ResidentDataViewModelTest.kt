package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.resident

import com.adamfoerster.tuavaga.core.domain.condo.CondoError
import com.adamfoerster.tuavaga.core.domain.condo.MembershipKind
import com.adamfoerster.tuavaga.core.domain.condo.NewCondominium
import com.adamfoerster.tuavaga.core.domain.condo.NewLevel
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.domain.vehicle.Vehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleError
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import com.adamfoerster.tuavaga.feature.onboarding.presentation.FakeActiveCondoRepository
import com.adamfoerster.tuavaga.feature.onboarding.presentation.FakeCondoRepository
import com.adamfoerster.tuavaga.feature.onboarding.presentation.FakeSessionRepository
import com.adamfoerster.tuavaga.feature.onboarding.presentation.FakeVehicleRepository
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoOnboardingSession
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoTarget
import com.adamfoerster.tuavaga.feature.onboarding.presentation.preview
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ResidentDataViewModelTest {

    private lateinit var session: CondoOnboardingSession
    private lateinit var condos: FakeCondoRepository
    private lateinit var active: FakeActiveCondoRepository
    private lateinit var vehicles: FakeVehicleRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        session = CondoOnboardingSession()
        condos = FakeCondoRepository()
        active = FakeActiveCondoRepository()
        vehicles = FakeVehicleRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ResidentDataViewModel(session, condos, active, vehicles, FakeSessionRepository())

    private fun ResidentDataViewModel.fillResident(block: String? = "B") {
        onAction(ResidentDataAction.OnUnitChange("142"))
        block?.let { onAction(ResidentDataAction.OnBlockSelect(it)) }
        onAction(ResidentDataAction.OnTermsChange(true))
    }

    @Test
    fun loadsBlocksAndPrefillsTheName() {
        session.target = CondoTarget.Existing(preview(blocks = null))
        val vm = viewModel()

        val state = vm.state.value
        assertEquals("Residencial Alameda Verde", state.condoName)
        assertEquals(listOf("A", "B", "C"), state.blocks)
        assertEquals("Adam Foerster", state.fullName)
        assertEquals(1, state.vehicleDrafts.size)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun existingVehiclesSkipTheEmptyForm() {
        vehicles = FakeVehicleRepository(listOf(Vehicle("v0", "ABC1D23", "Onix", "Preto", VehicleType.CAR)))
        session.target = CondoTarget.Existing(preview())
        val vm = viewModel()

        assertEquals(1, vm.state.value.existingVehicles.size)
        assertTrue(vm.state.value.vehicleDrafts.isEmpty())
    }

    @Test
    fun requiredFieldsAreValidated() {
        session.target = CondoTarget.Existing(preview(blocks = listOf("A", "B")))
        val vm = viewModel()
        vm.onAction(ResidentDataAction.OnPlateChange(0, "AB12"))
        vm.onAction(ResidentDataAction.OnPhoneChange("1234"))
        vm.onAction(ResidentDataAction.OnSubmit)

        val state = vm.state.value
        assertNotNull(state.blockError)
        assertNotNull(state.unitError)
        assertNotNull(state.termsError)
        assertNotNull(state.phoneError)
        assertNotNull(state.vehicleDrafts[0].plateError)
        assertNotNull(state.vehicleDrafts[0].modelError)
        assertTrue(condos.joins.isEmpty())
    }

    @Test
    fun joiningSavesVehicleSetsActiveAndRefreshes() = runTest {
        session.target = CondoTarget.Existing(preview(id = "c1", blocks = listOf("A", "B")))
        val vm = viewModel()
        vm.fillResident()
        vm.onAction(ResidentDataAction.OnKindSelect(MembershipKind.WORK))
        vm.onAction(ResidentDataAction.OnPhoneChange("11912345678"))
        vm.onAction(ResidentDataAction.OnPlateChange(0, "abc-1d23"))
        vm.onAction(ResidentDataAction.OnModelChange(0, "Chevrolet Onix"))
        vm.onAction(ResidentDataAction.OnColorSelect(0, "Preto"))
        vm.onAction(ResidentDataAction.OnSubmit)

        assertEquals(ResidentDataEvent.Finished, vm.events.first())
        val (condoId, resident) = condos.joins.single()
        assertEquals("c1", condoId)
        assertEquals("B", resident.block)
        assertEquals("(11) 91234-5678", resident.phone)
        assertEquals(MembershipKind.WORK, resident.kind)
        assertEquals("ABC1D23", vehicles.added.single().plate)
        assertEquals("c1", active.activeCondoId.value)
        assertEquals(1, condos.refreshCalls)
        assertNull(session.target)
    }

    @Test
    fun blankVehicleFormIsOptional() = runTest {
        session.target = CondoTarget.Existing(preview(blocks = emptyList()))
        val vm = viewModel()
        vm.fillResident(block = null)
        vm.onAction(ResidentDataAction.OnSubmit)

        assertEquals(ResidentDataEvent.Finished, vm.events.first())
        assertTrue(vehicles.added.isEmpty())
        assertNull(condos.joins.single().second.block)
    }

    @Test
    fun duplicatePlateStopsBeforeJoining() {
        vehicles.failNextWith = VehicleError.DuplicatePlate
        session.target = CondoTarget.Existing(preview(blocks = emptyList()))
        val vm = viewModel()
        vm.fillResident(block = null)
        vm.onAction(ResidentDataAction.OnPlateChange(0, "ABC1234"))
        vm.onAction(ResidentDataAction.OnModelChange(0, "Onix"))
        vm.onAction(ResidentDataAction.OnColorSelect(0, "Preto"))
        vm.onAction(ResidentDataAction.OnSubmit)

        assertEquals(ResidentTexts.duplicatePlate, vm.state.value.vehicleDrafts[0].plateError)
        assertTrue(condos.joins.isEmpty())
        assertEquals(false, vm.state.value.isSubmitting)
    }

    @Test
    fun newCondoIsCreatedWithTheResident() = runTest {
        session.target = CondoTarget.New(
            NewCondominium("Residencial Alameda Verde", "Rua das Figueiras, 410", null, listOf("A"), listOf(NewLevel("Subsolo 1", emptyList()))),
        )
        val vm = viewModel()
        assertTrue(vm.state.value.isNewCondo)
        vm.fillResident(block = "A")
        vm.onAction(ResidentDataAction.OnSubmit)

        assertEquals(ResidentDataEvent.Finished, vm.events.first())
        assertEquals("A", condos.creates.single().second.block)
        assertEquals("new-condo", active.activeCondoId.value)
    }

    @Test
    fun failedCreateCanBeRetried() = runTest {
        condos.createResult = Result.Failure(CondoError.Remote(DataError.Remote.NO_INTERNET))
        session.target = CondoTarget.New(
            NewCondominium("Edifício Santa Clara", "Rua X, 1", null, emptyList(), listOf(NewLevel("Térreo", emptyList()))),
        )
        val vm = viewModel()
        vm.fillResident(block = null)
        vm.onAction(ResidentDataAction.OnSubmit)
        assertNotNull(vm.state.value.error)

        condos.createResult = Result.Success("c2")
        vm.onAction(ResidentDataAction.OnSubmit)

        assertEquals(ResidentDataEvent.Finished, vm.events.first())
        assertEquals(2, condos.creates.size)
        assertEquals("c2", active.activeCondoId.value)
    }
}
