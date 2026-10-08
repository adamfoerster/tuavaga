package com.adamfoerster.tuavaga.feature.hosting.data.di

import com.adamfoerster.tuavaga.feature.hosting.data.SupabaseHostingRepository
import com.adamfoerster.tuavaga.feature.hosting.domain.HostingRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val hostingDataModule = module {
    single { SupabaseHostingRepository(get(), get()) } bind HostingRepository::class
}
