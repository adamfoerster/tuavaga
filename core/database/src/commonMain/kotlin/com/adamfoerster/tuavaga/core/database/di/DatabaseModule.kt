package com.adamfoerster.tuavaga.core.database.di

import androidx.room3.RoomDatabase
import com.adamfoerster.tuavaga.core.database.TuaVagaDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/** Provides a `RoomDatabase.Builder<TuaVagaDatabase>` with the platform's file location and driver. */
internal expect val platformDatabaseModule: Module

val databaseModule = module {
    includes(platformDatabaseModule)
    single {
        get<RoomDatabase.Builder<TuaVagaDatabase>>()
            // Schema changes ship with AutoMigrations (keeps the saved session); the destructive
            // fallback only covers a missing migration path, since everything else is a backend cache.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<TuaVagaDatabase>().sessionDao() }
    single { get<TuaVagaDatabase>().userDao() }
    single { get<TuaVagaDatabase>().appPrefsDao() }
    single { get<TuaVagaDatabase>().membershipDao() }
    single { get<TuaVagaDatabase>().bookingCacheDao() }
}
