package com.adamfoerster.tuavaga.feature.bookings.presentation.check

import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.feature.bookings.presentation.FakeBookings
import com.adamfoerster.tuavaga.feature.bookings.presentation.booking
import com.adamfoerster.tuavaga.feature.bookings.presentation.common.durationShort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CheckViewModelTest {

    private val now = LocalDateTime(2026, 10, 10, 8, 2)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun checkInNeedsTheThreeConfirmations() = runTest {
        val repo = FakeBookings(booking())
        val vm = CheckViewModel(CheckKind.IN, "b1", repo, now = { now })

        vm.onAction(CheckAction.OnToggle(0))
        vm.onAction(CheckAction.OnToggle(1))
        assertFalse(vm.state.value.canConfirm)
        vm.onAction(CheckAction.OnConfirmClick)
        assertTrue(repo.calls.isEmpty())

        vm.onAction(CheckAction.OnToggle(2))
        vm.onAction(CheckAction.OnToggle(1))
        vm.onAction(CheckAction.OnToggle(1))
        vm.onAction(CheckAction.OnConfirmClick)

        assertEquals(listOf("checkIn:b1"), repo.calls)
        assertEquals(CheckEvent.Done, vm.events.first())
        assertEquals(BookingStatus.IN_PROGRESS, repo.bookings.value.single().status)
    }

    @Test
    fun checkInRefusedStays() {
        val repo = FakeBookings(booking()).apply { actionError = BookingActionError.CheckInClosed }
        val vm = CheckViewModel(CheckKind.IN, "b1", repo, now = { now })
        (0..2).forEach { vm.onAction(CheckAction.OnToggle(it)) }
        vm.onAction(CheckAction.OnConfirmClick)

        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.isWorking)
    }

    @Test
    fun checkOut() = runTest {
        val repo = FakeBookings(booking(status = BookingStatus.IN_PROGRESS))
        val vm = CheckViewModel(CheckKind.OUT, "b1", repo, now = { now })
        (0..2).forEach { vm.onAction(CheckAction.OnToggle(it)) }
        vm.onAction(CheckAction.OnConfirmClick)

        assertEquals(listOf("checkOut:b1"), repo.calls)
        assertEquals(CheckEvent.Done, vm.events.first())
    }

    @Test
    fun moreTimeRecalculatesInTheSameUnit() {
        // 08:00 → 18:00 by the hour (10 × R$ 8).
        val repo = FakeBookings(booking(status = BookingStatus.IN_PROGRESS))
        val vm = CheckViewModel(CheckKind.OUT, "b1", repo, now = { now })

        vm.onAction(CheckAction.OnExtendClick)
        val state = vm.state.value
        assertTrue(state.isExtendOpen)
        assertEquals(12, state.extendOptions.size)
        assertEquals(LocalDateTime(2026, 10, 10, 18, 30), state.extendTo)
        assertEquals(LocalDateTime(2026, 10, 11, 0, 0), state.extendOptions.last())

        vm.onAction(CheckAction.OnExtendSelect(LocalDateTime(2026, 10, 10, 19, 0)))
        assertEquals(11, vm.state.value.quoteFor(LocalDateTime(2026, 10, 10, 19, 0))?.units)
        assertEquals(BillingUnit.HOUR, vm.state.value.quoteFor(LocalDateTime(2026, 10, 10, 19, 0))?.unit)

        vm.onAction(CheckAction.OnExtendConfirm)
        assertEquals(listOf("extend:b1"), repo.calls)
        assertFalse(vm.state.value.isExtendOpen)
        assertEquals(LocalDateTime(2026, 10, 10, 19, 0), vm.state.value.booking?.period?.end)
    }

    @Test
    fun whenLateTheOptionsStartAfterNow() {
        // Exit 18:00, now 18:42 (board 18 "Passou do horário 0:42").
        val repo = FakeBookings(booking(status = BookingStatus.IN_PROGRESS))
        val vm = CheckViewModel(CheckKind.OUT, "b1", repo, now = { LocalDateTime(2026, 10, 10, 18, 42) })
        vm.onAction(CheckAction.OnExtendClick)

        assertEquals(LocalDateTime(2026, 10, 10, 19, 0), vm.state.value.extendTo)
        assertEquals(12, vm.state.value.extendOptions.size)
    }

    @Test
    fun extensionNotFreeKeepsTheSheet() {
        val repo = FakeBookings(booking(status = BookingStatus.IN_PROGRESS)).apply { actionError = BookingActionError.SpotUnavailable }
        val vm = CheckViewModel(CheckKind.OUT, "b1", repo, now = { now })
        vm.onAction(CheckAction.OnExtendClick)
        vm.onAction(CheckAction.OnExtendConfirm)

        assertTrue(vm.state.value.isExtendOpen)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun durations() {
        assertEquals("42 min", durationShort(42))
        assertEquals("3 h", durationShort(180))
        assertEquals("3 h 20 min", durationShort(200))
        assertEquals("2 d 4 h", durationShort(2 * 24 * 60 + 4 * 60 + 10))
        assertEquals("1 d", durationShort(24 * 60))
    }
}
