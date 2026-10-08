package com.adamfoerster.tuavaga.feature.explore.presentation.di

import com.adamfoerster.tuavaga.core.domain.time.appNow
import com.adamfoerster.tuavaga.core.domain.time.appToday
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.presentation.detail.SpotDetailViewModel
import com.adamfoerster.tuavaga.feature.explore.presentation.list.ExploreViewModel
import com.adamfoerster.tuavaga.feature.explore.presentation.request.BookingRequestViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val explorePresentationModule = module {
    viewModel { ExploreViewModel(get(), get(), get(), now = { appNow() }) }
    // Route arguments come via parametersOf (see ExploreNavigation).
    viewModel { (condoId: String, spotId: String, period: BookingPeriod) ->
        SpotDetailViewModel(condoId, spotId, period, get(), get(), today = { appToday() })
    }
    viewModel { (condoId: String, spotId: String, period: BookingPeriod) ->
        BookingRequestViewModel(condoId, spotId, period, get(), get(), get(), now = { appNow() })
    }
}
