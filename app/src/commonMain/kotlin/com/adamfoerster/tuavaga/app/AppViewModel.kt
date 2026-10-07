package com.adamfoerster.tuavaga.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.prefs.AppPreferencesRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What the root needs to pick the first screen and react to sign-out. */
data class AppState(
    val session: SessionState = SessionState.Loading,
    val introSeen: Boolean = false,
) {
    val isLoading: Boolean get() = session == SessionState.Loading
    val isSignedIn: Boolean get() = session is SessionState.SignedIn
}

enum class StartDestination { INTRO, AUTH, HOME }

/** Signed-in users go home; otherwise the intro is shown once, then login. */
fun AppState.startDestination(): StartDestination = when {
    isSignedIn -> StartDestination.HOME
    !introSeen -> StartDestination.INTRO
    else -> StartDestination.AUTH
}

class AppViewModel(
    sessionRepository: SessionRepository,
    preferences: AppPreferencesRepository,
) : ViewModel() {

    // combine waits for both sources, so the state stays "loading" until the preference is read.
    val state: StateFlow<AppState> = combine(
        sessionRepository.sessionState,
        preferences.introSeen,
    ) { session, introSeen -> AppState(session, introSeen) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState())
}
