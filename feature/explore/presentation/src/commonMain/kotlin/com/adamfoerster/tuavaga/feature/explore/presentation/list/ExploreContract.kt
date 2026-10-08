package com.adamfoerster.tuavaga.feature.explore.presentation.list

import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.domain.ExploreFilters
import com.adamfoerster.tuavaga.feature.explore.domain.SpotListing

enum class ExploreMode { LIST, MAP }

data class GarageLevelChoice(val id: String, val name: String)

data class ExploreState(
    val condo: Membership? = null,
    val period: BookingPeriod,
    val mode: ExploreMode = ExploreMode.LIST,
    val filters: ExploreFilters = ExploreFilters(),
    /** Every active spot of the condominium, with its state for [period]. */
    val listings: List<SpotListing> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val isPeriodSheetOpen: Boolean = false,
    val selectedLevelId: String? = null,
    val selectedSpotId: String? = null,
) {
    /** List mode: free spots of other residents matching the filters. */
    val visible: List<SpotListing> get() = listings.filter { it.isBookable && filters.matches(it) }

    val levels: List<GarageLevelChoice>
        get() = listings.sortedBy { it.levelPosition }.map { GarageLevelChoice(it.levelId, it.levelName) }.distinct()

    val mapLevelId: String? get() = selectedLevelId?.takeIf { id -> levels.any { it.id == id } } ?: levels.firstOrNull()?.id

    val mapSpots: List<SpotListing> get() = listings.filter { it.levelId == mapLevelId }

    val selectedSpot: SpotListing? get() = mapSpots.firstOrNull { it.id == selectedSpotId }

    /** Nobody listed a spot here yet: the "Seja o primeiro" state. */
    val isCondoEmpty: Boolean get() = !isLoading && error == null && listings.none { !it.isMine }
}

sealed interface ExploreAction {
    data object OnPeriodClick : ExploreAction
    data class OnPeriodApply(val period: BookingPeriod) : ExploreAction
    data object OnPeriodDismiss : ExploreAction
    data class OnModeSelect(val mode: ExploreMode) : ExploreAction
    data class OnFeatureToggle(val feature: SpotFeature) : ExploreAction
    data object OnCheapToggle : ExploreAction
    data object OnClearFilters : ExploreAction
    data class OnLevelSelect(val levelId: String) : ExploreAction
    data class OnMapSpotClick(val spotId: String) : ExploreAction
    data object OnRetry : ExploreAction

    // Navigation, handled by the Root.
    data class OnOpenSpot(val spotId: String) : ExploreAction
    data object OnListSpotClick : ExploreAction
}
