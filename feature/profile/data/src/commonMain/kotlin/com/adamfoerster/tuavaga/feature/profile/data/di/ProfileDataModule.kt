package com.adamfoerster.tuavaga.feature.profile.data.di

import com.adamfoerster.tuavaga.feature.profile.data.SupabaseAccountRepository
import com.adamfoerster.tuavaga.feature.profile.domain.AccountRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val profileDataModule = module {
    single { SupabaseAccountRepository(get()) } bind AccountRepository::class
}
