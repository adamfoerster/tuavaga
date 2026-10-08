package com.adamfoerster.tuavaga.feature.messages.presentation.chat

import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.feature.messages.domain.ChatMessage
import com.adamfoerster.tuavaga.feature.messages.domain.MESSAGE_MAX
import kotlinx.datetime.LocalDateTime

/** A message sent from this screen that the server list does not show yet. */
data class PendingMessage(
    val localId: Long,
    val body: String,
    val at: LocalDateTime,
    val failed: Boolean = false,
    /** Sent; waits for the next load to bring the stored copy. */
    val delivered: Boolean = false,
    /** How many of my messages had this text when it was sent (the stored copy makes it one more). */
    val baseline: Int,
)

data class ChatState(
    /** Header (from the bookings cache): counterpart, code, spot, period, status. */
    val booking: Booking? = null,
    val messages: List<ChatMessage> = emptyList(),
    val pending: List<PendingMessage> = emptyList(),
    val draft: String = "",
    val isLoading: Boolean = true,
    val error: UiText? = null,
) {
    val canSend: Boolean get() = draft.isNotBlank() && draft.length <= MESSAGE_MAX
}

sealed interface ChatAction {
    data class OnDraftChange(val value: String) : ChatAction
    data object OnSendClick : ChatAction
    data class OnQuickReply(val text: String) : ChatAction
    data class OnRetry(val localId: Long) : ChatAction

    // Navigation, handled by the Root.
    data object OnBackClick : ChatAction
    data object OnBookingClick : ChatAction
}
