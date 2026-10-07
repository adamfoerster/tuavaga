package com.adamfoerster.tuavaga.core.data.di

import com.adamfoerster.tuavaga.core.data.session.SupabaseSessionRepository
import com.adamfoerster.tuavaga.core.data.supabase.RoomSessionManager
import com.adamfoerster.tuavaga.core.data.supabase.SupabaseClientFactory
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
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
}
