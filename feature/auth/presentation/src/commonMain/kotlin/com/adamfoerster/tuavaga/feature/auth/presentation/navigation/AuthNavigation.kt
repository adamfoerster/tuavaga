package com.adamfoerster.tuavaga.feature.auth.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.adamfoerster.tuavaga.feature.auth.presentation.confirm.ConfirmEmailRoot
import com.adamfoerster.tuavaga.feature.auth.presentation.login.LoginRoot
import com.adamfoerster.tuavaga.feature.auth.presentation.register.RegisterRoot
import com.adamfoerster.tuavaga.feature.auth.presentation.reset.ResetPasswordRoot
import kotlinx.serialization.Serializable

@Serializable
data object AuthGraph

@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

@Serializable
data class ConfirmEmailRoute(val email: String)

@Serializable
data class ResetPasswordRoute(val email: String = "")

/**
 * @param onAuthenticated called once the user is signed in; the caller leaves the auth graph.
 */
fun NavGraphBuilder.authGraph(
    navController: NavController,
    onAuthenticated: () -> Unit,
) {
    navigation<AuthGraph>(startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginRoot(
                onSignedIn = onAuthenticated,
                onEmailNotConfirmed = { email -> navController.navigate(ConfirmEmailRoute(email)) },
                onRegisterClick = {
                    navController.navigate(RegisterRoute) { launchSingleTop = true }
                },
                onForgotPasswordClick = { email -> navController.navigate(ResetPasswordRoute(email)) },
            )
        }
        composable<RegisterRoute> {
            RegisterRoot(
                onSignedIn = onAuthenticated,
                onConfirmationRequired = { email ->
                    navController.navigate(ConfirmEmailRoute(email)) {
                        popUpTo<RegisterRoute> { inclusive = true }
                    }
                },
                onBackToLogin = { navController.navigateUp() },
            )
        }
        composable<ConfirmEmailRoute> { entry ->
            ConfirmEmailRoot(
                email = entry.toRoute<ConfirmEmailRoute>().email,
                onSignedIn = onAuthenticated,
                onBack = { navController.navigateUp() },
            )
        }
        composable<ResetPasswordRoute> { entry ->
            ResetPasswordRoot(
                email = entry.toRoute<ResetPasswordRoute>().email,
                onPasswordReset = onAuthenticated,
                onBack = { navController.navigateUp() },
            )
        }
    }
}
