package com.adamfoerster.tuavaga.core.data.supabase

import com.adamfoerster.tuavaga.core.data.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

internal object SupabaseClientFactory {

    fun create(sessionManager: SessionManager): SupabaseClient {
        check(AppConfig.SUPABASE_URL.isNotBlank() && AppConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "SUPABASE_URL / SUPABASE_ANON_KEY não configurados. Veja local.properties.example."
        }
        return createSupabaseClient(
            supabaseUrl = AppConfig.SUPABASE_URL,
            supabaseKey = AppConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth) {
                this.sessionManager = sessionManager
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
                autoSaveToStorage = true
            }
            install(Postgrest)
            install(Realtime)
        }
    }
}
