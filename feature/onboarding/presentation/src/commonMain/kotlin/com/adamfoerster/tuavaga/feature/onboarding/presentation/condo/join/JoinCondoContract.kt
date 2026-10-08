package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.join

import com.adamfoerster.tuavaga.core.domain.condo.CondoPreview
import com.adamfoerster.tuavaga.core.presentation.UiText

data class JoinCondoState(
    val query: String = "",
    val results: List<CondoPreview> = emptyList(),
    val isSearching: Boolean = false,
    /** True once a search for the current query finished (to tell "nothing found" from "not searched"). */
    val hasSearched: Boolean = false,
    val searchError: UiText? = null,
    val selectedId: String? = null,
    val inviteCode: String = "",
    val inviteError: UiText? = null,
    val isSubmitting: Boolean = false,
    val error: UiText? = null,
)

sealed interface JoinCondoAction {
    data class OnQueryChange(val query: String) : JoinCondoAction
    data class OnSelect(val condoId: String) : JoinCondoAction
    data class OnInviteCodeChange(val code: String) : JoinCondoAction
    data object OnContinueClick : JoinCondoAction
    data object OnCreateCondoClick : JoinCondoAction
    data object OnBackClick : JoinCondoAction
}

sealed interface JoinCondoEvent {
    data object GoToResidentData : JoinCondoEvent

    /** The user already belongs to the chosen condominium: it became the active one. */
    data object Finished : JoinCondoEvent
}
