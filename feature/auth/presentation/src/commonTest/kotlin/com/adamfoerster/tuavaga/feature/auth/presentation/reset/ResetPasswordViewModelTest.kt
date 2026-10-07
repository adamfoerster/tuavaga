package com.adamfoerster.tuavaga.feature.auth.presentation.reset

import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.auth.domain.AuthError
import com.adamfoerster.tuavaga.feature.auth.presentation.FakeAuthRepository
import com.adamfoerster.tuavaga.feature.auth.presentation.reset.ResetPasswordState.Step
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ResetPasswordViewModelTest {

    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: ResetPasswordViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeAuthRepository()
        viewModel = ResetPasswordViewModel("morador@condominio.com", repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun prefillsEmailFromRoute() {
        assertEquals("morador@condominio.com", viewModel.state.value.email)
        assertEquals(Step.REQUEST_CODE, viewModel.state.value.step)
    }

    @Test
    fun sendingCodeMovesToNewPasswordStep() {
        viewModel.onAction(ResetPasswordAction.OnSendCodeClick)

        assertEquals(Step.SET_NEW_PASSWORD, viewModel.state.value.step)
        assertTrue("requestPasswordReset(morador@condominio.com)" in repository.calls)
    }

    @Test
    fun codeInputKeepsOnlyOtpLengthDigits() {
        viewModel.onAction(ResetPasswordAction.OnCodeChange("12a3456789"))
        assertEquals("12345678", viewModel.state.value.code)
    }

    @Test
    fun mismatchedPasswordsAreRejectedLocally() {
        viewModel.onAction(ResetPasswordAction.OnSendCodeClick)
        viewModel.onAction(ResetPasswordAction.OnCodeChange("12345678"))
        viewModel.onAction(ResetPasswordAction.OnNewPasswordChange("novaSenha1"))
        viewModel.onAction(ResetPasswordAction.OnNewPasswordConfirmationChange("outraSenha"))
        viewModel.onAction(ResetPasswordAction.OnResetClick)

        assertNotNull(viewModel.state.value.error)
        assertTrue(repository.calls.none { it.startsWith("resetPassword") })
    }

    @Test
    fun validResetEmitsPasswordReset() = runTest {
        viewModel.onAction(ResetPasswordAction.OnSendCodeClick)
        viewModel.onAction(ResetPasswordAction.OnCodeChange("12345678"))
        viewModel.onAction(ResetPasswordAction.OnNewPasswordChange("novaSenha1"))
        viewModel.onAction(ResetPasswordAction.OnNewPasswordConfirmationChange("novaSenha1"))
        viewModel.onAction(ResetPasswordAction.OnResetClick)

        assertEquals(ResetPasswordEvent.PasswordReset, viewModel.events.first())
    }

    @Test
    fun expiredCodeShowsError() {
        repository.resetPasswordResult = Result.Failure(AuthError.INVALID_OR_EXPIRED_CODE)
        viewModel.onAction(ResetPasswordAction.OnSendCodeClick)
        viewModel.onAction(ResetPasswordAction.OnCodeChange("12345678"))
        viewModel.onAction(ResetPasswordAction.OnNewPasswordChange("novaSenha1"))
        viewModel.onAction(ResetPasswordAction.OnNewPasswordConfirmationChange("novaSenha1"))
        viewModel.onAction(ResetPasswordAction.OnResetClick)

        assertNotNull(viewModel.state.value.error)
    }
}
