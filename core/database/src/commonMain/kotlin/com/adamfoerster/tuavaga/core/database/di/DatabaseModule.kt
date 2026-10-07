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
            // Everything stored locally is a cache of the backend; no migrations needed yet.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<TuaVagaDatabase>().sessionDao() }
    single { get<TuaVagaDatabase>().userDao() }
}
