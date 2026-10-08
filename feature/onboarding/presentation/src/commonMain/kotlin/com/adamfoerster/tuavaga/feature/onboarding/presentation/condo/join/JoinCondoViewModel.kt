package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.condo.ActiveCondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.CondoPreview
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.domain.validation.BrFormats
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoOnboardingSession
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoTarget
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Board 02 · Entrar em um condomínio: search by name/address or type an invite code. */
@OptIn(FlowPreview::class)
class JoinCondoViewModel(
    private val condoRepository: CondoRepository,
    private val activeCondoRepository: ActiveCondoRepository,
    private val session: CondoOnboardingSession,
) : ViewModel() {

    private val _state = MutableStateFlow(JoinCondoState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<JoinCondoEvent>()
    val events = eventChannel.receiveAsFlow()

    private val queries = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queries
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { query -> search(query) }
        }
    }

    fun onAction(action: JoinCondoAction) {
        when (action) {
            is JoinCondoAction.OnQueryChange -> {
                _state.update { it.copy(query = action.query, error = null) }
                queries.value = action.query.trim()
            }
            is JoinCondoAction.OnSelect -> _state.update {
                it.copy(selectedId = action.condoId, inviteCode = "", inviteError = null, error = null)
            }
            is JoinCondoAction.OnInviteCodeChange -> _state.update {
                it.copy(
                    inviteCode = action.code,
                    inviteError = null,
                    error = null,
                    selectedId = if (action.code.isBlank()) it.selectedId else null,
                )
            }
            JoinCondoAction.OnContinueClick -> submit()
            // Navigation-only actions are handled by the Root composable.
            JoinCondoAction.OnCreateCondoClick, JoinCondoAction.OnBackClick -> Unit
        }
    }

    private suspend fun search(query: String) {
        if (query.length < MIN_QUERY_LENGTH) {
            _state.update { it.copy(results = emptyList(), isSearching = false, hasSearched = false, searchError = null) }
            return
        }
        _state.update { it.copy(isSearching = true, searchError = null) }
        when (val result = condoRepository.search(query)) {
            is Result.Success -> _state.update { current ->
                current.copy(
                    results = result.data,
                    isSearching = false,
                    hasSearched = true,
                    selectedId = current.selectedId?.takeIf { id -> result.data.any { it.id == id } },
                )
            }
            is Result.Failure -> _state.update {
                it.copy(isSearching = false, hasSearched = false, searchError = result.error.toUiText())
            }
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isSubmitting) return

        if (current.inviteCode.isNotBlank()) {
            val code = BrFormats.normalizeInviteCode(current.inviteCode)
            if (code == null) {
                _state.update { it.copy(inviteError = JoinTexts.invalidCodeFormat) }
                return
            }
            viewModelScope.launch {
                _state.update { it.copy(isSubmitting = true) }
                when (val result = condoRepository.findByInviteCode(code)) {
                    is Result.Success -> {
                        val preview = result.data
                        if (preview == null) {
                            _state.update { it.copy(isSubmitting = false, inviteError = JoinTexts.codeNotFound) }
                        } else {
                            proceed(preview)
                        }
                    }
                    is Result.Failure -> _state.update { it.copy(isSubmitting = false, error = result.error.toUiText()) }
                }
            }
            return
        }

        val selected = current.results.firstOrNull { it.id == current.selectedId }
        if (selected == null) {
            _state.update { it.copy(error = JoinTexts.chooseCondo) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            proceed(selected)
        }
    }

    private suspend fun proceed(preview: CondoPreview) {
        if (preview.isMember) {
            activeCondoRepository.setActiveCondo(preview.id)
            _state.update { it.copy(isSubmitting = false) }
            eventChannel.send(JoinCondoEvent.Finished)
        } else {
            session.target = CondoTarget.Existing(preview)
            _state.update { it.copy(isSubmitting = false) }
            eventChannel.send(JoinCondoEvent.GoToResidentData)
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val MIN_QUERY_LENGTH = 2
    }
}

internal object JoinTexts {
    val invalidCodeFormat = UiText.Dynamic("Código inválido. Use o formato AV-4K7Q.")
    val codeNotFound = UiText.Dynamic("Código não encontrado. Confira com o vizinho.")
    val chooseCondo = UiText.Dynamic("Escolha um condomínio da lista ou digite o código de convite.")
}
