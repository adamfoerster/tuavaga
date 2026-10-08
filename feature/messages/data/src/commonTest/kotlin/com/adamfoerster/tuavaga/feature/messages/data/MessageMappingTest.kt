package com.adamfoerster.tuavaga.feature.messages.data

import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun conversationRow() {
        val row = json.decodeFromString<ConversationDto>(
            """{"booking_id":"b1","code":4821,"role":"owner","status":"in_progress","condo_id":"c1","condo_name":"Alameda",
               "spot_label":"B2-27","starts_at":"2026-10-10T11:00:00+00:00","ends_at":"2026-10-11T21:00:00+00:00",
               "counterpart_name":"Rafael Souza","last_body":"Cheguei","last_kind":"text","last_mine":false,
               "last_at":"2026-10-10T11:01:00+00:00","unread":2}""",
        ).toConversation()

        assertEquals(BookingRole.OWNER, row.role)
        assertEquals(BookingStatus.IN_PROGRESS, row.status)
        assertEquals(LocalDateTime(2026, 10, 10, 8, 1), row.last?.at)
        assertFalse(row.last!!.isMine)
        assertEquals(2, row.unread)
    }

    @Test
    fun conversationWithoutMessages() {
        val row = json.decodeFromString<ConversationDto>(
            """{"booking_id":"b1","code":4821,"role":"renter","status":"pending","condo_id":"c1","condo_name":"Alameda",
               "spot_label":"B2-27","starts_at":"2026-10-10T11:00:00+00:00","ends_at":"2026-10-11T21:00:00+00:00",
               "counterpart_name":null,"last_body":null,"last_kind":null,"last_mine":null,"last_at":null,"unread":0}""",
        ).toConversation()
        assertNull(row.last)
    }

    @Test
    fun messagesKnowWhoSent() {
        val mine = MessageDto("m1", "u1", "text", "Saindo agora", "2026-10-10T21:00:00+00:00").toMessage("u1")
        val system = MessageDto("m2", null, "system", "CHECK-OUT · 18:00", "2026-10-10T21:00:00+00:00").toMessage("u1")

        assertTrue(mine.isMine)
        assertEquals(LocalDateTime(2026, 10, 10, 18, 0), mine.at)
        assertTrue(system.isSystem)
        assertFalse(system.isMine)
    }
}
