package com.adamfoerster.tuavaga.feature.bookings.presentation.list

import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.BookingTab
import com.adamfoerster.tuavaga.core.domain.util.DataError
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

@OptIn(ExperimentalCoroutinesApi::class)
class BookingsViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val later = booking(id = "later", start = LocalDateTime(2026, 10, 20, 8, 0), end = LocalDateTime(2026, 10, 20, 12, 0))
    private val soon = booking(id = "soon", status = BookingStatus.PENDING, condoId = "c2")
    private val old = booking(id = "old", status = BookingStatus.COMPLETED, start = LocalDateTime(2026, 9, 1, 8, 0), end = LocalDateTime(2026, 9, 1, 9, 0))
    private val older = booking(id = "older", status = BookingStatus.REJECTED, start = LocalDateTime(2026, 8, 1, 8, 0), end = LocalDateTime(2026, 8, 1, 9, 0))
    private val asOwner = booking(id = "mine", role = BookingRole.OWNER)

    @Test
    fun showsTheRenterBookingsByTab() {
        val repo = FakeBookings(later, old, soon, older, asOwner)
        val vm = BookingsViewModel(repo)

        val state = vm.state.value
        assertEquals(1, repo.refreshes)
        assertFalse(state.isRefreshing)
        assertEquals(BookingTab.UPCOMING, state.tab)
        assertEquals(listOf("soon", "later"), state.visible.map { it.id })
        assertEquals(2, state.condoCount)
        assertEquals(2, state.count(BookingTab.UPCOMING))

        vm.onAction(BookingsAction.OnTabSelect(BookingTab.HISTORY))
        // Most recent first; owner bookings never show here.
        assertEquals(listOf("old", "older"), vm.state.value.visible.map { it.id })
    }

    @Test
    fun opensOnOngoingWhenParked() {
        val vm = BookingsViewModel(FakeBookings(later, booking(id = "now", status = BookingStatus.IN_PROGRESS)))

        assertEquals(BookingTab.ONGOING, vm.state.value.tab)
        assertEquals(listOf("now"), vm.state.value.visible.map { it.id })
    }

    @Test
    fun offlineKeepsTheCachedList() {
        val repo = FakeBookings(later).apply { refreshError = DataError.Remote.NO_INTERNET }
        val vm = BookingsViewModel(repo)

        assertNotNull(vm.state.value.error)
        assertEquals(listOf("later"), vm.state.value.visible.map { it.id })

        repo.refreshError = null
        vm.onAction(BookingsAction.OnRefresh)
        assertNull(vm.state.value.error)
    }

    @Test
    fun cacheUpdatesFlowIn() {
        val repo = FakeBookings(later)
        val vm = BookingsViewModel(repo)

        repo.bookings.value = listOf(later.copy(status = BookingStatus.CANCELLED))

        assertEquals(emptyList(), vm.state.value.visible)
        assertEquals(1, vm.state.value.count(BookingTab.HISTORY))
    }
}
