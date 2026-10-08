package com.adamfoerster.tuavaga.core.data.booking

import com.adamfoerster.tuavaga.core.data.condo.signedInUserId
import com.adamfoerster.tuavaga.core.data.util.remoteCall
import com.adamfoerster.tuavaga.core.data.util.toDbTimestamp
import com.adamfoerster.tuavaga.core.data.util.toRemoteError
import com.adamfoerster.tuavaga.core.database.booking.BookingCacheDao
import com.adamfoerster.tuavaga.core.database.booking.BookingCacheEntity
import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.booking.RejectReason
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

internal class SupabaseBookingRepository(
    private val postgrest: Postgrest,
    sessionRepository: SessionRepository,
    private val cacheDao: BookingCacheDao,
    private val clock: Clock = Clock.System,
) : BookingRepository {

    private val userId: Flow<String?> = sessionRepository.sessionState.signedInUserId()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val bookings: Flow<List<Booking>> = userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else cacheDao.observe(id).map { it?.json?.let(::decodeBookings).orEmpty() }
    }

    override suspend fun refresh(): EmptyResult<DataError.Remote> {
        val uid = userId.first() ?: return Result.Failure(DataError.Remote.UNAUTHORIZED)
        return remoteCall {
            val rows = postgrest.rpc("my_bookings").decodeList<BookingRowDto>()
            cacheDao.upsert(BookingCacheEntity(uid, cacheJson.encodeToString(rows), clock.now().toEpochMilliseconds()))
        }
    }

    override suspend fun approve(bookingId: String) = action("approve_booking", buildJsonObject { put("p_booking", bookingId) })

    override suspend fun reject(bookingId: String, reason: RejectReason, message: String?) = action(
        "reject_booking",
        buildJsonObject {
            put("p_booking", bookingId)
            put("p_reason", reason.key)
            put("p_message", message?.trim()?.takeIf { it.isNotEmpty() })
        },
    )

    override suspend fun cancel(bookingId: String) = action("cancel_booking", buildJsonObject { put("p_booking", bookingId) })

    override suspend fun checkIn(bookingId: String) = action("check_in", buildJsonObject { put("p_booking", bookingId) })

    override suspend fun checkOut(bookingId: String) = action("check_out", buildJsonObject { put("p_booking", bookingId) })

    override suspend fun extend(bookingId: String, newEnd: LocalDateTime) = action(
        "extend_booking",
        buildJsonObject {
            put("p_booking", bookingId)
            put("p_new_end", newEnd.toDbTimestamp())
        },
    )

    /** Runs a lifecycle RPC and, when it succeeds, reloads the cache (a failed reload is not an error). */
    private suspend fun action(function: String, params: JsonObject): EmptyResult<BookingActionError> = try {
        postgrest.rpc(function, params)
        refresh()
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        val known = (e as? PostgrestRestException)?.let { bookingActionErrorFromMessage(it.message) }
        Result.Failure(known ?: BookingActionError.Remote(e.toRemoteError()))
    }
}

private val cacheJson = Json { ignoreUnknownKeys = true }

/** The cached rows; an unreadable cache (older app version) counts as empty until the next refresh. */
internal fun decodeBookings(json: String): List<Booking> =
    runCatching { cacheJson.decodeFromString<List<BookingRowDto>>(json) }.getOrDefault(emptyList()).map { it.toBooking() }
