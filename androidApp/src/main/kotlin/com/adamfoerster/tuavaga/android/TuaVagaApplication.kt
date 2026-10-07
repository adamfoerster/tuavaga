package com.adamfoerster.tuavaga.android

import android.app.Application
import com.adamfoerster.tuavaga.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class TuaVagaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TuaVagaApplication)
        }
    }
}
