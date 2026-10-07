package com.adamfoerster.tuavaga.core.domain.util

sealed interface DataError : Error {
    enum class Remote : DataError {
        NO_INTERNET,
        REQUEST_TIMEOUT,
        TOO_MANY_REQUESTS,
        UNAUTHORIZED,
        SERVER_ERROR,
        UNKNOWN,
    }

    enum class Local : DataError {
        DISK_FULL,
        UNKNOWN,
    }
}
