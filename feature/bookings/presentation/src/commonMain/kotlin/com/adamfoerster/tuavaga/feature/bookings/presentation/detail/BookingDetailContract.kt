package com.adamfoerster.tuavaga.feature.bookings.presentation.detail

import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.RejectReason
import com.adamfoerster.tuavaga.core.presentation.RejectDraft
import com.adamfoerster.tuavaga.core.presentation.UiText
import kotlinx.datetime.LocalDateTime

data class BookingDetailState(
    val now: LocalDateTime,
    val booking: Booking? = null,
    /** No cached copy yet and the refresh is running. */
    val isLoading: Boolean = true,
    /** Not in the user's bookings after a refresh. */
    val notFound: Boolean = false,
    /** An action (cancel, approve, reject) is running. */
    val isWorking: Boolean = false,
    val error: UiText? = null,
    val isConfirmingCancel: Boolean = false,
    /** Owner refusing a pending request. */
    val rejecting: RejectDraft? = null,
)

sealed interface BookingDetailAction {
    data object OnRetry : BookingDetailAction

    /** The screen ticks every 30 s: "Começa em", check-in and lateness follow the clock. */
    data object OnTick : BookingDetailAction
    data object OnCancelClick : BookingDetailAction
    data object OnCancelDismiss : BookingDetailAction
    data object OnCancelConfirm : BookingDetailAction
    data object OnApproveClick : BookingDetailAction
    data object OnRejectClick : BookingDetailAction
    data class OnRejectReason(val reason: RejectReason) : BookingDetailAction
    data class OnRejectMessage(val message: String) : BookingDetailAction
    data object OnRejectDismiss : BookingDetailAction
    data object OnRejectConfirm : BookingDetailAction

    // Navigation, handled by the Root.
    data object OnBackClick : BookingDetailAction
    data object OnCheckInClick : BookingDetailAction
    data object OnCheckOutClick : BookingDetailAction
    data object OnMessageClick : BookingDetailAction

    /** Rejected, cancelled or expired: look for another spot. */
    data object OnExploreClick : BookingDetailAction
}
