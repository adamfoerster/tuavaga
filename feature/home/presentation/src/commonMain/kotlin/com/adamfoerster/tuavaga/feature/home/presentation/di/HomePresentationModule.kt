package com.adamfoerster.tuavaga.feature.home.presentation.di

import com.adamfoerster.tuavaga.feature.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homePresentationModule = module {
    viewModelOf(::HomeViewModel)
}
