package com.adamfoerster.tuavaga.core.database.session

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * The Supabase auth session (access + refresh token) serialized as JSON.
 * Single row: there is only ever one signed-in session on the device.
 */
@Entity(tableName = "auth_session")
data class SessionEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val json: String,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
