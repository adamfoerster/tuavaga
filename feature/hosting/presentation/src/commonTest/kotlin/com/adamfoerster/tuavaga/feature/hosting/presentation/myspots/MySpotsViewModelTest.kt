package com.adamfoerster.tuavaga.feature.hosting.presentation.myspots

import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotStatus
import com.adamfoerster.tuavaga.feature.hosting.presentation.FakeBookings
import com.adamfoerster.tuavaga.feature.hosting.presentation.FakeCondos
import com.adamfoerster.tuavaga.feature.hosting.presentation.FakeHosting
import com.adamfoerster.tuavaga.feature.hosting.presentation.membership
import com.adamfoerster.tuavaga.feature.hosting.presentation.ownerBooking
import com.adamfoerster.tuavaga.feature.hosting.presentation.spot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MySpotsViewModelTest {

    private lateinit var hosting: FakeHosting
    private lateinit var bookings: FakeBookings
    private lateinit var viewModel: MySpotsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        hosting = FakeHosting(listOf(spot(id = "a", condoId = "c1"), spot(id = "b", condoId = "c1", status = SpotStatus.PAUSED)))
        val condos = FakeCondos(membership("c1", "Residencial Alameda Verde"), membership("c2", "Edifício Santa Clara"))
        bookings = FakeBookings(
            ownerBooking("b1"),
            ownerBooking("b2", status = BookingStatus.COMPLETED, spotId = "a", start = LocalDateTime(2026, 10, 2, 8, 0), end = LocalDateTime(2026, 10, 2, 18, 0)),
            ownerBooking("x1", status = BookingStatus.CANCELLED),
            ownerBooking("x2", start = LocalDateTime(2026, 11, 2, 8, 0), end = LocalDateTime(2026, 11, 2, 18, 0)),
            ownerBooking("r1", role = BookingRole.RENTER),
            ownerBooking("p1", status = BookingStatus.PENDING, spotId = "a"),
        )
        viewModel = MySpotsViewModel(hosting, condos, bookings, today = { LocalDate(2026, 10, 8) })
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun spotsAreGroupedByCondominiumIncludingEmptyOnes() {
        viewModel.onAction(MySpotsAction.OnRefresh)

        val groups = viewModel.state.value.groups
        assertEquals(listOf("Residencial Alameda Verde", "Edifício Santa Clara"), groups.map { it.condoName })
        assertEquals(2, groups[0].spots.size)
        assertTrue(groups[1].spots.isEmpty())
    }

    @Test
    fun pauseAndReactivate() {
        viewModel.onAction(MySpotsAction.OnRefresh)
        viewModel.onAction(MySpotsAction.OnToggleStatus("a"))
        viewModel.onAction(MySpotsAction.OnToggleStatus("b"))

        assertEquals(listOf("a" to SpotStatus.PAUSED, "b" to SpotStatus.ACTIVE), hosting.statusChanges)
        val spots = viewModel.state.value.groups[0].spots
        assertEquals(listOf(SpotStatus.PAUSED, SpotStatus.ACTIVE), spots.map { it.status })
    }

    @Test
    fun loadErrorIsShown() {
        hosting.listError = DataError.Remote.NO_INTERNET
        viewModel.onAction(MySpotsAction.OnRefresh)

        assertNotNull(viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun monthEarningsAndRequestsComeFromTheBookings() {
        viewModel.onAction(MySpotsAction.OnRefresh)
        val state = viewModel.state.value

        // b1 + b2 (October, confirmed or completed); cancelled, November and renter bookings do not count.
        assertEquals(14000, state.monthEarningsCents)
        assertEquals(2, state.monthBookingCount)
        assertEquals(1, state.pendingCount)
        assertEquals(1, state.pendingFor("a"))
        assertEquals(0, state.pendingFor("b"))
        assertEquals(1, bookings.refreshes)
        assertEquals("140", reais(state.monthEarningsCents))
        assertEquals("70,50", reais(7050))
    }

    @Test
    fun metaLineLooksLikeTheDesign() {
        assertEquals("SUBSOLO 2 · SETOR B · R$ 8/H", spot().metaLine())
    }
}
