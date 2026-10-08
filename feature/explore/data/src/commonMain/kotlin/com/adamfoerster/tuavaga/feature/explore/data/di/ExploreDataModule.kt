package com.adamfoerster.tuavaga.feature.explore.data.di

import com.adamfoerster.tuavaga.feature.explore.data.SupabaseExploreRepository
import com.adamfoerster.tuavaga.feature.explore.domain.ExploreRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val exploreDataModule = module {
    single { SupabaseExploreRepository(get()) } bind ExploreRepository::class
}
