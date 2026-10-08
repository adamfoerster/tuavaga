package com.adamfoerster.tuavaga.feature.hosting.presentation.myspots

import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotStatus
import com.adamfoerster.tuavaga.feature.hosting.presentation.FakeCondos
import com.adamfoerster.tuavaga.feature.hosting.presentation.FakeHosting
import com.adamfoerster.tuavaga.feature.hosting.presentation.membership
import com.adamfoerster.tuavaga.feature.hosting.presentation.spot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MySpotsViewModelTest {

    private lateinit var hosting: FakeHosting
    private lateinit var viewModel: MySpotsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        hosting = FakeHosting(listOf(spot(id = "a", condoId = "c1"), spot(id = "b", condoId = "c1", status = SpotStatus.PAUSED)))
        val condos = FakeCondos(membership("c1", "Residencial Alameda Verde"), membership("c2", "Edifício Santa Clara"))
        viewModel = MySpotsViewModel(hosting, condos)
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
    fun metaLineLooksLikeTheDesign() {
        assertEquals("SUBSOLO 2 · SETOR B · R$ 8/H", spot().metaLine())
    }
}
