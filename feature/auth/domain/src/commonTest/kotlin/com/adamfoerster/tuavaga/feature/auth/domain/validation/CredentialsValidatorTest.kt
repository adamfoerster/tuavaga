package com.adamfoerster.tuavaga.feature.auth.domain.validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CredentialsValidatorTest {

    @Test
    fun acceptsWellFormedEmails() {
        assertTrue(CredentialsValidator.isValidEmail("morador@condominio.com.br"))
        assertTrue(CredentialsValidator.isValidEmail("  a@b.co  "))
    }

    @Test
    fun rejectsMalformedEmails() {
        assertFalse(CredentialsValidator.isValidEmail(""))
        assertFalse(CredentialsValidator.isValidEmail("sem-arroba.com"))
        assertFalse(CredentialsValidator.isValidEmail("a@b"))
        assertFalse(CredentialsValidator.isValidEmail("a b@c.com"))
    }

    @Test
    fun passwordNeedsMinimumLength() {
        assertFalse(CredentialsValidator.isValidPassword("12345"))
        assertTrue(CredentialsValidator.isValidPassword("123456"))
    }

    @Test
    fun otpMustBeEightDigits() {
        assertTrue(CredentialsValidator.isValidOtp("01234567"))
        assertFalse(CredentialsValidator.isValidOtp("1234567"))
        assertFalse(CredentialsValidator.isValidOtp("12a45678"))
    }
}
