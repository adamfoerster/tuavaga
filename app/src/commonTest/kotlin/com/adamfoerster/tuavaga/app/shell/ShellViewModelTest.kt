package com.adamfoerster.tuavaga.app.shell

import com.adamfoerster.tuavaga.app.FakeActiveCondo
import com.adamfoerster.tuavaga.app.FakeCondos
import com.adamfoerster.tuavaga.app.FakeMessages
import com.adamfoerster.tuavaga.app.FakeNotifications
import com.adamfoerster.tuavaga.app.conversation
import com.adamfoerster.tuavaga.app.membership
import com.adamfoerster.tuavaga.app.notification
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.Result
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
    private lateinit var notifications: FakeNotifications
    private lateinit var messages: FakeMessages
    private lateinit var viewModel: ShellViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        condos = FakeCondos().apply { memberships.value = listOf(membership("c1"), membership("c2")) }
        active = FakeActiveCondo()
        notifications = FakeNotifications()
        messages = FakeMessages()
        viewModel = ShellViewModel(condos, active, notifications, messages)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun unreadCountsForTheBellTheSwitcherAndTheMessagesTab() {
        notifications.notifications.value = Result.Success(
            listOf(notification("n1", "c1"), notification("n2", "c2"), notification("n3", "c2"), notification("n4", "c1", read = true)),
        )
        messages.conversations.value = Result.Success(listOf(conversation("b1", unread = 2), conversation("b2", unread = 1)))

        val state = viewModel.state.value
        assertEquals(3, state.unreadNotifications)
        assertEquals(mapOf("c1" to 1, "c2" to 2), state.unreadByCondo)
        assertEquals(3, state.unreadMessages)

        // A failed reload keeps the last counts.
        notifications.notifications.value = Result.Failure(DataError.Remote.NO_INTERNET)
        assertEquals(3, viewModel.state.value.unreadNotifications)
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
