package com.adamfoerster.tuavaga.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.TuaVagaTheme
import com.adamfoerster.tuavaga.feature.auth.presentation.navigation.AuthGraph
import com.adamfoerster.tuavaga.feature.auth.presentation.navigation.authGraph
import com.adamfoerster.tuavaga.feature.home.presentation.navigation.HomeRoute
import com.adamfoerster.tuavaga.feature.home.presentation.navigation.homeGraph
import com.adamfoerster.tuavaga.feature.onboarding.presentation.navigation.IntroRoute
import com.adamfoerster.tuavaga.feature.onboarding.presentation.navigation.onboardingGraph
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    TuaVagaTheme {
        val viewModel = koinViewModel<AppViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(KerbTheme.colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = KerbTheme.colors.apex)
            }
        } else {
            AppNavHost(state)
        }
    }
}

@Composable
private fun AppNavHost(state: AppState) {
    val navController = rememberNavController()
    // Decided once from the stored session; later changes are handled below and by the screens.
    val startDestination: Any = remember {
        when (state.startDestination()) {
            StartDestination.HOME -> HomeRoute
            StartDestination.INTRO -> IntroRoute
            StartDestination.AUTH -> AuthGraph
        }
    }

    // Signing out (or the session expiring) from anywhere sends the user back to login.
    LaunchedEffect(state.isSignedIn) {
        if (!state.isSignedIn && !navController.isSignedOutArea()) {
            navController.navigate(AuthGraph) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        onboardingGraph(
            onIntroFinished = {
                navController.navigate(AuthGraph) {
                    popUpTo<IntroRoute> { inclusive = true }
                }
            },
        )
        authGraph(
            navController = navController,
            onAuthenticated = {
                navController.navigate(HomeRoute) {
                    popUpTo<AuthGraph> { inclusive = true }
                }
            },
        )
        homeGraph()
    }
}

/** Intro and auth screens are the only places a signed-out user may be. */
private fun NavHostController.isSignedOutArea(): Boolean =
    currentDestination?.hierarchy?.any { it.hasRoute(AuthGraph::class) || it.hasRoute(IntroRoute::class) }
        ?: true
