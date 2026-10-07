package com.adamfoerster.tuavaga.feature.auth.presentation.login

import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.auth.domain.AuthError
import com.adamfoerster.tuavaga.feature.auth.presentation.FakeAuthRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeAuthRepository()
        viewModel = LoginViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fillValidCredentials() {
        viewModel.onAction(LoginAction.OnEmailChange("morador@condominio.com"))
        viewModel.onAction(LoginAction.OnPasswordChange("segredo123"))
    }

    @Test
    fun invalidEmailShowsErrorWithoutCallingBackend() {
        viewModel.onAction(LoginAction.OnEmailChange("invalido"))
        viewModel.onAction(LoginAction.OnPasswordChange("segredo123"))
        viewModel.onAction(LoginAction.OnLoginClick)

        assertNotNull(viewModel.state.value.error)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun successfulLoginEmitsSignedIn() = runTest {
        fillValidCredentials()
        viewModel.onAction(LoginAction.OnLoginClick)

        assertEquals(LoginEvent.SignedIn, viewModel.events.first())
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun wrongPasswordShowsError() {
        repository.signInResult = Result.Failure(AuthError.INVALID_CREDENTIALS)
        fillValidCredentials()
        viewModel.onAction(LoginAction.OnLoginClick)

        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun unconfirmedEmailResendsCodeAndAsksForConfirmation() = runTest {
        repository.signInResult = Result.Failure(AuthError.EMAIL_NOT_CONFIRMED)
        fillValidCredentials()
        viewModel.onAction(LoginAction.OnLoginClick)

        assertEquals(LoginEvent.EmailNotConfirmed("morador@condominio.com"), viewModel.events.first())
        assertTrue("resendSignUpCode(morador@condominio.com)" in repository.calls)
    }

    @Test
    fun editingFieldClearsError() {
        viewModel.onAction(LoginAction.OnLoginClick)
        assertNotNull(viewModel.state.value.error)

        viewModel.onAction(LoginAction.OnEmailChange("a"))
        assertEquals(null, viewModel.state.value.error)
    }
}
