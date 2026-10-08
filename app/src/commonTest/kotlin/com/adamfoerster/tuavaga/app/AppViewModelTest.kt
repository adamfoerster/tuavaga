package com.adamfoerster.tuavaga.app

import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.util.DataError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private lateinit var session: FakeSession
    private lateinit var condos: FakeCondos

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        session = FakeSession()
        condos = FakeCondos()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(introSeen: Boolean = true) = AppViewModel(session, FakePreferences(introSeen), condos)

    @Test
    fun firstLaunchStartsWithTheIntro() {
        val vm = viewModel(introSeen = false)

        assertEquals(AppArea.SIGNED_OUT, vm.state.value.area())
        assertEquals(StartDestination.INTRO, vm.state.value.signedOutStart())
        assertEquals(0, condos.refreshCalls)
    }

    @Test
    fun signInWithoutCondominiumGoesToOnboarding() {
        val vm = viewModel()
        session.sessionState.value = signedIn()

        assertEquals(1, condos.refreshCalls)
        assertEquals(AppArea.ONBOARDING, vm.state.value.area())
    }

    @Test
    fun signInWithCondominiumOpensTheApp() {
        condos.serverMemberships = listOf(membership("c1"))
        val vm = viewModel()
        session.sessionState.value = signedIn()

        assertEquals(AppArea.MAIN, vm.state.value.area())
    }

    @Test
    fun failedFirstLoadShowsErrorAndRetryRecovers() {
        condos.refreshError = DataError.Remote.NO_INTERNET
        val vm = viewModel()
        session.sessionState.value = signedIn()
        assertEquals(AppArea.MEMBERSHIP_ERROR, vm.state.value.area())

        condos.refreshError = null
        condos.serverMemberships = listOf(membership("c1"))
        vm.retryMemberships()

        assertEquals(AppArea.MAIN, vm.state.value.area())
    }

    @Test
    fun cachedMembershipOpensTheAppOffline() {
        condos.memberships.value = listOf(membership("c1"))
        condos.refreshError = DataError.Remote.NO_INTERNET
        session.sessionState.value = signedIn()
        val vm = viewModel()

        assertEquals(AppArea.MAIN, vm.state.value.area())
    }

    @Test
    fun signOutReturnsToLogin() {
        condos.serverMemberships = listOf(membership("c1"))
        session.sessionState.value = signedIn()
        val vm = viewModel()

        vm.signOut()
        condos.memberships.value = emptyList()

        assertEquals(AppArea.SIGNED_OUT, vm.state.value.area())
        assertEquals(StartDestination.AUTH, vm.state.value.signedOutStart())
    }

    @Test
    fun sessionStillLoadingKeepsTheSplash() {
        session.sessionState.value = SessionState.Loading

        assertEquals(AppArea.LOADING, viewModel().state.value.area())
    }
}
