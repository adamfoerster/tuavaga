package com.adamfoerster.tuavaga.feature.notifications.data.di

import com.adamfoerster.tuavaga.feature.notifications.data.SupabaseNotificationsRepository
import com.adamfoerster.tuavaga.feature.notifications.domain.NotificationsRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val notificationsDataModule = module {
    single { SupabaseNotificationsRepository(get(), get(), get(), get()) } bind NotificationsRepository::class
}
