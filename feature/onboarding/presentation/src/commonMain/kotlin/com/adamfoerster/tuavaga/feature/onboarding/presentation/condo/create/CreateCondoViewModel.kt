package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.condo.NewCondominium
import com.adamfoerster.tuavaga.core.domain.condo.NewLevel
import com.adamfoerster.tuavaga.core.domain.validation.BrFormats
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoOnboardingSession
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoTarget
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Board 20 · Cadastrar meu condomínio (step 1 of 2). Nothing is saved here: the condominium is
 * created together with the user's membership in the next step.
 */
class CreateCondoViewModel(
    private val session: CondoOnboardingSession,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateCondoState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<CreateCondoEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: CreateCondoAction) {
        when (action) {
            is CreateCondoAction.OnNameChange -> _state.update { it.copy(name = action.value, nameError = null) }
            is CreateCondoAction.OnAddressChange -> _state.update { it.copy(address = action.value, addressError = null) }
            is CreateCondoAction.OnCepChange -> _state.update { it.copy(cep = action.value, cepError = null) }
            is CreateCondoAction.OnBlocksChange -> _state.update { it.copy(blocks = action.value) }
            is CreateCondoAction.OnLevelInputChange -> _state.update { it.copy(levelInput = action.value) }
            CreateCondoAction.OnAddLevel -> addLevel()
            is CreateCondoAction.OnRemoveLevel -> updateLevels { it.filterIndexed { i, _ -> i != action.index } }
            is CreateCondoAction.OnMoveLevelUp -> updateLevels { levels ->
                if (action.index !in 1..levels.lastIndex) {
                    levels
                } else {
                    levels.toMutableList().apply { add(action.index - 1, removeAt(action.index)) }
                }
            }
            is CreateCondoAction.OnAddSector -> updateLevel(action.levelIndex) { level ->
                if (level.sectors.size >= MAX_SECTORS) level else level.copy(sectors = level.sectors + nextSectorName(level.sectors))
            }
            is CreateCondoAction.OnRemoveSector -> updateLevel(action.levelIndex) { level ->
                level.copy(sectors = level.sectors.filterIndexed { i, _ -> i != action.sectorIndex })
            }
            CreateCondoAction.OnSubmit -> submit()
            CreateCondoAction.OnBackClick -> Unit
        }
    }

    private fun addLevel() {
        val name = _state.value.levelInput.trim()
        if (name.isEmpty()) return
        _state.update { current ->
            when {
                current.levels.size >= MAX_LEVELS -> current.copy(levelsError = CreateTexts.tooManyLevels)
                current.levels.any { it.name.equals(name, ignoreCase = true) } ->
                    current.copy(levelsError = CreateTexts.duplicateLevel)
                else -> current.copy(levels = current.levels + LevelDraft(name), levelInput = "", levelsError = null)
            }
        }
    }

    private fun updateLevels(transform: (List<LevelDraft>) -> List<LevelDraft>) =
        _state.update { it.copy(levels = transform(it.levels), levelsError = null) }

    private fun updateLevel(index: Int, transform: (LevelDraft) -> LevelDraft) =
        updateLevels { levels -> levels.mapIndexed { i, level -> if (i == index) transform(level) else level } }

    private fun submit() {
        val current = _state.value
        val cep = current.cep.takeIf { it.isNotBlank() }?.let { BrFormats.normalizeCep(it) }
        val checked = current.copy(
            nameError = CreateTexts.nameRequired.takeIf { current.name.trim().length < 2 },
            addressError = CreateTexts.addressRequired.takeIf { current.address.trim().length < 3 },
            cepError = CreateTexts.invalidCep.takeIf { current.cep.isNotBlank() && cep == null },
            levelsError = CreateTexts.levelRequired.takeIf { current.levels.isEmpty() },
        )
        if (listOf(checked.nameError, checked.addressError, checked.cepError, checked.levelsError).any { it != null }) {
            _state.value = checked
            return
        }
        session.target = CondoTarget.New(
            NewCondominium(
                name = current.name.trim(),
                address = current.address.trim(),
                cep = cep,
                blocks = BrFormats.parseList(current.blocks),
                levels = current.levels.map { NewLevel(it.name, it.sectors) },
            ),
        )
        viewModelScope.launch { eventChannel.send(CreateCondoEvent.GoToResidentData) }
    }

    companion object {
        const val MAX_LEVELS = 20
        const val MAX_SECTORS = 26
    }
}

/** Next free letter for a sector: A, B, C… (skips letters already used). */
internal fun nextSectorName(existing: List<String>): String =
    ('A'..'Z').map { it.toString() }.firstOrNull { letter -> existing.none { it.equals(letter, ignoreCase = true) } }
        ?: (existing.size + 1).toString()

internal object CreateTexts {
    val nameRequired = UiText.Dynamic("Informe o nome do condomínio.")
    val addressRequired = UiText.Dynamic("Informe o endereço.")
    val invalidCep = UiText.Dynamic("CEP com 8 dígitos, ex.: 01310-100.")
    val levelRequired = UiText.Dynamic("Adicione pelo menos um andar ou subsolo da garagem.")
    val duplicateLevel = UiText.Dynamic("Esse andar já está na lista.")
    val tooManyLevels = UiText.Dynamic("Limite de ${CreateCondoViewModel.MAX_LEVELS} andares.")
}
