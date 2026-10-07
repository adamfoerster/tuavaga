package com.adamfoerster.tuavaga.feature.auth.domain

import com.adamfoerster.tuavaga.core.domain.util.Error

enum class AuthError : Error {
    INVALID_CREDENTIALS,
    EMAIL_NOT_CONFIRMED,
    EMAIL_ALREADY_REGISTERED,
    INVALID_EMAIL,
    WEAK_PASSWORD,
    SAME_PASSWORD,
    INVALID_OR_EXPIRED_CODE,
    SIGNUP_DISABLED,
    RATE_LIMITED,
    NO_INTERNET,
    TIMEOUT,
    SERVER_ERROR,
    UNKNOWN,
}
