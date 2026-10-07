package com.adamfoerster.tuavaga.core.data.prefs

import com.adamfoerster.tuavaga.core.database.prefs.AppPrefsDao
import com.adamfoerster.tuavaga.core.domain.prefs.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class RoomAppPreferencesRepository(
    private val dao: AppPrefsDao,
) : AppPreferencesRepository {

    override val introSeen: Flow<Boolean> = dao.observe()
        .map { it?.introSeen ?: false }
        .distinctUntilChanged()

    override suspend fun markIntroSeen() = dao.markIntroSeen()
}
