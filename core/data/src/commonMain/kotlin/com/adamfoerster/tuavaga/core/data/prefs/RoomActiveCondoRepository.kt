package com.adamfoerster.tuavaga.core.data.prefs

import com.adamfoerster.tuavaga.core.database.prefs.AppPrefsDao
import com.adamfoerster.tuavaga.core.domain.condo.ActiveCondoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class RoomActiveCondoRepository(
    private val dao: AppPrefsDao,
) : ActiveCondoRepository {

    override val activeCondoId: Flow<String?> = dao.observe()
        .map { it?.activeCondoId }
        .distinctUntilChanged()

    override suspend fun setActiveCondo(condoId: String) = dao.setActiveCondo(condoId)
}
