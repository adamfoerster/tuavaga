package com.adamfoerster.tuavaga.core.data.util

import com.adamfoerster.tuavaga.core.domain.util.DataError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.HttpRequestTimeoutException

/**
 * Generic mapping of supabase-kt / Ktor failures. Features refine it with their own errors.
 *
 * Callers must catch [Throwable], not just Exception: on wasmJs Ktor reports network failures
 * as `kotlin.Error("Fail to fetch")`.
 */
fun Throwable.toRemoteError(): DataError.Remote = when (this) {
    is HttpRequestTimeoutException -> DataError.Remote.REQUEST_TIMEOUT
    is HttpRequestException -> DataError.Remote.NO_INTERNET
    is RestException -> when (statusCode) {
        401, 403 -> DataError.Remote.UNAUTHORIZED
        429 -> DataError.Remote.TOO_MANY_REQUESTS
        in 500..599 -> DataError.Remote.SERVER_ERROR
        else -> DataError.Remote.UNKNOWN
    }
    else -> if (isBrowserFetchFailure()) DataError.Remote.NO_INTERNET else DataError.Remote.UNKNOWN
}

private fun Throwable.isBrowserFetchFailure(): Boolean =
    generateSequence(this) { it.cause }.any { it.message?.contains("fetch", ignoreCase = true) == true }
