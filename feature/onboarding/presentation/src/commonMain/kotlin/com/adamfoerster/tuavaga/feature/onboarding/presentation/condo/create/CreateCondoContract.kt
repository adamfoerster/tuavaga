package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.create

import com.adamfoerster.tuavaga.core.presentation.UiText

data class LevelDraft(val name: String, val sectors: List<String> = emptyList())

data class CreateCondoState(
    val name: String = "",
    val address: String = "",
    val cep: String = "",
    /** Comma-separated blocks / towers; empty = no blocks. */
    val blocks: String = "",
    val levelInput: String = "",
    val levels: List<LevelDraft> = emptyList(),
    val nameError: UiText? = null,
    val addressError: UiText? = null,
    val cepError: UiText? = null,
    val levelsError: UiText? = null,
)

sealed interface CreateCondoAction {
    data class OnNameChange(val value: String) : CreateCondoAction
    data class OnAddressChange(val value: String) : CreateCondoAction
    data class OnCepChange(val value: String) : CreateCondoAction
    data class OnBlocksChange(val value: String) : CreateCondoAction
    data class OnLevelInputChange(val value: String) : CreateCondoAction
    data object OnAddLevel : CreateCondoAction
    data class OnRemoveLevel(val index: Int) : CreateCondoAction
    data class OnMoveLevelUp(val index: Int) : CreateCondoAction
    data class OnAddSector(val levelIndex: Int) : CreateCondoAction
    data class OnRemoveSector(val levelIndex: Int, val sectorIndex: Int) : CreateCondoAction
    data object OnSubmit : CreateCondoAction
    data object OnBackClick : CreateCondoAction
}

sealed interface CreateCondoEvent {
    data object GoToResidentData : CreateCondoEvent
}
