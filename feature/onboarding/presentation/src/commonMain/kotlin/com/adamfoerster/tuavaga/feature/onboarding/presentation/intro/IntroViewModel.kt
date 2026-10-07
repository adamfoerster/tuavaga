package com.adamfoerster.tuavaga.feature.onboarding.presentation.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.prefs.AppPreferencesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** "Como funciona": shown once per device; continuing or skipping both mark it as seen. */
class IntroViewModel(
    private val preferences: AppPreferencesRepository,
) : ViewModel() {

    private val eventChannel = Channel<IntroEvent>()
    val events = eventChannel.receiveAsFlow()

    private var finishing = false

    fun onAction(action: IntroAction) {
        when (action) {
            IntroAction.OnContinueClick, IntroAction.OnSkipClick -> finish()
        }
    }

    private fun finish() {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            preferences.markIntroSeen()
            eventChannel.send(IntroEvent.Finished)
        }
    }
}
