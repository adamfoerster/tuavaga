package com.adamfoerster.tuavaga.feature.profile.data

import com.adamfoerster.tuavaga.feature.profile.domain.AccountError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountErrorsTest {

    @Test
    fun parkedBookingBlocksTheDeletion() {
        assertEquals(AccountError.BookingInProgress, accountErrorOf("booking_in_progress"))
        assertNull(accountErrorOf("timeout"))
    }
}
