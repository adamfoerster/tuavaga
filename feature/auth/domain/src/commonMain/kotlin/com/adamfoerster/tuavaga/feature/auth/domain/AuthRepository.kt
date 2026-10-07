package com.adamfoerster.tuavaga.feature.auth.domain

import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result

interface AuthRepository {
    suspend fun signIn(email: String, password: String): EmptyResult<AuthError>

    suspend fun signUp(fullName: String, email: String, password: String): Result<SignUpResult, AuthError>

    /** Confirms a new account with the 6-digit code e-mailed after [signUp]. Signs the user in. */
    suspend fun confirmSignUp(email: String, code: String): EmptyResult<AuthError>

    suspend fun resendSignUpCode(email: String): EmptyResult<AuthError>

    /** E-mails a 6-digit recovery code. */
    suspend fun requestPasswordReset(email: String): EmptyResult<AuthError>

    /** Verifies the recovery code and sets the new password. Leaves the user signed in. */
    suspend fun resetPassword(email: String, code: String, newPassword: String): EmptyResult<AuthError>
}

enum class SignUpResult {
    /** E-mail confirmation is disabled in Supabase: the user is already signed in. */
    SIGNED_IN,

    /** A confirmation code was e-mailed. */
    CONFIRMATION_REQUIRED,
}
