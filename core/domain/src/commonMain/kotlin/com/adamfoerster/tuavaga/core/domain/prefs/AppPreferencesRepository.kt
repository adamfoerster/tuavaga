package com.adamfoerster.tuavaga.core.domain.prefs

import kotlinx.coroutines.flow.Flow

/** Preferences kept only on this device (not synced to the backend). */
interface AppPreferencesRepository {
    /** Whether the "Como funciona" introduction was already shown. */
    val introSeen: Flow<Boolean>

    suspend fun markIntroSeen()
}
