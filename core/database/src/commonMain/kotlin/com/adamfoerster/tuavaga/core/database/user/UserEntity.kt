package com.adamfoerster.tuavaga.core.database.user

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Local copy of the signed-in user's profile. */
@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String?,
)
