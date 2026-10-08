package com.adamfoerster.tuavaga.feature.bookings.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.presentation.RejectDraft
import com.adamfoerster.tuavaga.core.presentation.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

/** Board 10 · Detalhe da reserva, for the renter and for the owner. */
class BookingDetailViewModel(
    private val bookingId: String,
    private val bookingRepository: BookingRepository,
    private val now: () -> LocalDateTime,
) : ViewModel() {

    private val _state = MutableStateFlow(BookingDetailState(now = now()))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            bookingRepository.bookings.collect { all ->
                val booking = all.firstOrNull { it.id == bookingId }
                _state.update {
                    it.copy(booking = booking, isLoading = it.isLoading && booking == null, notFound = it.notFound && booking == null)
                }
            }
        }
        refresh()
    }

    fun onAction(action: BookingDetailAction) {
        when (action) {
            BookingDetailAction.OnRetry -> refresh()
            BookingDetailAction.OnTick -> _state.update { it.copy(now = now()) }
            BookingDetailAction.OnCancelClick -> _state.update { it.copy(isConfirmingCancel = true, error = null) }
            BookingDetailAction.OnCancelDismiss -> _state.update { it.copy(isConfirmingCancel = false) }
            BookingDetailAction.OnCancelConfirm -> run(close = { it.copy(isConfirmingCancel = false) }) {
                bookingRepository.cancel(bookingId)
            }
            BookingDetailAction.OnApproveClick -> run { bookingRepository.approve(bookingId) }
            BookingDetailAction.OnRejectClick -> _state.update { it.copy(rejecting = RejectDraft(bookingId), error = null) }
            is BookingDetailAction.OnRejectReason -> _state.update { it.copy(rejecting = it.rejecting?.copy(reason = action.reason)) }
            is BookingDetailAction.OnRejectMessage -> _state.update { it.copy(rejecting = it.rejecting?.copy(message = action.message)) }
            BookingDetailAction.OnRejectDismiss -> _state.update { it.copy(rejecting = null) }
            BookingDetailAction.OnRejectConfirm -> {
                val draft = _state.value.rejecting ?: return
                val reason = draft.reason ?: return
                run(close = { it.copy(rejecting = null) }) { bookingRepository.reject(bookingId, reason, draft.message) }
            }
            BookingDetailAction.OnBackClick, BookingDetailAction.OnCheckInClick,
            BookingDetailAction.OnCheckOutClick, BookingDetailAction.OnExploreClick, BookingDetailAction.OnMessageClick,
            -> Unit
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            val result = bookingRepository.refresh()
            _state.update {
                it.copy(
                    isLoading = false,
                    notFound = it.booking == null && result is Result.Success,
                    error = if (it.booking == null) (result as? Result.Failure)?.error?.toUiText() else null,
                    now = now(),
                )
            }
        }
    }

    /** Runs an action; on success [close] dismisses its form (the cache already has the new state). */
    private fun run(
        close: (BookingDetailState) -> BookingDetailState = { it },
        block: suspend () -> EmptyResult<BookingActionError>,
    ) {
        if (_state.value.isWorking) return
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, error = null) }
            when (val result = block()) {
                is Result.Success -> _state.update { close(it).copy(isWorking = false, now = now()) }
                is Result.Failure -> _state.update { it.copy(isWorking = false, error = result.error.toUiText()) }
            }
        }
    }
}
