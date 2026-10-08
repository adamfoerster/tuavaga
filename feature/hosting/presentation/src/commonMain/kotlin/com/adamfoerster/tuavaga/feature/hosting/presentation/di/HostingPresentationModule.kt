package com.adamfoerster.tuavaga.feature.hosting.presentation.di

import com.adamfoerster.tuavaga.feature.hosting.presentation.myspots.MySpotsViewModel
import com.adamfoerster.tuavaga.feature.hosting.presentation.wizard.SpotWizardViewModel
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.todayIn
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.time.Clock

/**
 * "Today" for availability follows Brasília time. A fixed UTC−3 offset (Brazil has had no daylight
 * saving since 2019) because named zones like "America/Sao_Paulo" need a timezone database that
 * kotlinx-datetime does not ship on wasmJs — `TimeZone.of` throws there.
 */
internal val APP_TIME_ZONE = FixedOffsetTimeZone(UtcOffset(hours = -3))

val hostingPresentationModule = module {
    viewModelOf(::MySpotsViewModel)
    // Route arguments come via parametersOf (see SpotWizardRoot).
    viewModel { (spotId: String?, condoId: String?) ->
        SpotWizardViewModel(spotId, condoId, get(), get(), today = { Clock.System.todayIn(APP_TIME_ZONE) })
    }
}
