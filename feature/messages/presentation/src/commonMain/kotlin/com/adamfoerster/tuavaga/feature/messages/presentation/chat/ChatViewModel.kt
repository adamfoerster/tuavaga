package com.adamfoerster.tuavaga.feature.messages.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.messages.domain.ChatMessage
import com.adamfoerster.tuavaga.feature.messages.domain.MESSAGE_MAX
import com.adamfoerster.tuavaga.feature.messages.domain.MessagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

/**
 * Board 23 · Chat da reserva. Sending is optimistic: the text shows at once and is replaced by the
 * stored copy when Realtime reloads the conversation; a failed send stays with "Tentar de novo".
 */
class ChatViewModel(
    private val bookingId: String,
    private val messagesRepository: MessagesRepository,
    private val bookingRepository: BookingRepository,
    private val now: () -> LocalDateTime,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state = _state.asStateFlow()

    private var nextLocalId = 0L

    init {
        viewModelScope.launch {
            bookingRepository.bookings.collect { all ->
                _state.update { it.copy(booking = all.firstOrNull { b -> b.id == bookingId }) }
            }
        }
        viewModelScope.launch {
            // Opened from a notification before the bookings were loaded.
            if (bookingRepository.bookings.first().none { it.id == bookingId }) bookingRepository.refresh()
        }
        viewModelScope.launch {
            messagesRepository.messages(bookingId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _state.update { state ->
                            state.copy(
                                messages = result.data,
                                pending = state.pending.filterNot { it.delivered && result.data.mineWith(it.body) > it.baseline },
                                isLoading = false,
                                error = null,
                            )
                        }
                        // Opening the chat (or a new message arriving while it is open) reads it.
                        if (result.data.lastOrNull()?.let { !it.isMine && !it.isSystem } == true) {
                            messagesRepository.markRead(bookingId)
                        }
                    }
                    is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                }
            }
        }
    }

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.OnDraftChange -> _state.update { it.copy(draft = action.value.take(MESSAGE_MAX)) }
            ChatAction.OnSendClick -> {
                val text = _state.value.draft.trim()
                if (text.isEmpty()) return
                _state.update { it.copy(draft = "") }
                send(text)
            }
            is ChatAction.OnQuickReply -> send(action.text)
            is ChatAction.OnRetry -> {
                val pending = _state.value.pending.firstOrNull { it.localId == action.localId } ?: return
                _state.update { state -> state.copy(pending = state.pending.filterNot { it.localId == pending.localId }) }
                send(pending.body)
            }
            ChatAction.OnBackClick, ChatAction.OnBookingClick -> Unit
        }
    }

    private fun send(body: String) {
        val localId = nextLocalId++
        val pending = PendingMessage(localId, body, now(), baseline = _state.value.messages.mineWith(body))
        _state.update { it.copy(pending = it.pending + pending) }
        viewModelScope.launch {
            val result = messagesRepository.send(bookingId, body)
            _state.update { state ->
                state.copy(
                    pending = state.pending.mapNotNull { p ->
                        when {
                            p.localId != localId -> p
                            result is Result.Failure -> p.copy(failed = true)
                            // Already in the list (Realtime was faster than the answer).
                            state.messages.mineWith(body) > p.baseline -> null
                            else -> p.copy(delivered = true)
                        }
                    },
                )
            }
        }
    }

    private fun List<ChatMessage>.mineWith(body: String): Int = count { it.isMine && it.body == body }
}
