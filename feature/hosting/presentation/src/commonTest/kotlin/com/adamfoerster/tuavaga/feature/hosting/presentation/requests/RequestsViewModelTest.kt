package com.adamfoerster.tuavaga.feature.hosting.presentation.requests

import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.RejectReason
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.hosting.presentation.FakeBookings
import com.adamfoerster.tuavaga.feature.hosting.presentation.ownerBooking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class RequestsViewModelTest {

    private lateinit var repo: FakeBookings
    private lateinit var vm: RequestsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = FakeBookings(
            ownerBooking("late", status = BookingStatus.PENDING, respondBy = LocalDateTime(2026, 10, 9, 20, 0)),
            ownerBooking("first", status = BookingStatus.PENDING, respondBy = LocalDateTime(2026, 10, 9, 8, 0)),
            ownerBooking("confirmed"),
            ownerBooking("asking", status = BookingStatus.PENDING, role = BookingRole.RENTER),
        )
        vm = RequestsViewModel(repo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onlyPendingRequestsForMySpotsByDeadline() {
        assertEquals(listOf("first", "late"), vm.state.value.requests.map { it.id })
        assertEquals(1, repo.refreshes)
    }

    @Test
    fun acceptRemovesItFromTheList() {
        vm.onAction(RequestsAction.OnAcceptClick("first"))

        assertEquals(listOf("approve:first"), repo.calls)
        assertEquals(listOf("late"), vm.state.value.requests.map { it.id })
        assertNull(vm.state.value.workingId)
    }

    @Test
    fun conflictStaysOnTheCard() {
        repo.actionError = BookingActionError.Conflict
        vm.onAction(RequestsAction.OnAcceptClick("first"))

        assertEquals("first" to BookingActionError.Conflict.toUiText(), vm.state.value.actionError)
        assertEquals(2, vm.state.value.requests.size)
    }

    @Test
    fun rejectWithReasonAndMessage() {
        vm.onAction(RequestsAction.OnRejectClick("late"))
        assertEquals("late", vm.state.value.rejecting?.bookingId)
        vm.onAction(RequestsAction.OnRejectConfirm)
        assertEquals(emptyList(), repo.calls)

        vm.onAction(RequestsAction.OnRejectReason(RejectReason.OWN_USE))
        vm.onAction(RequestsAction.OnRejectMessage("Posso liberar outro dia."))
        vm.onAction(RequestsAction.OnRejectConfirm)

        assertEquals(listOf("reject:uso:Posso liberar outro dia.:late"), repo.calls)
        assertNull(vm.state.value.rejecting)
        assertEquals(listOf("first"), vm.state.value.requests.map { it.id })
    }

    @Test
    fun requestAnsweredElsewhereClosesTheForm() {
        vm.onAction(RequestsAction.OnRejectClick("late"))
        repo.bookings.value = repo.bookings.value.map { if (it.id == "late") it.copy(status = BookingStatus.EXPIRED) else it }

        assertNull(vm.state.value.rejecting)
    }
}
