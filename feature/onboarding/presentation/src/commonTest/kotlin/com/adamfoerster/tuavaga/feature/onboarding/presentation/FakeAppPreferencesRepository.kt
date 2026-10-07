package com.adamfoerster.tuavaga.feature.onboarding.presentation

import com.adamfoerster.tuavaga.core.domain.prefs.AppPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAppPreferencesRepository(introSeen: Boolean = false) : AppPreferencesRepository {
    override val introSeen = MutableStateFlow(introSeen)
    var markIntroSeenCalls = 0
        private set

    override suspend fun markIntroSeen() {
        markIntroSeenCalls++
        introSeen.value = true
    }
}
