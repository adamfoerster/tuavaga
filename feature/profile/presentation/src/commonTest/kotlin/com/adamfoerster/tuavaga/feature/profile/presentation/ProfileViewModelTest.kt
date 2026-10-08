package com.adamfoerster.tuavaga.feature.profile.presentation

import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.user.User
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private class FakeSession : SessionRepository {
        override val sessionState = MutableStateFlow<SessionState>(
            SessionState.SignedIn(User("u1", "adam@condominio.com", "Adam Foerster")),
        )
        var signOuts = 0

        override suspend fun signOut(): EmptyResult<DataError.Remote> {
            signOuts++
            return Result.Success(Unit)
        }
    }

    private lateinit var session: FakeSession
    private lateinit var viewModel: ProfileViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        session = FakeSession()
        viewModel = ProfileViewModel(session)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsTheSignedInUser() {
        assertEquals("Adam Foerster", viewModel.state.value.userName)
        assertEquals("adam@condominio.com", viewModel.state.value.userEmail)
    }

    @Test
    fun signOutRunsOnceEvenWithRepeatedTaps() {
        viewModel.onAction(ProfileAction.OnSignOutClick)
        viewModel.onAction(ProfileAction.OnSignOutClick)

        assertEquals(1, session.signOuts)
    }
}
