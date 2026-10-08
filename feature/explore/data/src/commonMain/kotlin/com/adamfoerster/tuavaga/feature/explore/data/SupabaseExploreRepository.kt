package com.adamfoerster.tuavaga.feature.explore.data

import com.adamfoerster.tuavaga.core.data.util.remoteCall
import com.adamfoerster.tuavaga.core.data.util.toRemoteError
import com.adamfoerster.tuavaga.core.domain.spot.Availability
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.explore.domain.BookingConfirmation
import com.adamfoerster.tuavaga.feature.explore.domain.BookingError
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.domain.BookingRequest
import com.adamfoerster.tuavaga.feature.explore.domain.ExploreRepository
import com.adamfoerster.tuavaga.feature.explore.domain.SpotListing
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class SupabaseExploreRepository(
    private val postgrest: Postgrest,
) : ExploreRepository {

    override suspend fun search(condoId: String, period: BookingPeriod): Result<List<SpotListing>, DataError.Remote> =
        remoteCall {
            postgrest.rpc(
                "search_spots",
                buildJsonObject {
                    put("p_condo", condoId)
                    put("p_start", period.start.toDbTimestamp())
                    put("p_end", period.end.toDbTimestamp())
                },
            ).decodeList<ListingDto>().map { it.toListing() }
        }

    override suspend fun availability(spotId: String): Result<Availability, DataError.Remote> = remoteCall {
        val weekly = postgrest.from("spot_weekly_availability")
            .select { filter { eq("spot_id", spotId) } }
            .decodeList<WeeklyRowDto>()
        val overrides = postgrest.from("spot_date_overrides")
            .select { filter { eq("spot_id", spotId) } }
            .decodeList<OverrideRowDto>()
        availabilityOf(weekly, overrides)
    }

    override suspend fun busyPeriods(spotId: String, range: BookingPeriod): Result<List<BookingPeriod>, DataError.Remote> =
        remoteCall {
            postgrest.rpc(
                "spot_busy_ranges",
                buildJsonObject {
                    put("p_spot", spotId)
                    put("p_from", range.start.toDbTimestamp())
                    put("p_to", range.end.toDbTimestamp())
                },
            ).decodeList<RangeDto>().map { it.toPeriod() }
        }

    override suspend fun requestBooking(request: BookingRequest): Result<BookingConfirmation, BookingError> = try {
        val rows = postgrest.rpc(
            "request_booking",
            buildJsonObject {
                put("p_spot", request.spotId)
                put("p_start", request.period.start.toDbTimestamp())
                put("p_end", request.period.end.toDbTimestamp())
                put("p_unit", request.quote.unit.toDb())
                put("p_vehicle", request.vehicleId)
                put("p_note", request.note)
            },
        ).decodeList<BookingResultDto>()
        Result.Success(rows.single().toConfirmation())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        val known = (e as? PostgrestRestException)?.let { bookingErrorFromMessage(it.message) }
        Result.Failure(known ?: BookingError.Remote(e.toRemoteError()))
    }
}
