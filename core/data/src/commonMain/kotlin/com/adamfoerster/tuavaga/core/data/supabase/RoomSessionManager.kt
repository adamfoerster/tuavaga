package com.adamfoerster.tuavaga.core.data.supabase

import com.adamfoerster.tuavaga.core.database.session.SessionDao
import com.adamfoerster.tuavaga.core.database.session.SessionEntity
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

/** Persists the Supabase session (tokens) in Room instead of the default key-value storage. */
internal class RoomSessionManager(
    private val sessionDao: SessionDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SessionManager {

    override suspend fun saveSession(session: UserSession) {
        sessionDao.upsert(SessionEntity(json = json.encodeToString(UserSession.serializer(), session)))
    }

    override suspend fun loadSession(): UserSession {
        val entity = sessionDao.get() ?: throw NoSessionFoundException()
        return json.decodeFromString(UserSession.serializer(), entity.json)
    }

    override suspend fun deleteSession() {
        sessionDao.clear()
    }
}
