package com.adamfoerster.tuavaga.core.data.session

import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.user.User
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SessionStateMappingTest {

    private val user = UserInfo(aud = "authenticated", id = "u1", email = "adam@condominio.com")
    private val signedIn = SessionState.SignedIn(User("u1", "adam@condominio.com", null))

    private fun authenticated(withUser: Boolean = true) = SessionStatus.Authenticated(
        UserSession(accessToken = "a", refreshToken = "r", expiresIn = 3600, tokenType = "bearer", user = if (withUser) user else null),
    )

    /** Collects everything the mapping emits while [statuses] are pushed in order. */
    private fun mapped(vararg statuses: SessionStatus, currentUser: UserInfo? = user): List<SessionState> {
        val emitted = mutableListOf<SessionState>()
        runTest(UnconfinedTestDispatcher()) {
            val source = MutableStateFlow<SessionStatus>(SessionStatus.Initializing)
            val job = launch {
                source.toSessionState(
                    currentUser = { currentUser },
                    observeUser = { info -> flowOf(SessionState.SignedIn(User(info.id, info.email.orEmpty(), null))) },
                ).collect { emitted += it }
            }
            statuses.forEach { source.value = it }
            job.cancel()
        }
        return emitted
    }

    @Test
    fun startupIsLoadingThenSignedIn() {
        assertEquals(listOf(SessionState.Loading, signedIn), mapped(authenticated()))
    }

    @Test
    fun goingToBackgroundAndBackKeepsTheUserSignedIn() {
        // Android: supabase-kt sets Initializing on onStop (screen locked) and reloads on onStart.
        val states = mapped(authenticated(), SessionStatus.Initializing, authenticated())

        assertEquals(listOf(SessionState.Loading, signedIn), states)
    }

    @Test
    fun signOutAfterBackgroundStillSignsOut() {
        val states = mapped(authenticated(), SessionStatus.Initializing, SessionStatus.NotAuthenticated(isSignOut = true))

        assertEquals(listOf(SessionState.Loading, signedIn, SessionState.SignedOut), states)
    }

    @Test
    fun sessionWithoutUserInfoUsesTheCurrentUser() {
        assertEquals(listOf(SessionState.Loading, signedIn), mapped(authenticated(withUser = false)))
    }

    @Test
    fun failedRefreshWithStoredUserStaysSignedIn() {
        val states = mapped(authenticated(), SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(Exception("offline"))))

        assertEquals(listOf(SessionState.Loading, signedIn), states)
    }

    @Test
    fun failedRefreshWithoutUserSignsOut() {
        val states = mapped(
            SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(Exception("offline"))),
            currentUser = null,
        )

        assertEquals(listOf(SessionState.Loading, SessionState.SignedOut), states)
    }
}
