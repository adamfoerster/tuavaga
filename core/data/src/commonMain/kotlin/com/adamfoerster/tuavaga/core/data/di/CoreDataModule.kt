package com.adamfoerster.tuavaga.core.data.di

import com.adamfoerster.tuavaga.core.data.condo.SupabaseCondoRepository
import com.adamfoerster.tuavaga.core.data.prefs.RoomActiveCondoRepository
import com.adamfoerster.tuavaga.core.data.prefs.RoomAppPreferencesRepository
import com.adamfoerster.tuavaga.core.data.session.SupabaseSessionRepository
import com.adamfoerster.tuavaga.core.data.supabase.RoomSessionManager
import com.adamfoerster.tuavaga.core.data.supabase.SupabaseClientFactory
import com.adamfoerster.tuavaga.core.data.vehicle.SupabaseVehicleRepository
import com.adamfoerster.tuavaga.core.domain.condo.ActiveCondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.prefs.AppPreferencesRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import org.koin.dsl.bind
import org.koin.dsl.module

val coreDataModule = module {
    single<SessionManager> { RoomSessionManager(get()) }
    single<SupabaseClient> { SupabaseClientFactory.create(get()) }
    single<Auth> { get<SupabaseClient>().auth }
    single<Postgrest> { get<SupabaseClient>().postgrest }
    single { SupabaseSessionRepository(get(), get()) } bind SessionRepository::class
    single { RoomAppPreferencesRepository(get()) } bind AppPreferencesRepository::class
    single { RoomActiveCondoRepository(get()) } bind ActiveCondoRepository::class
    single { SupabaseCondoRepository(get(), get(), get()) } bind CondoRepository::class
    single { SupabaseVehicleRepository(get()) } bind VehicleRepository::class
}
