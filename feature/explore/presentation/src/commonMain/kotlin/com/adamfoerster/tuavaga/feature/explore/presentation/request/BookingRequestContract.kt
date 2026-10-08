package com.adamfoerster.tuavaga.feature.explore.presentation.request

import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingQuote
import com.adamfoerster.tuavaga.core.domain.booking.quote
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.vehicle.Vehicle
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.feature.explore.domain.BookingConfirmation
import com.adamfoerster.tuavaga.feature.explore.domain.SpotListing

/** Board 08 (form), board 09 (summary), then the confirmation of board 17. */
enum class RequestStep { FORM, SUMMARY, DONE }

data class BookingRequestState(
    val step: RequestStep = RequestStep.FORM,
    val condoName: String = "",
    val period: BookingPeriod,
    val spot: SpotListing? = null,
    val unit: BillingUnit? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val vehicleId: String? = null,
    val note: String = "",
    val rulesAccepted: Boolean = false,
    val isPeriodSheetOpen: Boolean = false,
    val isLoading: Boolean = true,
    /** Re-checking availability after the period changed. */
    val isChecking: Boolean = false,
    val isSending: Boolean = false,
    val error: UiText? = null,
    val vehicleError: UiText? = null,
    val rulesError: UiText? = null,
    val confirmation: BookingConfirmation? = null,
) {
    val quote: BookingQuote? get() = unit?.let { spot?.prices?.quote(period, it) }

    val selectedVehicle: Vehicle? get() = vehicles.firstOrNull { it.id == vehicleId }
}

sealed interface BookingRequestAction {
    data object OnBackClick : BookingRequestAction
    data object OnPeriodClick : BookingRequestAction
    data class OnPeriodApply(val period: BookingPeriod) : BookingRequestAction
    data object OnPeriodDismiss : BookingRequestAction
    data class OnUnitSelect(val unit: BillingUnit) : BookingRequestAction
    data class OnVehicleSelect(val vehicleId: String) : BookingRequestAction
    data class OnNoteChange(val value: String) : BookingRequestAction
    data class OnRulesChange(val accepted: Boolean) : BookingRequestAction
    data object OnReviewClick : BookingRequestAction
    data object OnSendClick : BookingRequestAction
    data object OnRetry : BookingRequestAction
    data object OnDoneClick : BookingRequestAction
    data object OnViewBookingClick : BookingRequestAction
}

sealed interface BookingRequestEvent {
    /** Left the flow without booking. */
    data object Exit : BookingRequestEvent

    /** Booked (or requested); back to the main screens. */
    data object Finished : BookingRequestEvent

    /** Booked: open the new booking (board 18 "Ver reserva"). */
    data class ViewBooking(val bookingId: String) : BookingRequestEvent
}

const val NOTE_MAX = 280
