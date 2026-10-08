package com.adamfoerster.tuavaga.feature.hosting.presentation.myspots

import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.feature.hosting.domain.Spot

/** The user's spots in one of their condominiums (empty list = "Nenhuma vaga sua neste condomínio"). */
data class CondoSpots(val condoId: String, val condoName: String, val spots: List<Spot>)

data class MySpotsState(
    val groups: List<CondoSpots> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiText? = null,
    /** Spot whose pause/reactivate request is running. */
    val updatingSpotId: String? = null,
)

sealed interface MySpotsAction {
    data object OnRefresh : MySpotsAction
    data class OnToggleStatus(val spotId: String) : MySpotsAction

    // Navigation, handled by the Root.
    /** `null` = the active condominium. */
    data class OnCreateSpot(val condoId: String?) : MySpotsAction
    data class OnEditSpot(val spotId: String) : MySpotsAction
    data object OnWantSpotClick : MySpotsAction
}
