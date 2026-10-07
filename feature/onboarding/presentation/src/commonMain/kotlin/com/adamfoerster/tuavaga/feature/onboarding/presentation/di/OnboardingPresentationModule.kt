package com.adamfoerster.tuavaga.feature.onboarding.presentation.di

import com.adamfoerster.tuavaga.feature.onboarding.presentation.intro.IntroViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingPresentationModule = module {
    viewModelOf(::IntroViewModel)
}
