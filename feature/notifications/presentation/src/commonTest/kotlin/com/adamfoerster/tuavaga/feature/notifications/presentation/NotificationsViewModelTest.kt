package com.adamfoerster.tuavaga.feature.notifications.presentation

import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.domain.condo.CondoError
import com.adamfoerster.tuavaga.core.domain.condo.CondoPreview
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.Condominium
import com.adamfoerster.tuavaga.core.domain.condo.GarageLevel
import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.condo.MembershipKind
import com.adamfoerster.tuavaga.core.domain.condo.NewCondominium
import com.adamfoerster.tuavaga.core.domain.condo.ResidentInfo
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.notifications.domain.AppNotification
import com.adamfoerster.tuavaga.feature.notifications.domain.NotificationKind
import com.adamfoerster.tuavaga.feature.notifications.domain.NotificationsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private class FakeNotifications : NotificationsRepository {
        override val notifications = MutableStateFlow<Result<List<AppNotification>, DataError.Remote>>(Result.Success(emptyList()))
        var markError: DataError.Remote? = null
        val marked = mutableListOf<String?>()
        override suspend fun markRead(condoId: String?): EmptyResult<DataError.Remote> {
            marked += condoId
            return markError?.let { Result.Failure(it) } ?: Result.Success(Unit)
        }
    }

    private class FakeCondos(vararg memberships: Membership) : CondoRepository {
        override val memberships = MutableStateFlow(memberships.toList())
        override suspend fun refreshMemberships(): EmptyResult<DataError.Remote> = Result.Success(Unit)
        override suspend fun search(query: String): Result<List<CondoPreview>, DataError.Remote> = Result.Success(emptyList())
        override suspend fun findByInviteCode(code: String): Result<CondoPreview?, DataError.Remote> = Result.Success(null)
        override suspend fun blocksOf(condoId: String): Result<List<String>, DataError.Remote> = Result.Success(emptyList())
        override suspend fun garageOf(condoId: String): Result<List<GarageLevel>, DataError.Remote> = Result.Success(emptyList())
        override suspend fun join(condoId: String, resident: ResidentInfo): EmptyResult<CondoError> = Result.Success(Unit)
        override suspend fun create(condo: NewCondominium, resident: ResidentInfo): Result<String, CondoError> = Result.Success("x")
        override suspend fun leave(condoId: String): EmptyResult<CondoError> = Result.Success(Unit)
    }

    private fun membership(id: String, name: String) =
        Membership(Condominium(id, name, "Rua", null, emptyList(), "AV-4K7Q"), "B", "142", MembershipKind.RESIDENT)

    private fun n(id: String, condo: String, at: LocalDateTime, kind: NotificationKind = NotificationKind.REQUEST, read: Boolean = false) =
        AppNotification(id, kind, condo, "b-$id", "Título $id", "Corpo", at, read)

    private lateinit var repo: FakeNotifications
    private lateinit var vm: NotificationsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = FakeNotifications()
        // Board 26: today, yesterday and older, from two condominiums.
        repo.notifications.value = Result.Success(
            listOf(
                n("late", "av", LocalDateTime(2026, 10, 8, 18, 42), NotificationKind.LATE),
                n("req", "av", LocalDateTime(2026, 10, 8, 14, 10)),
                n("ok", "sc", LocalDateTime(2026, 10, 8, 11, 30), NotificationKind.APPROVED),
                n("rem", "av", LocalDateTime(2026, 10, 7, 20, 0), NotificationKind.REMINDER, read = true),
                n("old", "sc", LocalDateTime(2026, 10, 1, 9, 12), NotificationKind.REJECTED, read = true),
            ),
        )
        vm = NotificationsViewModel(repo, FakeCondos(membership("av", "Alameda Verde"), membership("sc", "Santa Clara")), today = { LocalDate(2026, 10, 8) })
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun groupedByDayNewestFirst() {
        val state = vm.state.value
        assertEquals(3, state.unread)
        assertEquals(listOf(LocalDate(2026, 10, 8), LocalDate(2026, 10, 7), LocalDate(2026, 10, 1)), state.groups.map { it.day })
        assertEquals(listOf("late", "req", "ok"), state.groups.first().items.map { it.id })
        assertEquals("Hoje", state.dayLabel(LocalDate(2026, 10, 8)))
        assertEquals("Ontem", state.dayLabel(LocalDate(2026, 10, 7)))
        assertNull(state.dayLabel(LocalDate(2026, 10, 1)))
        assertEquals("Santa Clara", state.condoName("sc"))
    }

    @Test
    fun filterByCondominiumAndMarkOnlyThoseRead() {
        vm.onAction(NotificationsAction.OnFilterSelect("sc"))
        assertEquals(listOf("ok", "old"), vm.state.value.visible.map { it.id })
        assertEquals(1, vm.state.value.unread)

        vm.onAction(NotificationsAction.OnMarkAllRead)
        assertEquals(listOf<String?>("sc"), repo.marked)
        assertEquals(0, vm.state.value.unread)

        vm.onAction(NotificationsAction.OnFilterSelect(null))
        assertEquals(2, vm.state.value.unread)
    }

    @Test
    fun nothingToMarkSendsNothing() {
        vm.onAction(NotificationsAction.OnMarkAllRead)
        vm.onAction(NotificationsAction.OnMarkAllRead)
        assertEquals(listOf<String?>(null), repo.marked)
    }

    @Test
    fun markFailureKeepsTheBadge() {
        repo.markError = DataError.Remote.NO_INTERNET
        vm.onAction(NotificationsAction.OnMarkAllRead)

        assertNotNull(vm.state.value.error)
        assertEquals(3, vm.state.value.unread)
    }

    @Test
    fun everyKindHasAWord() {
        assertEquals("Atraso" to KbTone.Caution, NotificationKind.LATE.tag())
        assertEquals("Lembrete" to KbTone.Info, NotificationKind.REMINDER.tag())
        NotificationKind.entries.forEach { assertTrue(it.tag().first.isNotBlank()) }
    }
}
