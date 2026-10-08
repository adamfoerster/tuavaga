package com.adamfoerster.tuavaga.feature.bookings.presentation.di

import com.adamfoerster.tuavaga.core.domain.time.appNow
import com.adamfoerster.tuavaga.feature.bookings.presentation.check.CheckKind
import com.adamfoerster.tuavaga.feature.bookings.presentation.check.CheckViewModel
import com.adamfoerster.tuavaga.feature.bookings.presentation.detail.BookingDetailViewModel
import com.adamfoerster.tuavaga.feature.bookings.presentation.list.BookingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val bookingsPresentationModule = module {
    viewModelOf(::BookingsViewModel)
    // Route arguments come via parametersOf (see BookingsNavigation).
    viewModel { (bookingId: String) -> BookingDetailViewModel(bookingId, get(), now = { appNow() }) }
    viewModel { (kind: CheckKind, bookingId: String) -> CheckViewModel(kind, bookingId, get(), now = { appNow() }) }
}
