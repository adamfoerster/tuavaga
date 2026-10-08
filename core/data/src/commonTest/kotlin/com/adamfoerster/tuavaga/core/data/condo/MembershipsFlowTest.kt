package com.adamfoerster.tuavaga.core.data.condo

import com.adamfoerster.tuavaga.core.database.condo.MembershipDao
import com.adamfoerster.tuavaga.core.database.condo.MembershipEntity
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.user.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MembershipsFlowTest {

    private class FakeDao(rows: List<MembershipEntity>) : MembershipDao {
        val table = MutableStateFlow(rows)
        override fun observe(userId: String): Flow<List<MembershipEntity>> = table.map { all -> all.filter { it.userId == userId } }
        override suspend fun clear(userId: String) {
            table.value = table.value.filterNot { it.userId == userId }
        }
        override suspend fun insertAll(memberships: List<MembershipEntity>) {
            table.value = table.value + memberships
        }
    }

    private fun row(userId: String, condoId: String) = MembershipEntity(
        userId = userId, condoId = condoId, condoName = "Condo $condoId", address = "Rua 1", cep = null,
        blocks = "", inviteCode = "AV-4K7Q", block = null, unit = "1", kind = "morador", position = 0,
    )

    private val signedIn = SessionState.SignedIn(User("u1", "u1@condominio.com", null))

    /** Like the real session repository: every collector sees Loading before the known state. */
    private fun session(state: SessionState): Flow<SessionState> = flowOf(state).onStart { emit(SessionState.Loading) }

    @Test
    fun firstMembershipsSkipTheInitialLoading() = runTest {
        val dao = FakeDao(listOf(row("u1", "c1"), row("u1", "c2"), row("u2", "c9")))

        val first = observeMemberships(session(signedIn).signedInUserId(), dao).first()

        assertEquals(listOf("c1", "c2"), first.map { it.condo.id })
    }

    @Test
    fun signedOutHasNoMemberships() = runTest {
        val dao = FakeDao(listOf(row("u1", "c1")))

        assertEquals(emptyList(), observeMemberships(session(SessionState.SignedOut).signedInUserId(), dao).first())
    }

    @Test
    fun userIdIsKnownOnFirstRead() = runTest {
        assertEquals("u1", session(signedIn).signedInUserId().first())
        assertNull(session(SessionState.SignedOut).signedInUserId().first())
    }
}
