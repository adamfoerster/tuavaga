package com.adamfoerster.tuavaga.feature.explore.presentation.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleRepository
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.explore.domain.BookingError
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.domain.BookingRequest
import com.adamfoerster.tuavaga.feature.explore.domain.ExploreRepository
import com.adamfoerster.tuavaga.feature.explore.domain.suggestedUnit
import com.adamfoerster.tuavaga.feature.explore.presentation.common.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

/** Boards 08 and 09 · Montar reserva → Resumo → pedido enviado. Nothing is charged in the app. */
class BookingRequestViewModel(
    private val condoId: String,
    private val spotId: String,
    period: BookingPeriod,
    private val exploreRepository: ExploreRepository,
    private val condoRepository: CondoRepository,
    private val vehicleRepository: VehicleRepository,
    private val now: () -> LocalDateTime,
) : ViewModel() {

    private val _state = MutableStateFlow(BookingRequestState(period = period))
    val state = _state.asStateFlow()

    private val eventChannel = Channel<BookingRequestEvent>()
    val events = eventChannel.receiveAsFlow()

    private var checkJob: Job? = null

    val nowForSheet: LocalDateTime get() = now()

    init {
        load()
    }

    fun onAction(action: BookingRequestAction) {
        when (action) {
            BookingRequestAction.OnBackClick -> back()
            BookingRequestAction.OnPeriodClick -> _state.update { it.copy(isPeriodSheetOpen = true) }
            BookingRequestAction.OnPeriodDismiss -> _state.update { it.copy(isPeriodSheetOpen = false) }
            is BookingRequestAction.OnPeriodApply -> {
                _state.update { it.copy(period = action.period, isPeriodSheetOpen = false, error = null) }
                checkAvailability()
            }
            is BookingRequestAction.OnUnitSelect -> _state.update { it.copy(unit = action.unit, error = null) }
            is BookingRequestAction.OnVehicleSelect -> _state.update { it.copy(vehicleId = action.vehicleId, vehicleError = null) }
            is BookingRequestAction.OnNoteChange -> _state.update { it.copy(note = action.value.take(NOTE_MAX)) }
            is BookingRequestAction.OnRulesChange -> _state.update { it.copy(rulesAccepted = action.accepted, rulesError = null) }
            BookingRequestAction.OnReviewClick -> review()
            BookingRequestAction.OnSendClick -> send()
            BookingRequestAction.OnRetry -> load()
            BookingRequestAction.OnDoneClick -> viewModelScope.launch { eventChannel.send(BookingRequestEvent.Finished) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val condoName = condoRepository.memberships.first().firstOrNull { it.condo.id == condoId }?.condo?.name.orEmpty()
            val vehicles = (vehicleRepository.list() as? Result.Success)?.data.orEmpty()
            when (val result = exploreRepository.search(condoId, _state.value.period)) {
                is Result.Success -> {
                    val spot = result.data.firstOrNull { it.id == spotId }
                    _state.update { current ->
                        current.copy(
                            condoName = condoName,
                            spot = spot,
                            unit = current.unit ?: spot?.prices?.suggestedUnit(current.period),
                            vehicles = vehicles,
                            vehicleId = current.vehicleId ?: vehicles.firstOrNull()?.id,
                            isLoading = false,
                            error = if (spot == null) RequestTexts.notFound else null,
                        )
                    }
                }
                is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            }
        }
    }

    /** The period changed: ask the backend again whether the spot is free then. */
    private fun checkAvailability() {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            _state.update { it.copy(isChecking = true) }
            when (val result = exploreRepository.search(condoId, _state.value.period)) {
                is Result.Success -> _state.update { current ->
                    val spot = result.data.firstOrNull { it.id == spotId } ?: current.spot
                    val unit = current.unit?.takeIf { spot?.prices?.of(it) != null } ?: spot?.prices?.suggestedUnit(current.period)
                    current.copy(spot = spot, unit = unit, isChecking = false)
                }
                is Result.Failure -> _state.update { it.copy(isChecking = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun review() {
        val s = _state.value
        val spot = s.spot ?: return
        if (s.isChecking) return
        val error = when {
            !spot.available -> RequestTexts.unavailable
            s.period.durationMinutes < spot.minPeriodMinutes -> BookingError.BelowMinimum.toUiText()
            s.quote == null -> BookingError.UnitNotOffered.toUiText()
            else -> null
        }
        val vehicleError = RequestTexts.chooseVehicle.takeIf { s.vehicles.isNotEmpty() && s.selectedVehicle == null }
        val rulesError = RequestTexts.acceptRules.takeIf { spot.rules.isNotEmpty() && !s.rulesAccepted }
        _state.update { it.copy(error = error, vehicleError = vehicleError, rulesError = rulesError) }
        if (error == null && vehicleError == null && rulesError == null) {
            _state.update { it.copy(step = RequestStep.SUMMARY) }
        }
    }

    private fun send() {
        val s = _state.value
        if (s.isSending) return
        val quote = s.quote ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }
            val request = BookingRequest(
                spotId = spotId,
                period = s.period,
                quote = quote,
                vehicleId = s.selectedVehicle?.id,
                note = s.note.trim().ifEmpty { null },
            )
            when (val result = exploreRepository.requestBooking(request)) {
                is Result.Success -> _state.update {
                    it.copy(isSending = false, step = RequestStep.DONE, confirmation = result.data)
                }
                is Result.Failure -> {
                    val error = result.error
                    _state.update {
                        it.copy(
                            isSending = false,
                            error = error.toUiText(),
                            // Availability or the period are the problem: back to the form to change them.
                            step = if (error is BookingError.Remote) it.step else RequestStep.FORM,
                        )
                    }
                    if (error == BookingError.SpotUnavailable) checkAvailability()
                }
            }
        }
    }

    private fun back() {
        when (_state.value.step) {
            RequestStep.FORM -> viewModelScope.launch { eventChannel.send(BookingRequestEvent.Exit) }
            RequestStep.SUMMARY -> _state.update { it.copy(step = RequestStep.FORM, error = null) }
            RequestStep.DONE -> viewModelScope.launch { eventChannel.send(BookingRequestEvent.Finished) }
        }
    }
}

internal object RequestTexts {
    val notFound = UiText.Dynamic("Esta vaga não está mais anunciada.")
    val unavailable = UiText.Dynamic("Essa vaga não está livre nesse período. Mude a entrada ou a saída.")
    val chooseVehicle = UiText.Dynamic("Escolha o veículo.")
    val acceptRules = UiText.Dynamic("Confirme que leu as regras da vaga.")
}
