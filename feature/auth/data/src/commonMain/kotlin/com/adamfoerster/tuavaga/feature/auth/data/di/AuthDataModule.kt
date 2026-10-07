package com.adamfoerster.tuavaga.feature.auth.data.di

import com.adamfoerster.tuavaga.feature.auth.data.SupabaseAuthRepository
import com.adamfoerster.tuavaga.feature.auth.domain.AuthRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val authDataModule = module {
    single { SupabaseAuthRepository(get()) } bind AuthRepository::class
}
