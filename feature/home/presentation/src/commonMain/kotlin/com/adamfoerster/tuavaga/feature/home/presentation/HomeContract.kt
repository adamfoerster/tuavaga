package com.adamfoerster.tuavaga.feature.home.presentation

data class HomeState(
    val userName: String? = null,
    val userEmail: String = "",
    val isSigningOut: Boolean = false,
)

sealed interface HomeAction {
    data object OnSignOutClick : HomeAction
}
