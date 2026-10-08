package com.adamfoerster.tuavaga.feature.explore.presentation.request

import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.spot.Prices
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.explore.domain.BookingConfirmation
import com.adamfoerster.tuavaga.feature.explore.domain.BookingError
import com.adamfoerster.tuavaga.feature.explore.presentation.FakeCondos
import com.adamfoerster.tuavaga.feature.explore.presentation.FakeExplore
import com.adamfoerster.tuavaga.feature.explore.presentation.FakeVehicles
import com.adamfoerster.tuavaga.feature.explore.presentation.NOW
import com.adamfoerster.tuavaga.feature.explore.presentation.listing
import com.adamfoerster.tuavaga.feature.explore.presentation.membership
import com.adamfoerster.tuavaga.feature.explore.presentation.onix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class BookingRequestViewModelTest {

    // Design example: Sáb 10/10 08:00 → Dom 11/10 18:00, 2 diárias × R$ 35 = R$ 70.
    private val weekend = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 11, 18, 0))
    private lateinit var explore: FakeExplore

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        explore = FakeExplore().apply { listings["c1"] = listOf(listing("27")) }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(vararg vehicles: com.adamfoerster.tuavaga.core.domain.vehicle.Vehicle) =
        BookingRequestViewModel("c1", "27", weekend, explore, FakeCondos(membership("c1", "Residencial Alameda Verde")), FakeVehicles(*vehicles), now = { NOW })

    @Test
    fun startsWithDailyChargeAndTheFirstVehicle() {
        val vm = viewModel(onix)

        val state = vm.state.value
        assertEquals("Residencial Alameda Verde", state.condoName)
        assertEquals(BillingUnit.DAY, state.unit)
        assertEquals(7000, state.quote?.totalCents)
        assertEquals("v1", state.vehicleId)
    }

    @Test
    fun rulesMustBeAcceptedBeforeTheSummary() {
        val vm = viewModel(onix)
        vm.onAction(BookingRequestAction.OnReviewClick)
        assertEquals(RequestTexts.acceptRules, vm.state.value.rulesError)
        assertEquals(RequestStep.FORM, vm.state.value.step)

        vm.onAction(BookingRequestAction.OnRulesChange(true))
        vm.onAction(BookingRequestAction.OnReviewClick)
        assertEquals(RequestStep.SUMMARY, vm.state.value.step)
    }

    @Test
    fun withoutVehiclesTheRequestGoesWithoutOne() = runTest {
        val vm = viewModel()
        vm.onAction(BookingRequestAction.OnRulesChange(true))
        vm.onAction(BookingRequestAction.OnReviewClick)
        vm.onAction(BookingRequestAction.OnSendClick)

        assertNull(explore.requests.single().vehicleId)
    }

    @Test
    fun unavailableSpotCannotBeReviewedAndANewPeriodIsChecked() {
        explore.availableIn = { period -> period.start.date.day == 17 }
        val vm = viewModel(onix)
        vm.onAction(BookingRequestAction.OnRulesChange(true))
        vm.onAction(BookingRequestAction.OnReviewClick)
        assertEquals(RequestTexts.unavailable, vm.state.value.error)

        val nextWeekend = BookingPeriod(LocalDateTime(2026, 10, 17, 8, 0), LocalDateTime(2026, 10, 17, 12, 0))
        vm.onAction(BookingRequestAction.OnPeriodApply(nextWeekend))
        // 4 h: the unit follows what fits (hours) and the spot is free again.
        assertEquals(BillingUnit.DAY, vm.state.value.unit)
        assertEquals(true, vm.state.value.spot?.available)
        vm.onAction(BookingRequestAction.OnUnitSelect(BillingUnit.HOUR))
        assertEquals(3200, vm.state.value.quote?.totalCents)
        vm.onAction(BookingRequestAction.OnReviewClick)
        assertEquals(RequestStep.SUMMARY, vm.state.value.step)
    }

    @Test
    fun belowTheMinimumIsCaughtBeforeSending() {
        explore.listings["c1"] = listOf(listing("27", minPeriodMinutes = 1440, prices = Prices(hourCents = 800)))
        val short = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 10, 10, 0))
        val vm = BookingRequestViewModel("c1", "27", short, explore, FakeCondos(), FakeVehicles(onix), now = { NOW })
        vm.onAction(BookingRequestAction.OnRulesChange(true))
        vm.onAction(BookingRequestAction.OnReviewClick)

        assertNotNull(vm.state.value.error)
        assertEquals(RequestStep.FORM, vm.state.value.step)
    }

    @Test
    fun sendingShowsTheConfirmation() = runTest {
        explore.requestResult = Result.Success(BookingConfirmation("b9", 4821, BookingStatus.CONFIRMED, 7000))
        val vm = viewModel(onix)
        vm.onAction(BookingRequestAction.OnNoteChange(" Chego cedo "))
        vm.onAction(BookingRequestAction.OnRulesChange(true))
        vm.onAction(BookingRequestAction.OnReviewClick)
        vm.onAction(BookingRequestAction.OnSendClick)

        val request = explore.requests.single()
        assertEquals(weekend, request.period)
        assertEquals(BillingUnit.DAY, request.quote.unit)
        assertEquals("v1", request.vehicleId)
        assertEquals("Chego cedo", request.note)
        assertEquals(RequestStep.DONE, vm.state.value.step)
        assertEquals(4821, vm.state.value.confirmation?.code)

        vm.onAction(BookingRequestAction.OnDoneClick)
        assertEquals(BookingRequestEvent.Finished, vm.events.first())
    }

    @Test
    fun viewBookingOpensTheNewBooking() = runTest {
        explore.requestResult = Result.Success(BookingConfirmation("b9", 4821, BookingStatus.PENDING, 7000))
        val vm = viewModel(onix)
        vm.onAction(BookingRequestAction.OnRulesChange(true))
        vm.onAction(BookingRequestAction.OnReviewClick)
        vm.onAction(BookingRequestAction.OnSendClick)
        vm.onAction(BookingRequestAction.OnViewBookingClick)

        assertEquals(BookingRequestEvent.ViewBooking("b9"), vm.events.first())
    }

    @Test
    fun spotTakenMeanwhileGoesBackToTheForm() {
        explore.requestResult = Result.Failure(BookingError.SpotUnavailable)
        val vm = viewModel(onix)
        vm.onAction(BookingRequestAction.OnRulesChange(true))
        vm.onAction(BookingRequestAction.OnReviewClick)
        vm.onAction(BookingRequestAction.OnSendClick)

        assertEquals(RequestStep.FORM, vm.state.value.step)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun networkFailureStaysOnTheSummary() {
        explore.requestResult = Result.Failure(BookingError.Remote(DataError.Remote.NO_INTERNET))
        val vm = viewModel(onix)
        vm.onAction(BookingRequestAction.OnRulesChange(true))
        vm.onAction(BookingRequestAction.OnReviewClick)
        vm.onAction(BookingRequestAction.OnSendClick)

        assertEquals(RequestStep.SUMMARY, vm.state.value.step)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun backFromTheFormExits() = runTest {
        val vm = viewModel(onix)
        vm.onAction(BookingRequestAction.OnBackClick)

        assertEquals(BookingRequestEvent.Exit, vm.events.first())
    }
}
