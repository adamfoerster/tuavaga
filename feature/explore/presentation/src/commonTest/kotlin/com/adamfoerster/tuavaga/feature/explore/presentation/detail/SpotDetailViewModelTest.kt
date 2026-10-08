package com.adamfoerster.tuavaga.feature.explore.presentation.detail

import com.adamfoerster.tuavaga.core.designsystem.components.KbDayState
import com.adamfoerster.tuavaga.core.domain.spot.Availability
import com.adamfoerster.tuavaga.core.domain.spot.TimeWindow
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.presentation.FakeCondos
import com.adamfoerster.tuavaga.feature.explore.presentation.FakeExplore
import com.adamfoerster.tuavaga.feature.explore.presentation.NOW
import com.adamfoerster.tuavaga.feature.explore.presentation.listing
import com.adamfoerster.tuavaga.feature.explore.presentation.membership
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SpotDetailViewModelTest {

    private val weekend = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 11, 18, 0))
    private lateinit var explore: FakeExplore

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        explore = FakeExplore().apply { listings["c1"] = listOf(listing("27")) }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(spotId: String = "27") =
        SpotDetailViewModel("c1", spotId, weekend, explore, FakeCondos(membership("c1", "Alameda Verde")), today = { NOW.date })

    @Test
    fun loadsTheSpotWithTheQuoteForThePeriod() {
        val state = viewModel().state.value

        assertEquals("Alameda Verde", state.condoName)
        assertEquals("27", state.spot?.id)
        // 34 h → 2 diárias × R$ 35.
        assertEquals(7000, state.quote?.totalCents)
        assertEquals(LocalDate(2026, 10, 1), state.month)
        assertEquals(monthRange(LocalDate(2026, 10, 1)), explore.busyRanges.single())
    }

    @Test
    fun spotNoLongerListedShowsAnError() {
        val state = viewModel(spotId = "99").state.value

        assertNull(state.spot)
        assertEquals(DetailTexts.notFound, state.error)
    }

    @Test
    fun networkErrorAndRetry() {
        explore.searchError = DataError.Remote.NO_INTERNET
        val vm = viewModel()
        assertNotNull(vm.state.value.error)

        explore.searchError = null
        vm.onAction(SpotDetailAction.OnRetry)
        assertNull(vm.state.value.error)
        assertEquals("27", vm.state.value.spot?.id)
    }

    @Test
    fun monthNavigationStopsAtTheCurrentMonth() {
        val vm = viewModel()
        vm.onAction(SpotDetailAction.OnPreviousMonth)
        assertEquals(LocalDate(2026, 10, 1), vm.state.value.month)

        vm.onAction(SpotDetailAction.OnNextMonth)
        assertEquals(LocalDate(2026, 11, 1), vm.state.value.month)
        assertEquals(monthRange(LocalDate(2026, 11, 1)), explore.busyRanges.last())
    }

    @Test
    fun calendarMarksPeriodBookingsBlocksAndPast() {
        val today = LocalDate(2026, 10, 8)
        val availability = Availability()
            .withWeekly(DayOfWeek.entries.toSet(), TimeWindow(8 * 60, 18 * 60))
            .block(listOf(LocalDate(2026, 10, 20)))
        val busy = listOf(BookingPeriod(LocalDateTime(2026, 10, 14, 9, 0), LocalDateTime(2026, 10, 15, 9, 0)))

        val cells = detailCalendarCells(LocalDate(2026, 10, 1), today, availability, busy, weekend)

        assertEquals(31, cells.size)
        fun stateOf(day: Int) = cells[day - 1].state
        assertEquals(KbDayState.Past, stateOf(7))
        assertEquals("HOJE", cells[7].mark)
        assertEquals(KbDayState.Free, stateOf(8))
        assertEquals(KbDayState.Selected, stateOf(10))
        assertEquals(KbDayState.Selected, stateOf(11))
        assertEquals(KbDayState.Booked, stateOf(14))
        assertEquals(KbDayState.Booked, stateOf(15))
        assertEquals(KbDayState.Blocked, stateOf(20))
        assertEquals("20 de outubro, bloqueado", cells[19].description)
    }
}
