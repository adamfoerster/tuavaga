package com.adamfoerster.tuavaga.feature.hosting.presentation.di

import com.adamfoerster.tuavaga.core.domain.time.appToday
import com.adamfoerster.tuavaga.feature.hosting.presentation.agenda.AgendaViewModel
import com.adamfoerster.tuavaga.feature.hosting.presentation.myspots.MySpotsViewModel
import com.adamfoerster.tuavaga.feature.hosting.presentation.requests.RequestsViewModel
import com.adamfoerster.tuavaga.feature.hosting.presentation.wizard.SpotWizardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val hostingPresentationModule = module {
    viewModel { MySpotsViewModel(get(), get(), get(), today = { appToday() }) }
    viewModelOf(::RequestsViewModel)
    // Route arguments come via parametersOf (see SpotWizardRoot and AgendaRoot).
    viewModel { (spotId: String?, condoId: String?) ->
        SpotWizardViewModel(spotId, condoId, get(), get(), today = { appToday() })
    }
    viewModel { (spotId: String) -> AgendaViewModel(spotId, get(), get(), today = { appToday() }) }
}
