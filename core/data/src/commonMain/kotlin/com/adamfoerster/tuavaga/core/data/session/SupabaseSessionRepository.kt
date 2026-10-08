package com.adamfoerster.tuavaga.core.data.session

import com.adamfoerster.tuavaga.core.data.util.toRemoteError
import com.adamfoerster.tuavaga.core.database.user.UserDao
import com.adamfoerster.tuavaga.core.database.user.UserEntity
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.user.User
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal class SupabaseSessionRepository(
    private val auth: Auth,
    private val userDao: UserDao,
) : SessionRepository {

    override val sessionState: Flow<SessionState> = auth.sessionStatus
        .toSessionState(currentUser = auth::currentUserOrNull, observeUser = ::observeUser)

    /** Mirrors the Supabase user into Room and exposes the local copy. */
    private fun observeUser(info: UserInfo): Flow<SessionState> {
        val remote = info.toEntity()
        return userDao.observe(remote.id)
            .onStart { userDao.upsert(remote) }
            .map { local -> SessionState.SignedIn((local ?: remote).toUser()) }
    }

    override suspend fun signOut(): EmptyResult<DataError.Remote> {
        val result: EmptyResult<DataError.Remote> = try {
            auth.signOut()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Server-side revocation failed (e.g. offline); still forget the session locally.
            auth.clearSession()
            Result.Failure(e.toRemoteError())
        }
        userDao.clear()
        return result
    }
}

/**
 * Maps supabase-kt's session status to the app's [SessionState].
 *
 * [SessionState.Loading] is emitted only once, before the first real status. Later
 * [SessionStatus.Initializing] emissions are ignored: on Android supabase-kt sets it whenever the app
 * goes to the background (screen locked, app switched) and reloads the session on return. Reporting
 * Loading there made the root swap the whole UI and lose every screen's state (e.g. a half-filled form).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun Flow<SessionStatus>.toSessionState(
    currentUser: () -> UserInfo?,
    observeUser: (UserInfo) -> Flow<SessionState>,
): Flow<SessionState> = flatMapLatest { status ->
    when (status) {
        SessionStatus.Initializing -> emptyFlow()
        is SessionStatus.NotAuthenticated -> flowOf(SessionState.SignedOut)
        // Refresh failed (usually offline) but tokens are still stored: keep the user in.
        is SessionStatus.RefreshFailure -> currentUser()?.let(observeUser) ?: flowOf(SessionState.SignedOut)
        // A session without user info keeps whatever was known until the next status.
        is SessionStatus.Authenticated -> (status.session.user ?: currentUser())?.let(observeUser) ?: emptyFlow()
    }
}
    .onStart { emit(SessionState.Loading) }
    .distinctUntilChanged()

private fun UserInfo.toEntity() = UserEntity(
    id = id,
    email = email.orEmpty(),
    fullName = (userMetadata?.get("full_name") as? JsonPrimitive)?.contentOrNull,
)

private fun UserEntity.toUser() = User(id = id, email = email, fullName = fullName)
