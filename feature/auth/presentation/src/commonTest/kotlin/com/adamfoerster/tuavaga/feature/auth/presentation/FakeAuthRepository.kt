package com.adamfoerster.tuavaga.feature.auth.presentation

import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.auth.domain.AuthError
import com.adamfoerster.tuavaga.feature.auth.domain.AuthRepository
import com.adamfoerster.tuavaga.feature.auth.domain.SignUpResult

class FakeAuthRepository : AuthRepository {
    var signInResult: EmptyResult<AuthError> = Result.Success(Unit)
    var signUpResult: Result<SignUpResult, AuthError> = Result.Success(SignUpResult.CONFIRMATION_REQUIRED)
    var requestPasswordResetResult: EmptyResult<AuthError> = Result.Success(Unit)
    var resetPasswordResult: EmptyResult<AuthError> = Result.Success(Unit)

    val calls = mutableListOf<String>()

    override suspend fun signIn(email: String, password: String): EmptyResult<AuthError> {
        calls += "signIn($email)"
        return signInResult
    }

    override suspend fun signUp(fullName: String, email: String, password: String): Result<SignUpResult, AuthError> {
        calls += "signUp($email)"
        return signUpResult
    }

    override suspend fun confirmSignUp(email: String, code: String): EmptyResult<AuthError> {
        calls += "confirmSignUp($email, $code)"
        return Result.Success(Unit)
    }

    override suspend fun resendSignUpCode(email: String): EmptyResult<AuthError> {
        calls += "resendSignUpCode($email)"
        return Result.Success(Unit)
    }

    override suspend fun requestPasswordReset(email: String): EmptyResult<AuthError> {
        calls += "requestPasswordReset($email)"
        return requestPasswordResetResult
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): EmptyResult<AuthError> {
        calls += "resetPassword($email, $code)"
        return resetPasswordResult
    }
}
