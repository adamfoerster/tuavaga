package com.adamfoerster.tuavaga.feature.profile.presentation

import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.condo.CondoError
import com.adamfoerster.tuavaga.core.domain.condo.MembershipKind
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleError
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import com.adamfoerster.tuavaga.feature.profile.domain.AccountError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var session: FakeSession
    private lateinit var condos: FakeCondos
    private lateinit var active: FakeActiveCondo
    private lateinit var vehicles: FakeVehicles
    private lateinit var account: FakeAccount
    private lateinit var viewModel: ProfileViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        session = FakeSession()
        condos = FakeCondos(
            membership("av", "Residencial Alameda Verde"),
            membership("sc", "Edifício Santa Clara", MembershipKind.WORK, block = null, unit = "3-15"),
        )
        active = FakeActiveCondo("av")
        vehicles = FakeVehicles(onix)
        account = FakeAccount()
        val bookings = FakeBookings(
            booking("b1", BookingStatus.COMPLETED),
            booking("b2", BookingStatus.CONFIRMED),
            booking("b3", BookingStatus.REJECTED),
            booking("b4", BookingStatus.COMPLETED, role = BookingRole.OWNER),
        )
        viewModel = ProfileViewModel(session, condos, active, vehicles, bookings, account)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun identityCondominiumsVehiclesAndBookings() {
        val state = viewModel.state.value
        assertEquals("Adam Foerster", state.userName)
        assertEquals("av", state.active?.condo?.id)
        assertEquals(listOf("ABC1D23"), state.vehicles.map { it.plate })
        // Rejected requests and bookings of my spots do not count.
        assertEquals(2, state.bookingCount)
        assertEquals("MORADOR · BL. B 142", state.memberships[0].roleLine())
        assertEquals("TRABALHO · SALA 3-15", state.memberships[1].roleLine())
    }

    @Test
    fun addVehicleValidatesFirst() {
        viewModel.onAction(ProfileAction.OnAddVehicleClick)
        viewModel.onAction(ProfileAction.OnPlateChange("xyz-12"))
        viewModel.onAction(ProfileAction.OnSaveVehicle)

        val draft = viewModel.state.value.vehicleDraft!!
        assertNotNull(draft.plateError)
        assertNotNull(draft.modelError)
        assertNotNull(draft.colorError)

        viewModel.onAction(ProfileAction.OnPlateChange("xyz-4e56"))
        viewModel.onAction(ProfileAction.OnModelChange(" Honda CG 160 "))
        viewModel.onAction(ProfileAction.OnColorSelect("Vermelho"))
        viewModel.onAction(ProfileAction.OnTypeSelect(VehicleType.MOTORCYCLE))
        viewModel.onAction(ProfileAction.OnSaveVehicle)

        assertNull(viewModel.state.value.vehicleDraft)
        val added = viewModel.state.value.vehicles.last()
        assertEquals("XYZ4E56", added.plate)
        assertEquals("Honda CG 160", added.model)
        assertEquals(VehicleType.MOTORCYCLE, added.type)
    }

    @Test
    fun duplicatePlateStaysOnTheField() {
        vehicles.error = VehicleError.DuplicatePlate
        viewModel.onAction(ProfileAction.OnAddVehicleClick)
        viewModel.onAction(ProfileAction.OnPlateChange("ABC1D23"))
        viewModel.onAction(ProfileAction.OnModelChange("Onix"))
        viewModel.onAction(ProfileAction.OnColorSelect("Preto"))
        viewModel.onAction(ProfileAction.OnSaveVehicle)

        assertEquals(ProfileTexts.duplicatePlate, viewModel.state.value.vehicleDraft?.plateError)
    }

    @Test
    fun editAndRemoveVehicle() {
        viewModel.onAction(ProfileAction.OnVehicleClick("v1"))
        assertEquals("Onix", viewModel.state.value.vehicleDraft?.model)
        viewModel.onAction(ProfileAction.OnColorSelect("Prata"))
        viewModel.onAction(ProfileAction.OnSaveVehicle)
        assertEquals("Prata", viewModel.state.value.vehicles.single().color)

        viewModel.onAction(ProfileAction.OnVehicleClick("v1"))
        viewModel.onAction(ProfileAction.OnRemoveVehicleClick)
        assertTrue(viewModel.state.value.vehicleDraft!!.isConfirmingRemove)
        viewModel.onAction(ProfileAction.OnRemoveVehicleConfirm)
        assertTrue(viewModel.state.value.vehicles.isEmpty())
    }

    @Test
    fun vehicleInAnActiveBookingIsKept() {
        vehicles.error = VehicleError.InUse
        viewModel.onAction(ProfileAction.OnVehicleClick("v1"))
        viewModel.onAction(ProfileAction.OnRemoveVehicleClick)
        viewModel.onAction(ProfileAction.OnRemoveVehicleConfirm)

        assertEquals(ProfileTexts.vehicleInUse, viewModel.state.value.error)
        assertEquals(1, viewModel.state.value.vehicles.size)
        assertFalse(viewModel.state.value.vehicleDraft!!.isConfirmingRemove)
    }

    @Test
    fun switchAndLeaveCondominiums() {
        viewModel.onAction(ProfileAction.OnCondoSelect("sc"))
        assertEquals("sc", active.activeCondoId.value)

        viewModel.onAction(ProfileAction.OnLeaveClick("av"))
        assertEquals("av", viewModel.state.value.leavingCondoId)
        viewModel.onAction(ProfileAction.OnLeaveConfirm)
        assertEquals(listOf("av"), condos.left)
        assertNull(viewModel.state.value.leavingCondoId)
        assertEquals(listOf("sc"), viewModel.state.value.memberships.map { it.condo.id })
    }

    @Test
    fun cannotLeaveWithActiveBookings() {
        condos.leaveError = CondoError.ActiveBookings
        viewModel.onAction(ProfileAction.OnLeaveClick("sc"))
        viewModel.onAction(ProfileAction.OnLeaveConfirm)

        assertEquals(ProfileTexts.leaveWithBookings, viewModel.state.value.error)
        assertEquals(2, viewModel.state.value.memberships.size)

        viewModel.onAction(ProfileAction.OnErrorDismiss)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun deleteAccountAsksThenSignsOut() {
        viewModel.onAction(ProfileAction.OnDeleteAccountClick)
        assertTrue(viewModel.state.value.isConfirmingDelete)
        assertEquals(0, account.deletions)

        viewModel.onAction(ProfileAction.OnDeleteConfirm)
        assertEquals(1, account.deletions)
        assertEquals(1, session.signOuts)
    }

    @Test
    fun deleteAccountWhileParkedIsRefused() {
        account.error = AccountError.BookingInProgress
        viewModel.onAction(ProfileAction.OnDeleteAccountClick)
        viewModel.onAction(ProfileAction.OnDeleteConfirm)

        assertEquals(ProfileTexts.deleteWhileParked, viewModel.state.value.error)
        assertEquals(0, session.signOuts)
        assertFalse(viewModel.state.value.isConfirmingDelete)
    }

    @Test
    fun signOutRunsOnceEvenWithRepeatedTaps() {
        viewModel.onAction(ProfileAction.OnSignOutClick)
        viewModel.onAction(ProfileAction.OnSignOutClick)

        assertEquals(1, session.signOuts)
    }
}
