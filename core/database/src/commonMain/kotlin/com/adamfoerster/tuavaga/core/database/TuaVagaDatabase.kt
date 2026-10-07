package com.adamfoerster.tuavaga.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.adamfoerster.tuavaga.core.database.session.SessionDao
import com.adamfoerster.tuavaga.core.database.session.SessionEntity
import com.adamfoerster.tuavaga.core.database.user.UserDao
import com.adamfoerster.tuavaga.core.database.user.UserEntity

@Database(
    entities = [SessionEntity::class, UserEntity::class],
    version = 1,
)
@ConstructedBy(TuaVagaDatabaseConstructor::class)
abstract class TuaVagaDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "tuavaga.db"
    }
}

@Suppress("KotlinNoActualForExpect")
expect object TuaVagaDatabaseConstructor : RoomDatabaseConstructor<TuaVagaDatabase> {
    override fun initialize(): TuaVagaDatabase
}
