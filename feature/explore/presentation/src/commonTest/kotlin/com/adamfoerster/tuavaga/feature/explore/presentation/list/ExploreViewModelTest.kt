package com.adamfoerster.tuavaga.feature.explore.presentation.list

import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.spot.Prices
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.feature.explore.presentation.FakeActiveCondo
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
class ExploreViewModelTest {

    private lateinit var explore: FakeExplore
    private lateinit var active: FakeActiveCondo
    private lateinit var viewModel: ExploreViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        explore = FakeExplore().apply {
            listings["c1"] = listOf(
                listing("27", features = setOf(SpotFeature.COVERED, SpotFeature.ELECTRIC)),
                listing("28", features = setOf(SpotFeature.WIDE), prices = Prices(dayCents = 3500)),
                listing("29", available = false),
                listing("30", isMine = true),
                listing("04", levelId = "s1", levelName = "Subsolo 1", levelPosition = 1),
            )
            listings["c2"] = emptyList()
        }
        active = FakeActiveCondo("c1")
        viewModel = ExploreViewModel(explore, FakeCondos(membership("c1"), membership("c2")), active, now = { NOW })
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun searchesTheActiveCondoForTheNextTwoHours() {
        val (condoId, period) = explore.searches.last()
        assertEquals("c1", condoId)
        assertEquals(BookingPeriod(LocalDateTime(2026, 10, 8, 15, 0), LocalDateTime(2026, 10, 8, 17, 0)), period)
        assertEquals(listOf("27", "28", "04"), viewModel.state.value.visible.map { it.id })
    }

    @Test
    fun ownAndBusySpotsAreOnlyOnTheMap() {
        val state = viewModel.state.value
        assertEquals(listOf("Subsolo 1", "Subsolo 2"), state.levels.map { it.name })
        // The map starts on the first level by position.
        assertEquals(listOf("04"), state.mapSpots.map { it.id })
        viewModel.onAction(ExploreAction.OnLevelSelect("s2"))
        assertEquals(listOf("27", "28", "29", "30"), viewModel.state.value.mapSpots.map { it.id })
    }

    @Test
    fun filtersNarrowTheList() {
        viewModel.onAction(ExploreAction.OnFeatureToggle(SpotFeature.COVERED))
        assertEquals(listOf("27"), viewModel.state.value.visible.map { it.id })

        viewModel.onAction(ExploreAction.OnFeatureToggle(SpotFeature.COVERED))
        viewModel.onAction(ExploreAction.OnCheapToggle)
        // "Até R$ 10/h" hides spots without an hourly price.
        assertEquals(listOf("27", "04"), viewModel.state.value.visible.map { it.id })

        viewModel.onAction(ExploreAction.OnClearFilters)
        assertTrue(viewModel.state.value.filters.isEmpty)
    }

    @Test
    fun changingThePeriodSearchesAgain() {
        val weekend = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 11, 18, 0))
        viewModel.onAction(ExploreAction.OnPeriodClick)
        assertTrue(viewModel.state.value.isPeriodSheetOpen)

        viewModel.onAction(ExploreAction.OnPeriodApply(weekend))

        assertEquals(weekend, explore.searches.last().second)
        assertFalse(viewModel.state.value.isPeriodSheetOpen)
    }

    @Test
    fun switchingCondominiumSearchesItAndShowsTheFirstSpotState() {
        active.activeCondoId.value = "c2"

        assertEquals("c2", explore.searches.last().first)
        assertTrue(viewModel.state.value.isCondoEmpty)
    }

    @Test
    fun mapSelectionToggles() {
        viewModel.onAction(ExploreAction.OnMapSpotClick("04"))
        assertEquals("04", viewModel.state.value.selectedSpot?.id)
        viewModel.onAction(ExploreAction.OnMapSpotClick("04"))
        assertNull(viewModel.state.value.selectedSpot)
    }

    @Test
    fun errorAndRetry() {
        explore.searchError = DataError.Remote.NO_INTERNET
        viewModel.onAction(ExploreAction.OnRetry)
        assertNotNull(viewModel.state.value.error)

        explore.searchError = null
        viewModel.onAction(ExploreAction.OnRetry)
        assertNull(viewModel.state.value.error)
        assertEquals(3, viewModel.state.value.visible.size)
    }
}
