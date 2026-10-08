package com.adamfoerster.tuavaga.core.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BrFormatsTest {

    @Test
    fun platesAcceptOldAndMercosulFormats() {
        assertEquals("ABC1D23", BrFormats.normalizePlate("abc-1d23"))
        assertEquals("ABC1234", BrFormats.normalizePlate(" ABC 1234 "))
        assertNull(BrFormats.normalizePlate("AB1234"))
        assertNull(BrFormats.normalizePlate("ABCD123"))
        assertNull(BrFormats.normalizePlate("ABC12345"))
    }

    @Test
    fun inviteCodesGetTheHyphen() {
        assertEquals("AV-4K7Q", BrFormats.normalizeInviteCode("av4k7q"))
        assertEquals("AV-4K7Q", BrFormats.normalizeInviteCode(" AV-4K7Q "))
        assertNull(BrFormats.normalizeInviteCode("A-4K7Q"))
        assertNull(BrFormats.normalizeInviteCode("AV-4K7"))
        assertNull(BrFormats.normalizeInviteCode("4V-4K7Q"))
    }

    @Test
    fun cepNeedsEightDigits() {
        assertEquals("01310-100", BrFormats.normalizeCep("01310100"))
        assertEquals("01310-100", BrFormats.normalizeCep("01310-100"))
        assertNull(BrFormats.normalizeCep("1310-100"))
    }

    @Test
    fun phonesAreFormattedWithAreaCode() {
        assertEquals("(11) 91234-5678", BrFormats.normalizePhone("11912345678"))
        assertEquals("(11) 3234-5678", BrFormats.normalizePhone("(11) 3234-5678"))
        assertEquals("(11) 91234-5678", BrFormats.normalizePhone("+55 11 91234-5678"))
        // 55 is also an area code (RS): not stripped from a plain 11-digit number.
        assertEquals("(55) 99123-4567", BrFormats.normalizePhone("55991234567"))
        assertNull(BrFormats.normalizePhone("91234-5678"))
    }

    @Test
    fun listsDropBlanksAndRepeats() {
        assertEquals(listOf("A", "B", "C"), BrFormats.parseList("A, B ,, C, A"))
        assertEquals(emptyList(), BrFormats.parseList("  "))
    }
}
