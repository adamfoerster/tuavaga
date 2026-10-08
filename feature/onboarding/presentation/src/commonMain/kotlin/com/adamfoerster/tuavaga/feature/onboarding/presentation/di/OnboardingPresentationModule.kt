package com.adamfoerster.tuavaga.feature.onboarding.presentation.di

import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoOnboardingSession
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.create.CreateCondoViewModel
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.join.JoinCondoViewModel
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.resident.ResidentDataViewModel
import com.adamfoerster.tuavaga.feature.onboarding.presentation.intro.IntroViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingPresentationModule = module {
    singleOf(::CondoOnboardingSession)
    viewModelOf(::IntroViewModel)
    viewModelOf(::JoinCondoViewModel)
    viewModelOf(::CreateCondoViewModel)
    viewModelOf(::ResidentDataViewModel)
}
