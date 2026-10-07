package com.adamfoerster.tuavaga.core.database.session

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface SessionDao {
    @Query("SELECT * FROM auth_session WHERE id = ${SessionEntity.SINGLE_ROW_ID}")
    suspend fun get(): SessionEntity?

    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Query("DELETE FROM auth_session")
    suspend fun clear()
}
