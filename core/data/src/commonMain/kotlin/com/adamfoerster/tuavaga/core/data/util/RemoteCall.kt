package com.adamfoerster.tuavaga.core.data.util

import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.Result
import kotlinx.coroutines.CancellationException

/**
 * Runs a Supabase call and maps any failure to [DataError.Remote]. Catches [Throwable] because
 * wasmJs reports network failures as `kotlin.Error`; cancellation is always rethrown.
 */
inline fun <T> remoteCall(block: () -> T): Result<T, DataError.Remote> = try {
    Result.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.Failure(e.toRemoteError())
}
