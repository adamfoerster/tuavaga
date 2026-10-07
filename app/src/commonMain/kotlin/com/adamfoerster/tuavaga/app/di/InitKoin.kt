package com.adamfoerster.tuavaga.app.di

import com.adamfoerster.tuavaga.app.AppViewModel
import com.adamfoerster.tuavaga.core.data.di.coreDataModule
import com.adamfoerster.tuavaga.core.database.di.databaseModule
import com.adamfoerster.tuavaga.feature.auth.data.di.authDataModule
import com.adamfoerster.tuavaga.feature.auth.presentation.di.authPresentationModule
import com.adamfoerster.tuavaga.feature.home.presentation.di.homePresentationModule
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val appModule = module {
    viewModelOf(::AppViewModel)
}

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            appModule,
            databaseModule,
            coreDataModule,
            authDataModule,
            authPresentationModule,
            homePresentationModule,
        )
    }
}
