package com.adamfoerster.tuavaga.core.database.condo

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MembershipDao {
    @Query("SELECT * FROM membership WHERE userId = :userId ORDER BY position")
    fun observe(userId: String): Flow<List<MembershipEntity>>

    @Query("DELETE FROM membership WHERE userId = :userId")
    suspend fun clear(userId: String)

    @Insert
    suspend fun insertAll(memberships: List<MembershipEntity>)

    /** Replaces the user's cache with the backend's current list. */
    @Transaction
    suspend fun replace(userId: String, memberships: List<MembershipEntity>) {
        clear(userId)
        insertAll(memberships)
    }
}
