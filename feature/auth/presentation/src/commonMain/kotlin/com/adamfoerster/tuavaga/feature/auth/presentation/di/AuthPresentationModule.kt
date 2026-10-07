package com.adamfoerster.tuavaga.feature.auth.presentation.di

import com.adamfoerster.tuavaga.feature.auth.presentation.confirm.ConfirmEmailViewModel
import com.adamfoerster.tuavaga.feature.auth.presentation.login.LoginViewModel
import com.adamfoerster.tuavaga.feature.auth.presentation.register.RegisterViewModel
import com.adamfoerster.tuavaga.feature.auth.presentation.reset.ResetPasswordViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    // The e-mail comes from the route (see AuthNavigation) via parametersOf.
    viewModel { (email: String) -> ConfirmEmailViewModel(email, get()) }
    viewModel { (email: String) -> ResetPasswordViewModel(email, get()) }
}
