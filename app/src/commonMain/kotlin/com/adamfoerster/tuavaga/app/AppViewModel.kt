package com.adamfoerster.tuavaga.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.prefs.AppPreferencesRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MembershipsLoad { IDLE, LOADING, DONE, FAILED }

/** What the root needs to decide which part of the app to show. */
data class AppState(
    val session: SessionState = SessionState.Loading,
    val introSeen: Boolean = false,
    /** Local cache of the user's condominiums (filled by the refresh after sign-in). */
    val memberships: List<Membership> = emptyList(),
    val membershipsLoad: MembershipsLoad = MembershipsLoad.IDLE,
)

/** Top-level parts of the app; each has its own navigation stack. */
enum class AppArea { LOADING, SIGNED_OUT, MEMBERSHIP_ERROR, ONBOARDING, MAIN }

fun AppState.area(): AppArea = when {
    session == SessionState.Loading -> AppArea.LOADING
    session !is SessionState.SignedIn -> AppArea.SIGNED_OUT
    // A cached membership is enough to open the app (offline included).
    memberships.isNotEmpty() -> AppArea.MAIN
    membershipsLoad == MembershipsLoad.DONE -> AppArea.ONBOARDING
    membershipsLoad == MembershipsLoad.FAILED -> AppArea.MEMBERSHIP_ERROR
    else -> AppArea.LOADING
}

enum class StartDestination { INTRO, AUTH }

/** First screen of the signed-out area: the intro is shown once per device, then login. */
fun AppState.signedOutStart(): StartDestination =
    if (introSeen) StartDestination.AUTH else StartDestination.INTRO

class AppViewModel(
    private val sessionRepository: SessionRepository,
    preferences: AppPreferencesRepository,
    private val condoRepository: CondoRepository,
) : ViewModel() {

    private val membershipsLoad = MutableStateFlow(MembershipsLoad.IDLE)

    // combine waits for every source, so the state stays LOADING until the local data is read.
    val state: StateFlow<AppState> = combine(
        sessionRepository.sessionState,
        preferences.introSeen,
        condoRepository.memberships,
        membershipsLoad,
    ) { session, introSeen, memberships, load -> AppState(session, introSeen, memberships, load) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    init {
        // Every sign-in (or a different user) reloads the memberships from the backend.
        viewModelScope.launch {
            sessionRepository.sessionState
                .map { (it as? SessionState.SignedIn)?.user?.id }
                .distinctUntilChanged()
                .collectLatest { userId ->
                    if (userId == null) membershipsLoad.value = MembershipsLoad.IDLE else refreshMemberships()
                }
        }
    }

    /** "Tentar de novo", or after an onboarding that could not refresh by itself. */
    fun retryMemberships() {
        viewModelScope.launch { refreshMemberships() }
    }

    fun signOut() {
        viewModelScope.launch { sessionRepository.signOut() }
    }

    private suspend fun refreshMemberships() {
        membershipsLoad.value = MembershipsLoad.LOADING
        membershipsLoad.value = when (condoRepository.refreshMemberships()) {
            is Result.Success -> MembershipsLoad.DONE
            is Result.Failure -> MembershipsLoad.FAILED
        }
    }
}
