package com.adamfoerster.tuavaga.feature.notifications.presentation.di

import com.adamfoerster.tuavaga.core.domain.time.appToday
import com.adamfoerster.tuavaga.feature.notifications.presentation.NotificationsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val notificationsPresentationModule = module {
    viewModel { NotificationsViewModel(get(), get(), today = { appToday() }) }
}
