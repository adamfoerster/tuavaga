package com.adamfoerster.tuavaga.app.shell

import com.adamfoerster.tuavaga.app.FakeActiveCondo
import com.adamfoerster.tuavaga.app.FakeCondos
import com.adamfoerster.tuavaga.app.membership
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelTest {

    private lateinit var condos: FakeCondos
    private lateinit var active: FakeActiveCondo
    private lateinit var viewModel: ShellViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        condos = FakeCondos().apply { memberships.value = listOf(membership("c1"), membership("c2")) }
        active = FakeActiveCondo()
        viewModel = ShellViewModel(condos, active)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun firstCondominiumIsActiveByDefault() {
        assertEquals("c1", viewModel.state.value.active?.condo?.id)
        assertEquals(MainTab.EXPLORE, viewModel.state.value.tab)
    }

    @Test
    fun choosingInTheSwitcherChangesTheActiveCondominium() {
        viewModel.onAction(ShellAction.OnOpenSwitcher)
        assertTrue(viewModel.state.value.isSwitcherOpen)

        viewModel.onAction(ShellAction.OnCondoSelect("c2"))

        assertEquals("c2", active.activeCondoId.value)
        assertEquals("c2", viewModel.state.value.active?.condo?.id)
        assertFalse(viewModel.state.value.isSwitcherOpen)
    }

    @Test
    fun leftCondominiumFallsBackToTheFirst() {
        active.activeCondoId.value = "c2"
        condos.memberships.value = listOf(membership("c1"))

        assertEquals("c1", viewModel.state.value.active?.condo?.id)
    }

    @Test
    fun addingACondominiumClosesTheSheet() {
        viewModel.onAction(ShellAction.OnOpenSwitcher)
        viewModel.onAction(ShellAction.OnAddCondoClick)

        assertFalse(viewModel.state.value.isSwitcherOpen)
    }

    @Test
    fun tabsSwitch() {
        viewModel.onAction(ShellAction.OnTabSelect(MainTab.PROFILE))

        assertEquals(MainTab.PROFILE, viewModel.state.value.tab)
    }

    @Test
    fun placeLineDescribesTheMembership() {
        assertEquals("Bloco B · Unidade 142 · Morador", membership("c1").placeLine())
    }
}
