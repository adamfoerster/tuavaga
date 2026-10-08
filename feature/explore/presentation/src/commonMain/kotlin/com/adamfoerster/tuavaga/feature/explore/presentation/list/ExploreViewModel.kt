package com.adamfoerster.tuavaga.feature.explore.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.condo.ActiveCondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.resolveActiveMembership
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.domain.ExploreFilters
import com.adamfoerster.tuavaga.feature.explore.domain.ExploreRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

/**
 * Boards 04/05/06 · Explorar: the active condominium's spots for a period, as a list or a garage
 * map. Searches again whenever the active condominium or the period changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModel(
    private val exploreRepository: ExploreRepository,
    condoRepository: CondoRepository,
    activeCondoRepository: ActiveCondoRepository,
    private val now: () -> LocalDateTime,
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreState(period = BookingPeriod.defaultFrom(now())))
    val state = _state.asStateFlow()

    private val reload = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(condoRepository.memberships, activeCondoRepository.activeCondoId) { memberships, activeId ->
                resolveActiveMembership(memberships, activeId)
            }
                .distinctUntilChanged()
                .collect { condo -> _state.update { it.copy(condo = condo, selectedSpotId = null) } }
        }
        viewModelScope.launch {
            combine(
                _state.map { it.condo?.condo?.id }.distinctUntilChanged(),
                _state.map { it.period }.distinctUntilChanged(),
                reload,
            ) { condoId, period, _ -> condoId to period }
                .collectLatest { (condoId, period) -> if (condoId != null) search(condoId, period) }
        }
    }

    val nowForSheet: LocalDateTime get() = now()

    fun onAction(action: ExploreAction) {
        when (action) {
            ExploreAction.OnPeriodClick -> _state.update { it.copy(isPeriodSheetOpen = true) }
            ExploreAction.OnPeriodDismiss -> _state.update { it.copy(isPeriodSheetOpen = false) }
            is ExploreAction.OnPeriodApply -> _state.update { it.copy(period = action.period, isPeriodSheetOpen = false) }
            is ExploreAction.OnModeSelect -> _state.update { it.copy(mode = action.mode) }
            is ExploreAction.OnFeatureToggle -> _state.update {
                val features = it.filters.features
                it.copy(filters = it.filters.copy(features = if (action.feature in features) features - action.feature else features + action.feature))
            }
            ExploreAction.OnCheapToggle -> _state.update {
                it.copy(filters = it.filters.copy(maxHourCents = if (it.filters.maxHourCents == null) ExploreFilters.CHEAP_HOUR_CENTS else null))
            }
            ExploreAction.OnClearFilters -> _state.update { it.copy(filters = ExploreFilters()) }
            is ExploreAction.OnLevelSelect -> _state.update { it.copy(selectedLevelId = action.levelId, selectedSpotId = null) }
            is ExploreAction.OnMapSpotClick -> _state.update {
                it.copy(selectedSpotId = if (it.selectedSpotId == action.spotId) null else action.spotId)
            }
            ExploreAction.OnRetry -> reload.update { it + 1 }
            is ExploreAction.OnOpenSpot, ExploreAction.OnListSpotClick -> Unit
        }
    }

    private suspend fun search(condoId: String, period: BookingPeriod) {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = exploreRepository.search(condoId, period)) {
            is Result.Success -> _state.update { it.copy(isLoading = false, listings = result.data) }
            is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
        }
    }
}
