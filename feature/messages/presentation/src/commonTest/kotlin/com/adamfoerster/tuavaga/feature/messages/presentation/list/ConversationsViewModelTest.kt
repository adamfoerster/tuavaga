package com.adamfoerster.tuavaga.feature.messages.presentation.list

import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.messages.domain.Conversation
import com.adamfoerster.tuavaga.feature.messages.presentation.FakeMessages
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val conversation = Conversation(
        bookingId = "b1", code = 4821, role = BookingRole.RENTER, status = BookingStatus.CONFIRMED, condoId = "c1",
        condoName = "Alameda", spotLabel = "B2-27",
        period = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 11, 18, 0)),
        counterpartName = "Marina Ribeiro", last = null, unread = 1,
    )

    @Test
    fun liveListKeepsTheLastOneOnErrors() {
        val repo = FakeMessages()
        val vm = ConversationsViewModel(repo)
        assertFalse(vm.state.value.isLoading)

        repo.conversations.value = Result.Success(listOf(conversation))
        assertEquals(listOf("b1"), vm.state.value.conversations.map { it.bookingId })

        repo.conversations.value = Result.Failure(DataError.Remote.NO_INTERNET)
        assertNotNull(vm.state.value.error)
        assertEquals(1, vm.state.value.conversations.size)

        repo.conversations.value = Result.Success(listOf(conversation.copy(unread = 0)))
        assertNull(vm.state.value.error)
    }
}
