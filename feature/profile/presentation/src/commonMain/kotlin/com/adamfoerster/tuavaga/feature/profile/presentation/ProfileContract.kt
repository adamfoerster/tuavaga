package com.adamfoerster.tuavaga.feature.profile.presentation

data class ProfileState(
    val userName: String? = null,
    val userEmail: String = "",
    val isSigningOut: Boolean = false,
)

sealed interface ProfileAction {
    data object OnSignOutClick : ProfileAction
}
