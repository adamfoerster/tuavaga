package com.adamfoerster.tuavaga.feature.hosting.data

import com.adamfoerster.tuavaga.feature.hosting.domain.ApprovalMode
import com.adamfoerster.tuavaga.feature.hosting.domain.Availability
import com.adamfoerster.tuavaga.feature.hosting.domain.DayOverride
import com.adamfoerster.tuavaga.feature.hosting.domain.Prices
import com.adamfoerster.tuavaga.feature.hosting.domain.Spot
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotDraft
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotFormats
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotStatus
import com.adamfoerster.tuavaga.feature.hosting.domain.TimeWindow
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Columns selected for a spot, with level, sector and availability embedded. */
internal const val SPOT_COLUMNS = "id,condo_id,level_id,sector_id,number,size_label,description," +
    "price_hour_cents,price_day_cents,price_week_cents,min_period_minutes,cancel_notice_hours,approval,rules,status," +
    "condo_levels(name),condo_sectors(name)," +
    "spot_weekly_availability(weekday,start_time,end_time),spot_date_overrides(day,kind,start_time,end_time)"

@Serializable
internal data class NameDto(val name: String)

@Serializable
internal data class WeeklyDto(
    val weekday: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
)

@Serializable
internal data class OverrideDto(
    val day: String,
    val kind: String,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
)

@Serializable
internal data class SpotDto(
    val id: String,
    @SerialName("condo_id") val condoId: String,
    @SerialName("level_id") val levelId: String,
    @SerialName("sector_id") val sectorId: String? = null,
    val number: String,
    @SerialName("size_label") val sizeLabel: String? = null,
    val description: String? = null,
    @SerialName("price_hour_cents") val priceHourCents: Int? = null,
    @SerialName("price_day_cents") val priceDayCents: Int? = null,
    @SerialName("price_week_cents") val priceWeekCents: Int? = null,
    @SerialName("min_period_minutes") val minPeriodMinutes: Int,
    @SerialName("cancel_notice_hours") val cancelNoticeHours: Int,
    val approval: String,
    val rules: List<String> = emptyList(),
    val status: String,
    @SerialName("condo_levels") val level: NameDto,
    @SerialName("condo_sectors") val sector: NameDto? = null,
    @SerialName("spot_weekly_availability") val weekly: List<WeeklyDto> = emptyList(),
    @SerialName("spot_date_overrides") val overrides: List<OverrideDto> = emptyList(),
)

/** Postgres `time` comes as "08:00:00"; "24:00:00" is the end of the day. */
internal fun parseDbTime(value: String): Int = SpotFormats.parseTime(value.take(5))
    ?: error("Unexpected time from the database: $value")

internal fun SpotDto.toSpot() = Spot(
    id = id,
    condoId = condoId,
    levelId = levelId,
    levelName = level.name,
    sectorId = sectorId,
    sectorName = sector?.name,
    number = number,
    sizeLabel = sizeLabel,
    description = description,
    prices = Prices(priceHourCents, priceDayCents, priceWeekCents),
    minPeriodMinutes = minPeriodMinutes,
    cancelNoticeHours = cancelNoticeHours,
    approval = if (approval == "auto") ApprovalMode.AUTO else ApprovalMode.MANUAL,
    rules = rules,
    status = if (status == "paused") SpotStatus.PAUSED else SpotStatus.ACTIVE,
    availability = Availability(
        weekly = weekly.associate { DayOfWeek(it.weekday) to TimeWindow(parseDbTime(it.startTime), parseDbTime(it.endTime)) },
        overrides = overrides.associate { o ->
            LocalDate.parse(o.day) to if (o.kind == "open") {
                DayOverride.Open(TimeWindow(parseDbTime(o.startTime!!), parseDbTime(o.endTime!!)))
            } else {
                DayOverride.Blocked
            }
        },
    ),
)

internal fun ApprovalMode.toDb() = if (this == ApprovalMode.AUTO) "auto" else "manual"

internal fun SpotStatus.toDb() = if (this == SpotStatus.PAUSED) "paused" else "active"

internal fun Availability.weeklyJson(): JsonArray = buildJsonArray {
    weekly.entries.sortedBy { it.key.isoDayNumber }.forEach { (day, window) ->
        add(
            buildJsonObject {
                put("weekday", day.isoDayNumber)
                put("start", SpotFormats.formatTime(window.startMinutes))
                put("end", SpotFormats.formatTime(window.endMinutes))
            },
        )
    }
}

internal fun Availability.overridesJson(): JsonArray = buildJsonArray {
    overrides.entries.sortedBy { it.key }.forEach { (day, override) ->
        add(
            buildJsonObject {
                put("day", day.toString())
                when (override) {
                    DayOverride.Blocked -> put("kind", "blocked")
                    is DayOverride.Open -> {
                        put("kind", "open")
                        put("start", SpotFormats.formatTime(override.window.startMinutes))
                        put("end", SpotFormats.formatTime(override.window.endMinutes))
                    }
                }
            },
        )
    }
}

/** Parameters of `save_spot` (supabase/migrations/20261009000000_spots.sql). */
internal fun SpotDraft.toSaveParams(): JsonObject = buildJsonObject {
    put("p_spot_id", id)
    put("p_condo", condoId)
    put("p_level", levelId)
    put("p_sector", sectorId)
    put("p_number", number.trim())
    put("p_size_label", sizeLabel?.trim())
    put("p_description", description?.trim())
    put("p_price_hour_cents", prices.hourCents)
    put("p_price_day_cents", prices.dayCents)
    put("p_price_week_cents", prices.weekCents)
    put("p_min_period_minutes", minPeriodMinutes)
    put("p_cancel_notice_hours", cancelNoticeHours)
    put("p_approval", approval.toDb())
    putJsonArray("p_rules") { rules.forEach { add(it) } }
    put("p_weekly", availability.weeklyJson())
    put("p_overrides", availability.overridesJson())
}
