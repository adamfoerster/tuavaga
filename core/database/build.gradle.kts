plugins {
    alias(libs.plugins.tuavaga.kmp.room)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
            // SQLite engine for the web worker in core/database/sqlite-worker (wired via app/webpack.config.d).
            implementation(npm("@sqlite.org/sqlite-wasm", libs.versions.sqlite.wasm.get()))
        }
    }
}
