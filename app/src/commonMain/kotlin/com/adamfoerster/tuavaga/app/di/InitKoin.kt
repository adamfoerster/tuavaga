package com.adamfoerster.tuavaga.app.di

import com.adamfoerster.tuavaga.app.AppViewModel
import com.adamfoerster.tuavaga.app.shell.ShellViewModel
import com.adamfoerster.tuavaga.core.data.di.coreDataModule
import com.adamfoerster.tuavaga.core.database.di.databaseModule
import com.adamfoerster.tuavaga.feature.auth.data.di.authDataModule
import com.adamfoerster.tuavaga.feature.auth.presentation.di.authPresentationModule
import com.adamfoerster.tuavaga.feature.explore.data.di.exploreDataModule
import com.adamfoerster.tuavaga.feature.explore.presentation.di.explorePresentationModule
import com.adamfoerster.tuavaga.feature.hosting.data.di.hostingDataModule
import com.adamfoerster.tuavaga.feature.hosting.presentation.di.hostingPresentationModule
import com.adamfoerster.tuavaga.feature.onboarding.presentation.di.onboardingPresentationModule
import com.adamfoerster.tuavaga.feature.profile.presentation.di.profilePresentationModule
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val appModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::ShellViewModel)
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
            profilePresentationModule,
            onboardingPresentationModule,
            exploreDataModule,
            explorePresentationModule,
            hostingDataModule,
            hostingPresentationModule,
        )
    }
}
