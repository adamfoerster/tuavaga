package com.adamfoerster.tuavaga.feature.explore.domain

import com.adamfoerster.tuavaga.core.domain.spot.ApprovalMode
import com.adamfoerster.tuavaga.core.domain.spot.Prices
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExploreFiltersTest {

    @Test
    fun filtersNeedEveryFeatureAndCheapHours() {
        fun listing(features: Set<SpotFeature>, prices: Prices) = SpotListing(
            id = "s", levelId = "l", levelName = "Subsolo 2", levelPosition = 1, sectorName = "B", number = "27",
            sizeLabel = null, description = null, features = features, heightCm = null, directions = null,
            prices = prices, minPeriodMinutes = 60, cancelNoticeHours = 24, approval = ApprovalMode.AUTO, rules = emptyList(),
            ownerName = null, ownerBlock = null, ownerUnit = null, isMine = false, available = true, weekly = emptyMap(),
        )
        val coveredCheap = listing(setOf(SpotFeature.COVERED, SpotFeature.ELECTRIC), Prices(hourCents = 800))
        val coveredDaily = listing(setOf(SpotFeature.COVERED), Prices(dayCents = 3500))

        val covered = ExploreFilters(features = setOf(SpotFeature.COVERED))
        assertTrue(covered.matches(coveredCheap) && covered.matches(coveredDaily))
        val cheap = covered.copy(maxHourCents = ExploreFilters.CHEAP_HOUR_CENTS)
        assertTrue(cheap.matches(coveredCheap))
        assertFalse(cheap.matches(coveredDaily))
        assertFalse(ExploreFilters(features = setOf(SpotFeature.WIDE)).matches(coveredCheap))
        assertEquals("B2-27", coveredCheap.code)
    }
}
