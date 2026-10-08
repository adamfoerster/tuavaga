package com.adamfoerster.tuavaga.feature.bookings.presentation.detail

import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.RejectReason
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.bookings.presentation.FakeBookings
import com.adamfoerster.tuavaga.feature.bookings.presentation.booking
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BookingDetailViewModelTest {

    private var clock = LocalDateTime(2026, 10, 8, 14, 0)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repo: FakeBookings, id: String = "b1") = BookingDetailViewModel(id, repo, now = { clock })

    @Test
    fun showsTheCachedBookingAndRefreshes() {
        val repo = FakeBookings(booking())
        val vm = viewModel(repo)

        assertEquals("b1", vm.state.value.booking?.id)
        assertFalse(vm.state.value.isLoading)
        assertEquals(1, repo.refreshes)
    }

    @Test
    fun notFoundOnlyAfterASuccessfulRefresh() {
        val repo = FakeBookings().apply { refreshError = DataError.Remote.NO_INTERNET }
        val vm = viewModel(repo)
        assertFalse(vm.state.value.notFound)
        assertEquals(DataError.Remote.NO_INTERNET.toUiText(), vm.state.value.error)

        repo.refreshError = null
        vm.onAction(BookingDetailAction.OnRetry)
        assertTrue(vm.state.value.notFound)
    }

    @Test
    fun aBookingRequestedJustNowAppearsAfterTheRefresh() {
        val repo = FakeBookings()
        val vm = viewModel(repo)
        repo.bookings.value = listOf(booking())

        assertEquals("b1", vm.state.value.booking?.id)
        assertFalse(vm.state.value.notFound)
    }

    @Test
    fun cancelAsksFirst() {
        val repo = FakeBookings(booking())
        val vm = viewModel(repo)

        vm.onAction(BookingDetailAction.OnCancelClick)
        assertTrue(vm.state.value.isConfirmingCancel)
        assertTrue(repo.calls.isEmpty())

        vm.onAction(BookingDetailAction.OnCancelConfirm)
        assertEquals(listOf("cancel:b1"), repo.calls)
        assertEquals(BookingStatus.CANCELLED, vm.state.value.booking?.status)
        assertFalse(vm.state.value.isConfirmingCancel)
    }

    @Test
    fun cancelRefusedByTheServerShowsWhy() {
        val repo = FakeBookings(booking()).apply { actionError = BookingActionError.CancelWindowClosed }
        val vm = viewModel(repo)
        vm.onAction(BookingDetailAction.OnCancelClick)
        vm.onAction(BookingDetailAction.OnCancelConfirm)

        assertEquals(BookingActionError.CancelWindowClosed.toUiText(), vm.state.value.error)
        assertTrue(vm.state.value.isConfirmingCancel)
    }

    @Test
    fun ownerAnswersARequest() {
        val repo = FakeBookings(booking(role = BookingRole.OWNER, status = BookingStatus.PENDING))
        val vm = viewModel(repo)

        vm.onAction(BookingDetailAction.OnRejectClick)
        vm.onAction(BookingDetailAction.OnRejectConfirm)
        // A reason is required.
        assertTrue(repo.calls.isEmpty())

        vm.onAction(BookingDetailAction.OnRejectReason(RejectReason.VISIT))
        vm.onAction(BookingDetailAction.OnRejectMessage("Posso liberar outro dia."))
        vm.onAction(BookingDetailAction.OnRejectConfirm)
        assertEquals(listOf("reject:b1"), repo.calls)
        assertEquals(RejectReason.VISIT, vm.state.value.booking?.rejectReason)
        assertNull(vm.state.value.rejecting)
    }

    @Test
    fun approveConflictKeepsTheRequest() {
        val repo = FakeBookings(booking(role = BookingRole.OWNER, status = BookingStatus.PENDING)).apply {
            actionError = BookingActionError.Conflict
        }
        val vm = viewModel(repo)
        vm.onAction(BookingDetailAction.OnApproveClick)

        assertNotNull(vm.state.value.error)
        assertEquals(BookingStatus.PENDING, vm.state.value.booking?.status)
        assertFalse(vm.state.value.isWorking)
    }

    @Test
    fun tickMovesTheClock() {
        val vm = viewModel(FakeBookings(booking()))
        clock = LocalDateTime(2026, 10, 10, 7, 45)
        vm.onAction(BookingDetailAction.OnTick)

        assertEquals(clock, vm.state.value.now)
    }
}
