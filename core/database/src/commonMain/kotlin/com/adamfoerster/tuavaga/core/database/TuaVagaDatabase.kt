package com.adamfoerster.tuavaga.core.database

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.adamfoerster.tuavaga.core.database.prefs.AppPrefsDao
import com.adamfoerster.tuavaga.core.database.prefs.AppPrefsEntity
import com.adamfoerster.tuavaga.core.database.session.SessionDao
import com.adamfoerster.tuavaga.core.database.session.SessionEntity
import com.adamfoerster.tuavaga.core.database.user.UserDao
import com.adamfoerster.tuavaga.core.database.user.UserEntity

@Database(
    entities = [SessionEntity::class, UserEntity::class, AppPrefsEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ConstructedBy(TuaVagaDatabaseConstructor::class)
abstract class TuaVagaDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun userDao(): UserDao
    abstract fun appPrefsDao(): AppPrefsDao

    companion object {
        const val NAME = "tuavaga.db"
    }
}

@Suppress("KotlinNoActualForExpect")
expect object TuaVagaDatabaseConstructor : RoomDatabaseConstructor<TuaVagaDatabase> {
    override fun initialize(): TuaVagaDatabase
}
