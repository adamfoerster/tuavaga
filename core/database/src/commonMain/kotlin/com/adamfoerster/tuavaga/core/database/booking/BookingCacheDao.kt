package com.adamfoerster.tuavaga.core.database.booking

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingCacheDao {
    @Query("SELECT * FROM booking_cache WHERE userId = :userId")
    fun observe(userId: String): Flow<BookingCacheEntity?>

    @Upsert
    suspend fun upsert(cache: BookingCacheEntity)
}
