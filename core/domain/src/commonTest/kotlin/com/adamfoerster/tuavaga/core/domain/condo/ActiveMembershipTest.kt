package com.adamfoerster.tuavaga.core.domain.condo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActiveMembershipTest {

    private fun membership(id: String) = Membership(
        condo = Condominium(id, "Condo $id", "Rua $id", null, emptyList(), "AV-4K7Q"),
        block = null,
        unit = "1",
        kind = MembershipKind.RESIDENT,
    )

    private val a = membership("a")
    private val b = membership("b")

    @Test
    fun storedActiveCondoWins() {
        assertEquals(b, resolveActiveMembership(listOf(a, b), "b"))
    }

    @Test
    fun fallsBackToFirstWhenStoredIsMissingOrLeft() {
        assertEquals(a, resolveActiveMembership(listOf(a, b), null))
        assertEquals(a, resolveActiveMembership(listOf(a, b), "gone"))
    }

    @Test
    fun noMembershipsMeansNoActiveCondo() {
        assertNull(resolveActiveMembership(emptyList(), "a"))
    }
}
