package com.adamfoerster.tuavaga.feature.hosting.data

import com.adamfoerster.tuavaga.feature.hosting.domain.ApprovalMode
import com.adamfoerster.tuavaga.feature.hosting.domain.DayOverride
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotDraft
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotStatus
import com.adamfoerster.tuavaga.feature.hosting.domain.TimeWindow
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class SpotMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val row = """
        {"id":"s1","condo_id":"c1","level_id":"l2","sector_id":"sB","number":"27","size_label":"2,5 × 5,0",
         "description":"Perto do elevador","price_hour_cents":800,"price_day_cents":3500,"price_week_cents":null,
         "min_period_minutes":120,"cancel_notice_hours":24,"approval":"auto","rules":["Sem caminhonete"],"status":"paused",
         "condo_levels":{"name":"Subsolo 2"},"condo_sectors":{"name":"B"},
         "spot_weekly_availability":[{"weekday":1,"start_time":"08:00:00","end_time":"18:00:00"},
                                     {"weekday":7,"start_time":"00:00:00","end_time":"24:00:00"}],
         "spot_date_overrides":[{"day":"2026-10-12","kind":"blocked","start_time":null,"end_time":null},
                                {"day":"2026-10-17","kind":"open","start_time":"09:00:00","end_time":"12:00:00"}]}
    """

    @Test
    fun rowWithEmbeddedTablesBecomesASpot() {
        val spot = json.decodeFromString<SpotDto>(row).toSpot()

        assertEquals("B2-27", spot.code)
        assertEquals(800, spot.prices.hourCents)
        assertEquals(null, spot.prices.weekCents)
        assertEquals(ApprovalMode.AUTO, spot.approval)
        assertEquals(SpotStatus.PAUSED, spot.status)
        assertEquals(TimeWindow(480, 1080), spot.availability.weekly[DayOfWeek.MONDAY])
        assertEquals(TimeWindow(0, 1440), spot.availability.weekly[DayOfWeek.SUNDAY])
        assertEquals(DayOverride.Blocked, spot.availability.overrides[LocalDate(2026, 10, 12)])
        assertEquals(DayOverride.Open(TimeWindow(540, 720)), spot.availability.overrides[LocalDate(2026, 10, 17)])
    }

    @Test
    fun draftBecomesTheRpcParameters() {
        val spot = json.decodeFromString<SpotDto>(row).toSpot()
        val draft = SpotDraft(
            id = null, condoId = spot.condoId, levelId = spot.levelId, sectorId = null, number = " 27 ",
            sizeLabel = null, description = "Coberta", prices = spot.prices, minPeriodMinutes = 60,
            cancelNoticeHours = 2, approval = ApprovalMode.MANUAL, rules = spot.rules, availability = spot.availability,
        )

        val params = draft.toSaveParams()

        assertEquals(JsonNull, params["p_spot_id"])
        assertEquals(JsonNull, params["p_sector"])
        assertEquals("27", params["p_number"]!!.jsonPrimitive.content)
        assertEquals("manual", params["p_approval"]!!.jsonPrimitive.content)
        val weekly = params["p_weekly"]!!.jsonArray
        assertEquals(1, weekly[0].jsonObject["weekday"]!!.jsonPrimitive.int)
        assertEquals("24:00", weekly[1].jsonObject["end"]!!.jsonPrimitive.content)
        val overrides = params["p_overrides"]!!.jsonArray
        assertEquals("blocked", overrides[0].jsonObject["kind"]!!.jsonPrimitive.content)
        assertEquals("09:00", overrides[1].jsonObject["start"]!!.jsonPrimitive.content)
    }
}
