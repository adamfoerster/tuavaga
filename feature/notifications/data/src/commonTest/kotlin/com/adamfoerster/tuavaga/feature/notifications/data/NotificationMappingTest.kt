package com.adamfoerster.tuavaga.feature.notifications.data

import com.adamfoerster.tuavaga.feature.notifications.domain.NotificationKind
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationMappingTest {

    @Test
    fun row() {
        val n = NotificationDto(
            "n1", "late", "c1", "b1", "Rafael S. passou do horário na vaga B2-14", "Saída combinada às 18:00.",
            "2026-10-08T21:42:00+00:00", null,
        ).toNotification()!!

        assertEquals(NotificationKind.LATE, n.kind)
        assertEquals(LocalDateTime(2026, 10, 8, 18, 42), n.at)
        assertFalse(n.isRead)
        assertTrue(NotificationDto("n2", "approved", "c1", null, "t", "b", "2026-10-08T21:42:00+00:00", "2026-10-08T22:00:00+00:00").toNotification()!!.isRead)
    }

    @Test
    fun unknownKindIsLeftOut() {
        assertNull(NotificationDto("n3", "review", "c1", null, "t", "b", "2026-10-08T21:42:00+00:00").toNotification())
    }
}
