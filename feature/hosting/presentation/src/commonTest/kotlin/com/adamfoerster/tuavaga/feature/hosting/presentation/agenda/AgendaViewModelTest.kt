package com.adamfoerster.tuavaga.feature.hosting.presentation.agenda

import com.adamfoerster.tuavaga.core.designsystem.components.KbDayState
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.spot.Availability
import com.adamfoerster.tuavaga.core.domain.spot.TimeWindow
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.feature.hosting.presentation.FakeBookings
import com.adamfoerster.tuavaga.feature.hosting.presentation.FakeHosting
import com.adamfoerster.tuavaga.feature.hosting.presentation.ownerBooking
import com.adamfoerster.tuavaga.feature.hosting.presentation.spot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModelTest {

    private val availability = Availability()
        .withWeekly(DayOfWeek.entries.toSet(), TimeWindow(8 * 60, 18 * 60))
        .block(listOf(LocalDate(2026, 10, 25)))
    private lateinit var bookings: FakeBookings

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        bookings = FakeBookings(
            // Board 28: Beatriz qua 07 (past), Rafael sáb 10 → dom 11, Paulo sex 16 (waiting).
            ownerBooking("bea", status = BookingStatus.COMPLETED, start = LocalDateTime(2026, 10, 7, 8, 0), end = LocalDateTime(2026, 10, 7, 18, 0)),
            ownerBooking("rafa"),
            ownerBooking("paulo", status = BookingStatus.PENDING, start = LocalDateTime(2026, 10, 16, 19, 0), end = LocalDateTime(2026, 10, 16, 23, 0)),
            ownerBooking("gone", status = BookingStatus.REJECTED, start = LocalDateTime(2026, 10, 20, 8, 0), end = LocalDateTime(2026, 10, 20, 9, 0)),
            ownerBooking("other", spotId = "spot2"),
            ownerBooking("nov", start = LocalDateTime(2026, 11, 3, 8, 0), end = LocalDateTime(2026, 11, 3, 9, 0)),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(hosting: FakeHosting = FakeHosting(listOf(spot(id = "spot1", availability = availability)))) =
        AgendaViewModel("spot1", hosting, bookings, today = { LocalDate(2026, 10, 8) })

    @Test
    fun monthListAndCalendar() {
        val vm = viewModel()
        val state = vm.state.value

        assertEquals("spot1", state.spot?.id)
        assertEquals(listOf("bea", "rafa", "paulo"), state.monthBookings.map { it.id })
        val cells = state.cells()
        fun cell(day: Int) = cells[day - 1]
        assertEquals(KbDayState.Booked, cell(7).state)
        assertEquals(KbDayState.Past, cell(6).state)
        assertEquals("HOJE", cell(8).mark)
        assertEquals("RES", cell(10).mark)
        assertEquals("RES", cell(11).mark)
        assertEquals("PED", cell(16).mark)
        assertEquals(KbDayState.Free, cell(20).state)
        assertEquals(KbDayState.Blocked, cell(25).state)
    }

    @Test
    fun monthNavigationStopsAtTheCurrentMonth() {
        val vm = viewModel()
        vm.onAction(AgendaAction.OnPreviousMonth)
        assertEquals(LocalDate(2026, 10, 1), vm.state.value.month)

        vm.onAction(AgendaAction.OnNextMonth)
        assertEquals(LocalDate(2026, 11, 1), vm.state.value.month)
        assertEquals(listOf("nov"), vm.state.value.monthBookings.map { it.id })
    }

    @Test
    fun spotErrorAndRetry() {
        val hosting = FakeHosting().apply { listError = DataError.Remote.NO_INTERNET }
        val vm = viewModel(hosting)
        assertNotNull(vm.state.value.error)

        hosting.spots += spot(id = "spot1", availability = availability)
        vm.onAction(AgendaAction.OnRetry)
        assertEquals("spot1", vm.state.value.spot?.id)
    }
}
