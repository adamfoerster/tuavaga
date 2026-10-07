package com.adamfoerster.tuavaga.feature.auth.domain.validation

/** Client-side checks; the server is still the source of truth. */
object CredentialsValidator {
    /** Supabase's default minimum; keep in sync with Auth > Providers > Email settings. */
    const val MIN_PASSWORD_LENGTH = 6
    const val OTP_LENGTH = 8

    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    fun isValidPassword(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH

    fun isValidOtp(code: String): Boolean = code.length == OTP_LENGTH && code.all(Char::isDigit)
}
