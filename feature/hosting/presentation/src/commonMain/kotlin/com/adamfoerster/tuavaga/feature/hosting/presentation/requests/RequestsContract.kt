package com.adamfoerster.tuavaga.feature.hosting.presentation.requests

import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.RejectReason
import com.adamfoerster.tuavaga.core.presentation.RejectDraft
import com.adamfoerster.tuavaga.core.presentation.UiText

data class RequestsState(
    /** Pending requests for the user's spots, oldest deadline first. */
    val requests: List<Booking> = emptyList(),
    val isRefreshing: Boolean = true,
    /** Refresh failed (the cached requests stay). */
    val error: UiText? = null,
    /** Request whose accept/reject is running. */
    val workingId: String? = null,
    /** Error of the last accept/reject, shown on that card. */
    val actionError: Pair<String, UiText>? = null,
    val rejecting: RejectDraft? = null,
)

sealed interface RequestsAction {
    data object OnRefresh : RequestsAction
    data class OnAcceptClick(val bookingId: String) : RequestsAction
    data class OnRejectClick(val bookingId: String) : RequestsAction
    data class OnRejectReason(val reason: RejectReason) : RequestsAction
    data class OnRejectMessage(val message: String) : RequestsAction
    data object OnRejectDismiss : RequestsAction
    data object OnRejectConfirm : RequestsAction

    // Navigation, handled by the Root.
    data object OnBackClick : RequestsAction
    data class OnOpenClick(val bookingId: String) : RequestsAction
}
