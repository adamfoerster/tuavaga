package com.adamfoerster.tuavaga.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
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
import com.adamfoerster.tuavaga.core.designsystem.TuaVagaTheme
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.feature.auth.presentation.navigation.AuthGraph
import com.adamfoerster.tuavaga.feature.auth.presentation.navigation.authGraph
import com.adamfoerster.tuavaga.feature.home.presentation.navigation.HomeRoute
import com.adamfoerster.tuavaga.feature.home.presentation.navigation.homeGraph
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    TuaVagaTheme {
        val viewModel = koinViewModel<AppViewModel>()
        val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

        if (sessionState == SessionState.Loading) {
            Surface(Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        } else {
            AppNavHost(isSignedIn = sessionState is SessionState.SignedIn)
        }
    }
}

@Composable
private fun AppNavHost(isSignedIn: Boolean) {
    val navController = rememberNavController()
    // Decided once from the stored session; later changes are handled below and by the screens.
    val startDestination: Any = remember { if (isSignedIn) HomeRoute else AuthGraph }

    // Signing out (or the session expiring) from anywhere sends the user back to login.
    LaunchedEffect(isSignedIn) {
        if (!isSignedIn && !navController.isInAuthGraph()) {
            navController.navigate(AuthGraph) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
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

private fun NavHostController.isInAuthGraph(): Boolean =
    currentDestination?.hierarchy?.any { it.hasRoute(AuthGraph::class) } ?: true
