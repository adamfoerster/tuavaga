package com.adamfoerster.tuavaga.feature.hosting.presentation.di

import com.adamfoerster.tuavaga.core.domain.time.appToday
import com.adamfoerster.tuavaga.feature.hosting.presentation.myspots.MySpotsViewModel
import com.adamfoerster.tuavaga.feature.hosting.presentation.wizard.SpotWizardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val hostingPresentationModule = module {
    viewModelOf(::MySpotsViewModel)
    // Route arguments come via parametersOf (see SpotWizardRoot).
    viewModel { (spotId: String?, condoId: String?) ->
        SpotWizardViewModel(spotId, condoId, get(), get(), today = { appToday() })
    }
}
