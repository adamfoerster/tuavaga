package com.adamfoerster.tuavaga.feature.explore.domain

import com.adamfoerster.tuavaga.core.domain.spot.ApprovalMode
import com.adamfoerster.tuavaga.core.domain.spot.Availability
import com.adamfoerster.tuavaga.core.domain.spot.Prices
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import com.adamfoerster.tuavaga.core.domain.spot.TimeWindow
import com.adamfoerster.tuavaga.core.domain.spot.spotCode
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.Error
import com.adamfoerster.tuavaga.core.domain.util.Result
import kotlinx.datetime.DayOfWeek

/** An active spot of the condominium as seen by someone looking for a place, for one period. */
data class SpotListing(
    val id: String,
    val levelId: String,
    val levelName: String,
    val levelPosition: Int,
    val sectorName: String?,
    val number: String,
    val sizeLabel: String?,
    val description: String?,
    val features: Set<SpotFeature>,
    val heightCm: Int?,
    val directions: String?,
    val prices: Prices,
    val minPeriodMinutes: Int,
    val cancelNoticeHours: Int,
    val approval: ApprovalMode,
    val rules: List<String>,
    val ownerName: String?,
    val ownerBlock: String?,
    val ownerUnit: String?,
    /** The user's own spot (shown on the map, never bookable). */
    val isMine: Boolean,
    /** Free for the searched period (open, not booked, at least the minimum period). */
    val available: Boolean,
    val weekly: Map<DayOfWeek, TimeWindow>,
) {
    val code: String get() = spotCode(levelName, sectorName, number)

    val isBookable: Boolean get() = available && !isMine
}

/** "Marina Ribeiro Souza" → "Marina R." (the design never shows full surnames). */
fun shortName(fullName: String?): String? {
    val parts = fullName?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
    return when {
        parts.isEmpty() -> null
        parts.size == 1 -> parts[0]
        else -> "${parts.first()} ${parts.last().first().uppercaseChar()}."
    }
}

/** Filter chips of board 04. */
data class ExploreFilters(
    val features: Set<SpotFeature> = emptySet(),
    /** "Até R$ 10/h": hourly price at most this (spots without hourly price are hidden). */
    val maxHourCents: Int? = null,
) {
    val isEmpty: Boolean get() = features.isEmpty() && maxHourCents == null

    fun matches(spot: SpotListing): Boolean =
        spot.features.containsAll(features) &&
            (maxHourCents == null || spot.prices.hourCents.let { it != null && it <= maxHourCents })

    companion object {
        const val CHEAP_HOUR_CENTS = 1000
    }
}

enum class BookingStatus { PENDING, CONFIRMED }

/** What the backend answered to a booking request. */
data class BookingConfirmation(val id: String, val code: Long, val status: BookingStatus, val totalCents: Int)

data class BookingRequest(
    val spotId: String,
    val period: BookingPeriod,
    val quote: BookingQuote,
    val vehicleId: String?,
    val note: String?,
)

sealed interface BookingError : Error {
    /** Paused, booked or closed in the period (also when someone else confirmed first). */
    data object SpotUnavailable : BookingError
    data object BelowMinimum : BookingError
    data object UnitNotOffered : BookingError
    data object OwnSpot : BookingError
    data object InvalidVehicle : BookingError
    data object InvalidPeriod : BookingError
    data class Remote(val error: DataError.Remote) : BookingError
}

interface ExploreRepository {
    /** Active spots of [condoId] with their state for [period] (members only). */
    suspend fun search(condoId: String, period: BookingPeriod): Result<List<SpotListing>, DataError.Remote>

    /** Weekly rule and exceptions of a spot (detail calendar). */
    suspend fun availability(spotId: String): Result<Availability, DataError.Remote>

    /** Confirmed bookings of a spot overlapping [range], without who booked. */
    suspend fun busyPeriods(spotId: String, range: BookingPeriod): Result<List<BookingPeriod>, DataError.Remote>

    suspend fun requestBooking(request: BookingRequest): Result<BookingConfirmation, BookingError>
}
