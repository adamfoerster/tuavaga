package com.adamfoerster.tuavaga.feature.messages.presentation.chat

import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.messages.presentation.FakeBookings
import com.adamfoerster.tuavaga.feature.messages.presentation.FakeMessages
import com.adamfoerster.tuavaga.feature.messages.presentation.NOW
import com.adamfoerster.tuavaga.feature.messages.presentation.chatBooking
import com.adamfoerster.tuavaga.feature.messages.presentation.common.periodShort
import com.adamfoerster.tuavaga.feature.messages.presentation.message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var messages: FakeMessages
    private lateinit var bookings: FakeBookings

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        messages = FakeMessages()
        bookings = FakeBookings(chatBooking)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ChatViewModel("b1", messages, bookings, now = { NOW })

    @Test
    fun headerComesFromTheBookingAndNoRefreshWhenCached() {
        val vm = viewModel()

        assertEquals(4821, vm.state.value.booking?.code)
        assertEquals(0, bookings.refreshes)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun openedFromANotificationLoadsTheBookings() {
        bookings = FakeBookings()
        viewModel()
        assertEquals(1, bookings.refreshes)
    }

    @Test
    fun sendingClearsTheDraftAndTheStoredCopyReplacesThePendingOne() {
        messages.deliverSends = false
        val vm = viewModel()
        vm.onAction(ChatAction.OnDraftChange("  Cheguei, estou na rampa  "))
        assertTrue(vm.state.value.canSend)
        vm.onAction(ChatAction.OnSendClick)

        assertEquals(listOf("Cheguei, estou na rampa"), messages.sent)
        assertEquals("", vm.state.value.draft)
        // Sent but Realtime has not reloaded yet: still shown as pending.
        assertEquals(1, vm.state.value.pending.size)
        assertTrue(vm.state.value.pending.single().delivered)

        messages.deliver("Cheguei, estou na rampa")
        assertTrue(vm.state.value.pending.isEmpty())
        assertEquals(listOf("Cheguei, estou na rampa"), vm.state.value.messages.map { it.body })
    }

    @Test
    fun aRepeatedTextIsNotMistakenForTheNewOne() {
        messages.chat.value = Result.Success(listOf(message("m0", "Cheguei", mine = true)))
        messages.deliverSends = false
        val vm = viewModel()
        vm.onAction(ChatAction.OnQuickReply("Cheguei"))

        assertEquals(1, vm.state.value.pending.size)
        messages.deliver("Cheguei")
        assertTrue(vm.state.value.pending.isEmpty())
    }

    @Test
    fun failedSendCanBeRetried() {
        messages.sendError = DataError.Remote.NO_INTERNET
        val vm = viewModel()
        vm.onAction(ChatAction.OnQuickReply("Saindo agora"))

        val failed = vm.state.value.pending.single()
        assertTrue(failed.failed)

        messages.sendError = null
        vm.onAction(ChatAction.OnRetry(failed.localId))
        assertEquals(listOf("Saindo agora", "Saindo agora"), messages.sent)
        assertTrue(vm.state.value.pending.isEmpty())
        assertEquals(listOf("Saindo agora"), vm.state.value.messages.map { it.body })
    }

    @Test
    fun incomingMessagesAreMarkedRead() {
        val vm = viewModel()
        assertTrue(messages.reads.isEmpty())

        messages.deliver("Pode vir", mine = false)
        assertEquals(listOf("b1"), messages.reads)

        // System messages and my own do not trigger it.
        messages.chat.value = Result.Success(vm.state.value.messages + message("s", "CHECK-IN · 08:02", mine = false, system = true))
        assertEquals(1, messages.reads.size)
    }

    @Test
    fun loadErrorIsShown() {
        messages.chat.value = Result.Failure(DataError.Remote.NO_INTERNET)
        val vm = viewModel()
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun blankDraftCannotBeSent() {
        val vm = viewModel()
        vm.onAction(ChatAction.OnDraftChange("   "))
        assertFalse(vm.state.value.canSend)
        vm.onAction(ChatAction.OnSendClick)
        assertTrue(messages.sent.isEmpty())
    }

    @Test
    fun shortPeriods() {
        fun p(a: LocalDateTime, b: LocalDateTime) = BookingPeriod(a, b).periodShort()
        assertEquals("10–11/10", p(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 11, 18, 0)))
        assertEquals("10/10", p(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 10, 18, 0)))
        assertEquals("10/10", p(LocalDateTime(2026, 10, 10, 20, 0), LocalDateTime(2026, 10, 11, 0, 0)))
        assertEquals("30/10–02/11", p(LocalDateTime(2026, 10, 30, 8, 0), LocalDateTime(2026, 11, 2, 18, 0)))
    }
}
