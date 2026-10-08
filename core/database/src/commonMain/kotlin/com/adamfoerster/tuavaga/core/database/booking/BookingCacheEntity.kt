package com.adamfoerster.tuavaga.core.database.booking

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Last list of bookings the backend returned for a user (renter and owner), kept as the JSON of the
 * `my_bookings` rows, so "Reservas" opens offline (board 18 "Suas reservas salvas continuam visíveis").
 */
@Entity(tableName = "booking_cache")
data class BookingCacheEntity(
    @PrimaryKey val userId: String,
    val json: String,
    /** Epoch millis of the refresh. */
    val updatedAt: Long,
)
