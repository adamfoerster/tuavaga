package com.adamfoerster.tuavaga.core.data.realtime

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Supabase Realtime "postgres changes" of one table. The server applies the table's RLS, so a
 * subscriber only hears about rows it can read.
 */
interface RealtimeChanges {
    /**
     * Emits once when the channel is subscribed and then once per insert/update/delete of [table]
     * (optionally only rows where [column] = [value]). Callers reload on every emission, so nothing
     * between their first load and the subscription is lost. Throws when Realtime is unavailable.
     */
    fun changes(table: String, column: String? = null, value: String? = null): Flow<Unit>
}

internal class SupabaseRealtimeChanges(private val client: SupabaseClient) : RealtimeChanges {

    override fun changes(table: String, column: String?, value: String?): Flow<Unit> = flow {
        val channel = client.channel("$table-${value ?: "all"}-${Random.nextLong()}")
        // Must be declared before subscribing.
        val actions = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            this.table = table
            if (column != null && value != null) filter(column, FilterOperator.EQ, value)
        }
        try {
            channel.subscribe(blockUntilSubscribed = true)
            emit(Unit)
            emitAll(actions.map { })
        } finally {
            withContext(NonCancellable) { client.realtime.removeChannel(channel) }
        }
    }
}

/**
 * A query kept current by Realtime: runs [load] when subscribed and after every change of [table].
 * When Realtime is unavailable (offline, blocked socket) it still loads, then retries the
 * subscription with a growing pause (2 s → 60 s), loading again on each attempt.
 */
fun <T> RealtimeChanges.liveQuery(
    table: String,
    column: String? = null,
    value: String? = null,
    load: suspend () -> T,
): Flow<T> = flow {
    var attempt = 0
    while (true) {
        try {
            changes(table, column, value).collect {
                attempt = 0
                emit(load())
            }
            return@flow
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            emit(load())
            delay((2_000L shl attempt.coerceAtMost(5)).coerceAtMost(60_000L))
            attempt++
        }
    }
}
