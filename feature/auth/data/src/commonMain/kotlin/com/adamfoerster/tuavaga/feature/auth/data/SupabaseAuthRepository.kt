package com.adamfoerster.tuavaga.feature.auth.data

import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.domain.util.asEmptyResult
import com.adamfoerster.tuavaga.feature.auth.domain.AuthError
import com.adamfoerster.tuavaga.feature.auth.domain.AuthRepository
import com.adamfoerster.tuavaga.feature.auth.domain.SignUpResult
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class SupabaseAuthRepository(
    private val auth: Auth,
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): EmptyResult<AuthError> = authCall {
        auth.signInWith(Email) {
            this.email = email.normalized()
            this.password = password
        }
    }

    override suspend fun signUp(
        fullName: String,
        email: String,
        password: String,
    ): Result<SignUpResult, AuthError> = authCall {
        val pendingUser = auth.signUpWith(Email) {
            this.email = email.normalized()
            this.password = password
            data = buildJsonObject { put("full_name", fullName.trim()) }
        }
        // supabase-kt returns null when "Confirm email" is off and the user got signed in.
        if (pendingUser == null) SignUpResult.SIGNED_IN else SignUpResult.CONFIRMATION_REQUIRED
    }

    override suspend fun confirmSignUp(email: String, code: String): EmptyResult<AuthError> = authCall {
        auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email.normalized(), token = code.trim())
    }.asEmptyResult()

    override suspend fun resendSignUpCode(email: String): EmptyResult<AuthError> = authCall {
        auth.resendEmail(type = OtpType.Email.SIGNUP, email = email.normalized())
    }

    override suspend fun requestPasswordReset(email: String): EmptyResult<AuthError> = authCall {
        auth.resetPasswordForEmail(email = email.normalized())
    }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
    ): EmptyResult<AuthError> = authCall {
        // Verifying the code already signs the user in; don't let leaving the screen cancel the
        // password update halfway and strand them signed in with the old password.
        withContext(NonCancellable) {
            val alreadyVerified = auth.currentUserOrNull()?.email.equals(email.normalized(), ignoreCase = true)
            if (!alreadyVerified) {
                // A previous attempt may have verified the (now consumed) code and failed afterwards.
                auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = email.normalized(), token = code.trim())
            }
            auth.updateUser { password = newPassword }
        }
    }.asEmptyResult()

    private fun String.normalized() = trim().lowercase()
}

private inline fun <T> authCall(block: () -> T): Result<T, AuthError> = try {
    Result.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.Failure(e.toAuthError())
}
