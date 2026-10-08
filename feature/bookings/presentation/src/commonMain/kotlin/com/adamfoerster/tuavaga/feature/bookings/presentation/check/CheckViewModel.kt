package com.adamfoerster.tuavaga.feature.bookings.presentation.check

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.presentation.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

/** Boards 24 (check-in) and 25 (check-out, with "Preciso de mais tempo"). */
class CheckViewModel(
    kind: CheckKind,
    private val bookingId: String,
    private val bookingRepository: BookingRepository,
    private val now: () -> LocalDateTime,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckState(kind = kind, now = now()))
    val state = _state.asStateFlow()

    private val eventChannel = Channel<CheckEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            bookingRepository.bookings.collect { all ->
                _state.update { it.copy(booking = all.firstOrNull { b -> b.id == bookingId }) }
            }
        }
    }

    fun onAction(action: CheckAction) {
        when (action) {
            is CheckAction.OnToggle -> _state.update {
                it.copy(checked = if (action.index in it.checked) it.checked - action.index else it.checked + action.index)
            }
            CheckAction.OnTick -> _state.update { it.copy(now = now()) }
            CheckAction.OnConfirmClick -> confirm()
            CheckAction.OnExtendClick -> _state.update { it.copy(isExtendOpen = true, extendTo = it.extendOptions.firstOrNull(), error = null) }
            is CheckAction.OnExtendSelect -> _state.update { it.copy(extendTo = action.end) }
            CheckAction.OnExtendDismiss -> _state.update { it.copy(isExtendOpen = false) }
            CheckAction.OnExtendConfirm -> extend()
            CheckAction.OnBackClick -> Unit
        }
    }

    private fun confirm() {
        val state = _state.value
        if (!state.canConfirm) return
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, error = null) }
            val result = when (state.kind) {
                CheckKind.IN -> bookingRepository.checkIn(bookingId)
                CheckKind.OUT -> bookingRepository.checkOut(bookingId)
            }
            when (result) {
                is Result.Success -> {
                    _state.update { it.copy(isWorking = false) }
                    eventChannel.send(CheckEvent.Done)
                }
                is Result.Failure -> _state.update { it.copy(isWorking = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun extend() {
        val end = _state.value.extendTo ?: return
        if (_state.value.isWorking) return
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, error = null) }
            when (val result = bookingRepository.extend(bookingId, end)) {
                is Result.Success -> _state.update { it.copy(isWorking = false, isExtendOpen = false, now = now()) }
                is Result.Failure -> _state.update { it.copy(isWorking = false, error = result.error.toUiText()) }
            }
        }
    }
}
