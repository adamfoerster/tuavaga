package com.adamfoerster.tuavaga.feature.profile.data

import com.adamfoerster.tuavaga.core.data.util.toRemoteError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.profile.domain.AccountError
import com.adamfoerster.tuavaga.feature.profile.domain.AccountRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CancellationException

internal class SupabaseAccountRepository(
    private val postgrest: Postgrest,
) : AccountRepository {

    override suspend fun deleteAccount(): EmptyResult<AccountError> = try {
        postgrest.rpc("delete_own_account")
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.Failure(e.toAccountError())
    }
}

internal fun Throwable.toAccountError(): AccountError =
    (this as? PostgrestRestException)?.let { accountErrorOf(it.message) } ?: AccountError.Remote(toRemoteError())

/** delete_own_account raises booking_in_progress (see 20261013000000_account.sql). */
internal fun accountErrorOf(message: String?): AccountError? =
    if (message?.contains("booking_in_progress") == true) AccountError.BookingInProgress else null
