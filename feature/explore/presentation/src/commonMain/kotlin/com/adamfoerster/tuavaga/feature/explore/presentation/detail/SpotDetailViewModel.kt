package com.adamfoerster.tuavaga.feature.explore.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.core.presentation.firstOfMonth
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.domain.ExploreRepository
import com.adamfoerster.tuavaga.feature.explore.domain.quote
import com.adamfoerster.tuavaga.feature.explore.domain.suggestedUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Board 07 · Detalhe da vaga, for the period chosen in Explorar. */
class SpotDetailViewModel(
    private val condoId: String,
    private val spotId: String,
    period: BookingPeriod,
    private val exploreRepository: ExploreRepository,
    private val condoRepository: CondoRepository,
    today: () -> LocalDate,
) : ViewModel() {

    private val _state = MutableStateFlow(
        today().let { now -> SpotDetailState(period = period, today = now, month = period.start.date.firstOfMonth()) },
    )
    val state = _state.asStateFlow()

    private var busyJob: Job? = null

    init {
        load()
    }

    fun onAction(action: SpotDetailAction) {
        when (action) {
            SpotDetailAction.OnPreviousMonth -> changeMonth(-1)
            SpotDetailAction.OnNextMonth -> changeMonth(1)
            SpotDetailAction.OnRetry -> load()
            SpotDetailAction.OnBackClick, SpotDetailAction.OnRequestClick -> Unit
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val condoName = condoRepository.memberships.first().firstOrNull { it.condo.id == condoId }?.condo?.name.orEmpty()
            val period = _state.value.period
            val spot = when (val result = exploreRepository.search(condoId, period)) {
                is Result.Success -> result.data.firstOrNull { it.id == spotId }
                is Result.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                    return@launch
                }
            }
            if (spot == null) {
                _state.update { it.copy(isLoading = false, condoName = condoName, error = DetailTexts.notFound) }
                return@launch
            }
            val availability = (exploreRepository.availability(spotId) as? Result.Success)?.data
            _state.update {
                it.copy(
                    condoName = condoName,
                    spot = spot,
                    availability = availability ?: it.availability,
                    quote = spot.prices.suggestedUnit(period)?.let { unit -> spot.prices.quote(period, unit) },
                    isLoading = false,
                )
            }
            loadBusy()
        }
    }

    private fun changeMonth(delta: Int) {
        _state.update {
            val month = if (delta > 0) it.month.plus(1, DateTimeUnit.MONTH) else it.month.minus(1, DateTimeUnit.MONTH)
            if (month < it.today.firstOfMonth()) it else it.copy(month = month)
        }
        loadBusy()
    }

    private fun loadBusy() {
        busyJob?.cancel()
        busyJob = viewModelScope.launch {
            val result = exploreRepository.busyPeriods(spotId, monthRange(_state.value.month))
            if (result is Result.Success) _state.update { it.copy(busy = result.data) }
        }
    }
}

internal object DetailTexts {
    val notFound = UiText.Dynamic("Esta vaga não está mais anunciada.")
}
