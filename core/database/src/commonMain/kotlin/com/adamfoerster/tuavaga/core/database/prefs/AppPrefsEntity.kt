package com.adamfoerster.tuavaga.core.database.prefs

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Device-local app preferences. Single row, created on first write. */
@Entity(tableName = "app_prefs")
data class AppPrefsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(defaultValue = "0") val introSeen: Boolean = false,
    /** Condominium shown below the selector; may point to one the user left (then the first is used). */
    val activeCondoId: String? = null,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
