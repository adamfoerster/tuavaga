package com.adamfoerster.tuavaga.core.database.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.adamfoerster.tuavaga.core.database.TuaVagaDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal actual val platformDatabaseModule = module {
    single {
        val context = androidContext()
        Room.databaseBuilder<TuaVagaDatabase>(
            context = context,
            name = context.getDatabasePath(TuaVagaDatabase.NAME).absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
    }
}
