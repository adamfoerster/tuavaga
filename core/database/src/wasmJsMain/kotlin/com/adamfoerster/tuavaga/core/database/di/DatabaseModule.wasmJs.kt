package com.adamfoerster.tuavaga.core.database.di

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import com.adamfoerster.tuavaga.core.database.TuaVagaDatabase
import org.koin.dsl.module
import org.w3c.dom.Worker

internal actual val platformDatabaseModule = module {
    single {
        Room.databaseBuilder<TuaVagaDatabase>(name = TuaVagaDatabase.NAME)
            .setDriver(WebWorkerSQLiteDriver(createSqliteWorker()))
    }
}

// Webpack bundles the worker (and @sqlite.org/sqlite-wasm with it) because of the
// `new Worker(new URL(..., import.meta.url))` shape. See core/database/sqlite-worker.
private fun createSqliteWorker(): Worker =
    js("""new Worker(new URL("tuavaga-sqlite-worker/worker.js", import.meta.url), { type: "module" })""")
