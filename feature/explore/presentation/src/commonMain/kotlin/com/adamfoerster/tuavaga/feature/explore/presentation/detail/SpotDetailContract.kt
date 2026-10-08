package com.adamfoerster.tuavaga.feature.explore.presentation.detail

import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingQuote
import com.adamfoerster.tuavaga.core.domain.spot.Availability
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.feature.explore.domain.SpotListing
import kotlinx.datetime.LocalDate

data class SpotDetailState(
    val condoName: String = "",
    val period: BookingPeriod,
    val today: LocalDate,
    /** First day of the month shown in the calendar. */
    val month: LocalDate,
    val spot: SpotListing? = null,
    val availability: Availability = Availability(),
    val busy: List<BookingPeriod> = emptyList(),
    val quote: BookingQuote? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
)

sealed interface SpotDetailAction {
    data object OnPreviousMonth : SpotDetailAction
    data object OnNextMonth : SpotDetailAction
    data object OnRetry : SpotDetailAction

    // Navigation, handled by the Root.
    data object OnBackClick : SpotDetailAction
    data object OnRequestClick : SpotDetailAction
}
