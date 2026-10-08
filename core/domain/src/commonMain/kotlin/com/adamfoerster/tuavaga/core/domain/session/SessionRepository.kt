package com.adamfoerster.tuavaga.core.domain.session

import com.adamfoerster.tuavaga.core.domain.user.User
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import kotlinx.coroutines.flow.Flow

/**
 * App-wide view of who is signed in. Any feature can observe it or sign out;
 * signing in lives in the auth feature.
 */
interface SessionRepository {
    val sessionState: Flow<SessionState>

    /** Always ends the local session; the error only reports a failed server-side revocation. */
    suspend fun signOut(): EmptyResult<DataError.Remote>
}

sealed interface SessionState {
    /**
     * Only while the stored session is read at startup. Never emitted again afterwards (e.g. when the
     * app goes to the background), so the UI is not torn down and screens keep their state.
     */
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: User) : SessionState
}
