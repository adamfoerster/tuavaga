package com.adamfoerster.tuavaga.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        sessionRepository.sessionState
            .filterIsInstance<SessionState.SignedIn>()
            .onEach { session ->
                _state.update { it.copy(userName = session.user.fullName, userEmail = session.user.email) }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnSignOutClick -> signOut()
        }
    }

    private fun signOut() {
        if (_state.value.isSigningOut) return
        viewModelScope.launch {
            _state.update { it.copy(isSigningOut = true) }
            // The local session is always cleared, so the app root moves to the login screen
            // even when the server could not be reached; nothing else to handle here.
            sessionRepository.signOut()
        }
    }
}
