package com.adamfoerster.tuavaga.app

import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.condo.ActiveCondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.CondoError
import com.adamfoerster.tuavaga.core.domain.condo.CondoPreview
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.Condominium
import com.adamfoerster.tuavaga.core.domain.condo.GarageLevel
import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.condo.MembershipKind
import com.adamfoerster.tuavaga.core.domain.condo.NewCondominium
import com.adamfoerster.tuavaga.core.domain.condo.ResidentInfo
import com.adamfoerster.tuavaga.core.domain.prefs.AppPreferencesRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.user.User
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.messages.domain.ChatMessage
import com.adamfoerster.tuavaga.feature.messages.domain.Conversation
import com.adamfoerster.tuavaga.feature.messages.domain.MessagesRepository
import com.adamfoerster.tuavaga.feature.notifications.domain.AppNotification
import com.adamfoerster.tuavaga.feature.notifications.domain.NotificationKind
import com.adamfoerster.tuavaga.feature.notifications.domain.NotificationsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDateTime

fun membership(id: String, name: String = "Condo $id") = Membership(
    condo = Condominium(id, name, "Rua $id", null, listOf("A", "B"), "AV-4K7Q"),
    block = "B",
    unit = "142",
    kind = MembershipKind.RESIDENT,
)

fun signedIn(id: String = "u1") = SessionState.SignedIn(User(id, "$id@condominio.com", "Morador"))

class FakeSession(initial: SessionState = SessionState.SignedOut) : SessionRepository {
    override val sessionState = MutableStateFlow(initial)

    override suspend fun signOut(): EmptyResult<DataError.Remote> {
        sessionState.value = SessionState.SignedOut
        return Result.Success(Unit)
    }
}

class FakePreferences(introSeen: Boolean = true) : AppPreferencesRepository {
    override val introSeen = MutableStateFlow(introSeen)
    override suspend fun markIntroSeen() {
        introSeen.value = true
    }
}

/** Membership cache + a scripted backend: [serverMemberships] is what a refresh loads. */
class FakeCondos : CondoRepository {
    override val memberships = MutableStateFlow<List<Membership>>(emptyList())
    var serverMemberships: List<Membership> = emptyList()
    var refreshError: DataError.Remote? = null
    var refreshCalls = 0

    override suspend fun refreshMemberships(): EmptyResult<DataError.Remote> {
        refreshCalls++
        refreshError?.let { return Result.Failure(it) }
        memberships.value = serverMemberships
        return Result.Success(Unit)
    }

    override suspend fun search(query: String): Result<List<CondoPreview>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun findByInviteCode(code: String): Result<CondoPreview?, DataError.Remote> = Result.Success(null)
    override suspend fun blocksOf(condoId: String): Result<List<String>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun garageOf(condoId: String): Result<List<GarageLevel>, DataError.Remote> = Result.Success(emptyList())
    override suspend fun join(condoId: String, resident: ResidentInfo): EmptyResult<CondoError> = Result.Success(Unit)
    override suspend fun create(condo: NewCondominium, resident: ResidentInfo): Result<String, CondoError> =
        Result.Success("new")
    override suspend fun leave(condoId: String): EmptyResult<CondoError> = Result.Success(Unit)
}

class FakeActiveCondo(initial: String? = null) : ActiveCondoRepository {
    override val activeCondoId = MutableStateFlow(initial)
    override suspend fun setActiveCondo(condoId: String) {
        activeCondoId.value = condoId
    }
}

class FakeNotifications : NotificationsRepository {
    override val notifications = MutableStateFlow<Result<List<AppNotification>, DataError.Remote>>(Result.Success(emptyList()))
    override suspend fun markRead(condoId: String?): EmptyResult<DataError.Remote> = Result.Success(Unit)
}

class FakeMessages : MessagesRepository {
    override val conversations = MutableStateFlow<Result<List<Conversation>, DataError.Remote>>(Result.Success(emptyList()))
    override fun messages(bookingId: String): Flow<Result<List<ChatMessage>, DataError.Remote>> = flowOf(Result.Success(emptyList()))
    override suspend fun send(bookingId: String, body: String): EmptyResult<DataError.Remote> = Result.Success(Unit)
    override suspend fun markRead(bookingId: String): EmptyResult<DataError.Remote> = Result.Success(Unit)
}

fun notification(id: String, condoId: String, read: Boolean = false) = AppNotification(
    id = id, kind = NotificationKind.REQUEST, condoId = condoId, bookingId = null, title = "t", body = "b",
    at = LocalDateTime(2026, 10, 8, 14, 0), isRead = read,
)

fun conversation(bookingId: String, unread: Int) = Conversation(
    bookingId = bookingId, code = 4821, role = BookingRole.RENTER, status = BookingStatus.CONFIRMED, condoId = "c1",
    condoName = "Condo", spotLabel = "B2-27",
    period = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 10, 18, 0)),
    counterpartName = "Marina Ribeiro", last = null, unread = unread,
)
