package com.adamfoerster.tuavaga.feature.messages.presentation

import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingQuote
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.Counterpart
import com.adamfoerster.tuavaga.core.domain.booking.RejectReason
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.messages.domain.ChatMessage
import com.adamfoerster.tuavaga.feature.messages.domain.Conversation
import com.adamfoerster.tuavaga.feature.messages.domain.MessagesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDateTime

val NOW = LocalDateTime(2026, 10, 10, 8, 5)

fun message(id: String, body: String, mine: Boolean, system: Boolean = false, at: LocalDateTime = NOW) =
    ChatMessage(id = id, body = body, isSystem = system, isMine = !system && mine, at = at)

/** Messages per booking as a live list; sends append to it only when [deliverSends] is on. */
class FakeMessages : MessagesRepository {
    override val conversations = MutableStateFlow<Result<List<Conversation>, DataError.Remote>>(Result.Success(emptyList()))
    val chat = MutableStateFlow<Result<List<ChatMessage>, DataError.Remote>>(Result.Success(emptyList()))
    var sendError: DataError.Remote? = null
    var deliverSends = true
    val sent = mutableListOf<String>()
    val reads = mutableListOf<String>()

    override fun messages(bookingId: String): Flow<Result<List<ChatMessage>, DataError.Remote>> = chat

    override suspend fun send(bookingId: String, body: String): EmptyResult<DataError.Remote> {
        sent += body
        sendError?.let { return Result.Failure(it) }
        if (deliverSends) deliver(body)
        return Result.Success(Unit)
    }

    /** What Realtime would do after an insert: the list reloads with the stored message. */
    fun deliver(body: String, mine: Boolean = true) {
        val current = (chat.value as? Result.Success)?.data.orEmpty()
        chat.value = Result.Success(current + message("m${current.size}", body, mine))
    }

    override suspend fun markRead(bookingId: String): EmptyResult<DataError.Remote> {
        reads += bookingId
        return Result.Success(Unit)
    }
}

val chatBooking = Booking(
    id = "b1", code = 4821, role = BookingRole.RENTER, status = BookingStatus.CONFIRMED, condoId = "c1",
    condoName = "Residencial Alameda Verde", spotId = "s1", levelName = "Subsolo 2", sectorName = "B", spotNumber = "27",
    directions = null, rules = emptyList(), cancelNoticeHours = 24,
    period = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 11, 18, 0)),
    quote = BookingQuote(BillingUnit.DAY, 2, 3500), note = null, rejectReason = null, rejectMessage = null,
    respondBy = null, cancelledByOwner = null, checkedInAt = null, checkedOutAt = null,
    counterpart = Counterpart("Marina Ribeiro", "A", "31"), vehicle = null, conflict = null,
)

class FakeBookings(vararg initial: Booking) : BookingRepository {
    override val bookings = MutableStateFlow(initial.toList())
    var refreshes = 0
    override suspend fun refresh(): EmptyResult<DataError.Remote> {
        refreshes++
        return Result.Success(Unit)
    }
    private val none: EmptyResult<BookingActionError> = Result.Success(Unit)
    override suspend fun approve(bookingId: String) = none
    override suspend fun reject(bookingId: String, reason: RejectReason, message: String?) = none
    override suspend fun cancel(bookingId: String) = none
    override suspend fun checkIn(bookingId: String) = none
    override suspend fun checkOut(bookingId: String) = none
    override suspend fun extend(bookingId: String, newEnd: LocalDateTime) = none
}
