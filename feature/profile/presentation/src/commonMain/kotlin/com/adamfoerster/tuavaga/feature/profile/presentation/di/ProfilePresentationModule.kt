package com.adamfoerster.tuavaga.feature.profile.presentation.di

import com.adamfoerster.tuavaga.feature.profile.presentation.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profilePresentationModule = module {
    viewModelOf(::ProfileViewModel)
}
