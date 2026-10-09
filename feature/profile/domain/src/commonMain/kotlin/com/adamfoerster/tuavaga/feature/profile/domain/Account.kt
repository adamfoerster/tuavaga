package com.adamfoerster.tuavaga.feature.profile.domain

import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Error

sealed interface AccountError : Error {
    /** Someone is parked in a booking of the user (as renter or owner): finish it first. */
    data object BookingInProgress : AccountError
    data class Remote(val error: DataError.Remote) : AccountError
}

interface AccountRepository {
    /**
     * Deletes the account on the server: future bookings are cancelled (the other party is notified)
     * and everything of the user is removed. The caller then signs out locally.
     */
    suspend fun deleteAccount(): EmptyResult<AccountError>
}
