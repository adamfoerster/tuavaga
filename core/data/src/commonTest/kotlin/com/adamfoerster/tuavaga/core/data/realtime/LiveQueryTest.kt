package com.adamfoerster.tuavaga.core.data.realtime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LiveQueryTest {

    @Test
    fun loadsWhenSubscribedAndOnEveryChange() = runTest {
        val events = MutableSharedFlow<Unit>()
        val realtime = object : RealtimeChanges {
            override fun changes(table: String, column: String?, value: String?): Flow<Unit> =
                events.onSubscription { emit(Unit) }
        }
        var loads = 0
        val results = mutableListOf<Int>()
        val job = launch { realtime.liveQuery("messages") { ++loads }.take(3).toList(results) }
        runCurrent()
        events.emit(Unit)
        events.emit(Unit)
        job.join()

        assertEquals(listOf(1, 2, 3), results)
    }

    @Test
    fun withoutRealtimeItStillLoadsAndRetries() = runTest {
        var subscriptions = 0
        val realtime = object : RealtimeChanges {
            override fun changes(table: String, column: String?, value: String?): Flow<Unit> = flow {
                subscriptions++
                error("socket blocked")
            }
        }
        var loads = 0
        val results = mutableListOf<Int>()
        val job = launch { realtime.liveQuery("notifications") { ++loads }.toList(results) }
        runCurrent()
        assertEquals(listOf(1), results)

        advanceTimeBy(2_001)
        assertEquals(listOf(1, 2), results)
        assertEquals(2, subscriptions)
        job.cancel()
    }
}
