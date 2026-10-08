package com.adamfoerster.tuavaga.feature.explore.data

import com.adamfoerster.tuavaga.core.data.util.fromDbTimestamp
import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.spot.ApprovalMode
import com.adamfoerster.tuavaga.core.domain.spot.Availability
import com.adamfoerster.tuavaga.core.domain.spot.DayOverride
import com.adamfoerster.tuavaga.core.domain.spot.Prices
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import com.adamfoerster.tuavaga.core.domain.spot.SpotFormats
import com.adamfoerster.tuavaga.core.domain.spot.TimeWindow
import com.adamfoerster.tuavaga.feature.explore.domain.BookingConfirmation
import com.adamfoerster.tuavaga.feature.explore.domain.BookingError
import com.adamfoerster.tuavaga.feature.explore.domain.SpotListing
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row of `search_spots` (supabase/migrations/20261010000100_bookings.sql). */
@Serializable
internal data class ListingDto(
    val id: String,
    @SerialName("level_id") val levelId: String,
    @SerialName("level_name") val levelName: String,
    @SerialName("level_position") val levelPosition: Int,
    @SerialName("sector_name") val sectorName: String? = null,
    val number: String,
    @SerialName("size_label") val sizeLabel: String? = null,
    val description: String? = null,
    val features: List<String> = emptyList(),
    @SerialName("height_cm") val heightCm: Int? = null,
    val directions: String? = null,
    @SerialName("price_hour_cents") val priceHourCents: Int? = null,
    @SerialName("price_day_cents") val priceDayCents: Int? = null,
    @SerialName("price_week_cents") val priceWeekCents: Int? = null,
    @SerialName("min_period_minutes") val minPeriodMinutes: Int,
    @SerialName("cancel_notice_hours") val cancelNoticeHours: Int,
    val approval: String,
    val rules: List<String> = emptyList(),
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("owner_block") val ownerBlock: String? = null,
    @SerialName("owner_unit") val ownerUnit: String? = null,
    @SerialName("is_mine") val isMine: Boolean,
    val available: Boolean,
    val weekly: List<WindowDto> = emptyList(),
)

/** Weekly window as `search_spots` aggregates it ("HH:MM"). */
@Serializable
internal data class WindowDto(val weekday: Int, val start: String, val end: String)

@Serializable
internal data class WeeklyRowDto(
    val weekday: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
)

@Serializable
internal data class OverrideRowDto(
    val day: String,
    val kind: String,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
)

@Serializable
internal data class RangeDto(
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
)

@Serializable
internal data class BookingResultDto(
    val id: String,
    val code: Long,
    val status: String,
    @SerialName("total_cents") val totalCents: Int,
)

/** "08:00" or Postgres "08:00:00"; "24:00" is the end of the day. */
internal fun parseTime(value: String): Int =
    SpotFormats.parseTime(value.take(5)) ?: error("Unexpected time from the database: $value")

internal fun ListingDto.toListing() = SpotListing(
    id = id,
    levelId = levelId,
    levelName = levelName,
    levelPosition = levelPosition,
    sectorName = sectorName,
    number = number,
    sizeLabel = sizeLabel,
    description = description,
    features = features.mapNotNull { SpotFeature.fromKey(it) }.toSet(),
    heightCm = heightCm,
    directions = directions,
    prices = Prices(priceHourCents, priceDayCents, priceWeekCents),
    minPeriodMinutes = minPeriodMinutes,
    cancelNoticeHours = cancelNoticeHours,
    approval = if (approval == "auto") ApprovalMode.AUTO else ApprovalMode.MANUAL,
    rules = rules,
    ownerName = ownerName,
    ownerBlock = ownerBlock,
    ownerUnit = ownerUnit,
    isMine = isMine,
    available = available,
    weekly = weekly.associate { DayOfWeek(it.weekday) to TimeWindow(parseTime(it.start), parseTime(it.end)) },
)

internal fun availabilityOf(weekly: List<WeeklyRowDto>, overrides: List<OverrideRowDto>) = Availability(
    weekly = weekly.associate { DayOfWeek(it.weekday) to TimeWindow(parseTime(it.startTime), parseTime(it.endTime)) },
    overrides = overrides.associate { o ->
        LocalDate.parse(o.day) to if (o.kind == "open") {
            DayOverride.Open(TimeWindow(parseTime(o.startTime!!), parseTime(o.endTime!!)))
        } else {
            DayOverride.Blocked
        }
    },
)

internal fun RangeDto.toPeriod() = BookingPeriod(
    start = fromDbTimestamp(startsAt),
    end = fromDbTimestamp(endsAt),
)

internal fun BookingResultDto.toConfirmation() = BookingConfirmation(
    id = id,
    code = code,
    status = if (status == "confirmed") BookingStatus.CONFIRMED else BookingStatus.PENDING,
    totalCents = totalCents,
)

/** request_booking raises stable messages (see the migration); anything else is a remote error. */
internal fun bookingErrorFromMessage(message: String?): BookingError? = when {
    message == null -> null
    "spot_unavailable" in message -> BookingError.SpotUnavailable
    "below_minimum" in message -> BookingError.BelowMinimum
    "unit_not_offered" in message -> BookingError.UnitNotOffered
    "own_spot" in message -> BookingError.OwnSpot
    "invalid_vehicle" in message -> BookingError.InvalidVehicle
    "invalid_period" in message -> BookingError.InvalidPeriod
    else -> null
}
