package com.adamfoerster.tuavaga.feature.hosting.presentation.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.booking.pendingRequests
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.presentation.RejectDraft
import com.adamfoerster.tuavaga.core.presentation.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Board 17 · Solicitações: accept, or refuse with a reason and an optional message. */
class RequestsViewModel(
    private val bookingRepository: BookingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RequestsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            bookingRepository.bookings.collect { all ->
                _state.update { state ->
                    val requests = all.pendingRequests()
                    // A request answered elsewhere (or expired) closes its form.
                    state.copy(
                        requests = requests,
                        rejecting = state.rejecting?.takeIf { draft -> requests.any { it.id == draft.bookingId } },
                    )
                }
            }
        }
        refresh()
    }

    fun onAction(action: RequestsAction) {
        when (action) {
            RequestsAction.OnRefresh -> refresh()
            is RequestsAction.OnAcceptClick -> answer(action.bookingId) { bookingRepository.approve(action.bookingId) }
            is RequestsAction.OnRejectClick -> _state.update { it.copy(rejecting = RejectDraft(action.bookingId), actionError = null) }
            is RequestsAction.OnRejectReason -> _state.update { it.copy(rejecting = it.rejecting?.copy(reason = action.reason)) }
            is RequestsAction.OnRejectMessage -> _state.update { it.copy(rejecting = it.rejecting?.copy(message = action.message)) }
            RequestsAction.OnRejectDismiss -> _state.update { it.copy(rejecting = null) }
            RequestsAction.OnRejectConfirm -> {
                val draft = _state.value.rejecting ?: return
                val reason = draft.reason ?: return
                answer(draft.bookingId) { bookingRepository.reject(draft.bookingId, reason, draft.message) }
            }
            RequestsAction.OnBackClick, is RequestsAction.OnOpenClick -> Unit
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val result = bookingRepository.refresh()
            _state.update { it.copy(isRefreshing = false, error = (result as? Result.Failure)?.error?.toUiText()) }
        }
    }

    private fun answer(bookingId: String, block: suspend () -> EmptyResult<BookingActionError>) {
        if (_state.value.workingId != null) return
        viewModelScope.launch {
            _state.update { it.copy(workingId = bookingId, actionError = null) }
            when (val result = block()) {
                is Result.Success -> _state.update { it.copy(workingId = null, rejecting = null) }
                is Result.Failure -> _state.update {
                    it.copy(workingId = null, actionError = bookingId to result.error.toUiText())
                }
            }
        }
    }
}
