package com.adamfoerster.tuavaga.core.database.prefs

import androidx.room3.Dao
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppPrefsDao {
    @Query("SELECT * FROM app_prefs WHERE id = ${AppPrefsEntity.SINGLE_ROW_ID}")
    fun observe(): Flow<AppPrefsEntity?>

    @Query(
        "INSERT INTO app_prefs (id, introSeen) VALUES (${AppPrefsEntity.SINGLE_ROW_ID}, 1) " +
            "ON CONFLICT(id) DO UPDATE SET introSeen = 1",
    )
    suspend fun markIntroSeen()
}
