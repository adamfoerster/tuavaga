package com.adamfoerster.tuavaga.feature.auth.data

import com.adamfoerster.tuavaga.core.data.util.toRemoteError
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.feature.auth.domain.AuthError
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException

internal fun Throwable.toAuthError(): AuthError {
    if (this is AuthRestException) {
        when (errorCode) {
            AuthErrorCode.InvalidCredentials -> return AuthError.INVALID_CREDENTIALS
            AuthErrorCode.EmailNotConfirmed -> return AuthError.EMAIL_NOT_CONFIRMED
            AuthErrorCode.EmailExists,
            AuthErrorCode.UserAlreadyExists -> return AuthError.EMAIL_ALREADY_REGISTERED
            AuthErrorCode.EmailAddressInvalid,
            AuthErrorCode.ValidationFailed -> return AuthError.INVALID_EMAIL
            AuthErrorCode.WeakPassword -> return AuthError.WEAK_PASSWORD
            AuthErrorCode.SamePassword -> return AuthError.SAME_PASSWORD
            AuthErrorCode.OtpExpired -> return AuthError.INVALID_OR_EXPIRED_CODE
            AuthErrorCode.SignupDisabled,
            AuthErrorCode.EmailProviderDisabled -> return AuthError.SIGNUP_DISABLED
            AuthErrorCode.OverRequestRateLimit,
            AuthErrorCode.OverEmailSendRateLimit -> return AuthError.RATE_LIMITED
            else -> Unit
        }
    }
    return when (toRemoteError()) {
        DataError.Remote.NO_INTERNET -> AuthError.NO_INTERNET
        DataError.Remote.REQUEST_TIMEOUT -> AuthError.TIMEOUT
        DataError.Remote.TOO_MANY_REQUESTS -> AuthError.RATE_LIMITED
        DataError.Remote.SERVER_ERROR -> AuthError.SERVER_ERROR
        DataError.Remote.UNAUTHORIZED,
        DataError.Remote.UNKNOWN -> AuthError.UNKNOWN
    }
}
