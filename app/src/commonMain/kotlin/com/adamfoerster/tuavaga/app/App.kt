package com.adamfoerster.tuavaga.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.adamfoerster.tuavaga.app.shell.MainShellRoot
import com.adamfoerster.tuavaga.app.shell.MainTab
import com.adamfoerster.tuavaga.app.shell.ShellNavigation
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.TuaVagaTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.feature.auth.presentation.navigation.AuthGraph
import com.adamfoerster.tuavaga.feature.auth.presentation.navigation.authGraph
import com.adamfoerster.tuavaga.feature.bookings.presentation.navigation.BookingDetailRoute
import com.adamfoerster.tuavaga.feature.bookings.presentation.navigation.bookingsGraph
import com.adamfoerster.tuavaga.feature.explore.presentation.navigation.exploreGraph
import com.adamfoerster.tuavaga.feature.explore.presentation.navigation.spotDetailRoute
import com.adamfoerster.tuavaga.feature.hosting.presentation.navigation.AgendaRoute
import com.adamfoerster.tuavaga.feature.hosting.presentation.navigation.RequestsRoute
import com.adamfoerster.tuavaga.feature.hosting.presentation.navigation.SpotWizardRoute
import com.adamfoerster.tuavaga.feature.hosting.presentation.navigation.hostingGraph
import com.adamfoerster.tuavaga.feature.onboarding.presentation.navigation.CondoOnboardingGraph
import com.adamfoerster.tuavaga.feature.onboarding.presentation.navigation.IntroRoute
import com.adamfoerster.tuavaga.feature.onboarding.presentation.navigation.condoOnboardingGraph
import com.adamfoerster.tuavaga.feature.onboarding.presentation.navigation.introGraph
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
private data object MainRoute

/**
 * Root. The app is split in areas (signed out, onboarding, main), each with its own NavHost;
 * the area follows the session and the user's condominiums, so signing in, finishing the first
 * onboarding or signing out need no navigation calls.
 */
@Composable
fun App() {
    TuaVagaTheme {
        val viewModel = koinViewModel<AppViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        when (state.area()) {
            AppArea.LOADING -> LoadingScreen()
            AppArea.SIGNED_OUT -> SignedOutArea(startWithIntro = state.signedOutStart() == StartDestination.INTRO)
            AppArea.MEMBERSHIP_ERROR -> MembershipErrorScreen(
                onRetry = viewModel::retryMemberships,
                onSignOut = viewModel::signOut,
            )
            AppArea.ONBOARDING -> OnboardingArea(
                // Normally the new membership already switched to MAIN; this covers a failed refresh.
                onFinished = viewModel::retryMemberships,
                onExit = viewModel::signOut,
            )
            AppArea.MAIN -> MainArea()
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(KerbTheme.colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = KerbTheme.colors.apex)
    }
}

@Composable
private fun SignedOutArea(startWithIntro: Boolean) {
    val navController = rememberNavController()
    // Decided once when the area opens; finishing the intro navigates to login below.
    val start: Any = remember { if (startWithIntro) IntroRoute else AuthGraph }
    NavHost(navController = navController, startDestination = start) {
        introGraph(
            onIntroFinished = {
                navController.navigate(AuthGraph) { popUpTo<IntroRoute> { inclusive = true } }
            },
        )
        // Signing in changes the session, which moves the app to another area.
        authGraph(navController = navController, onAuthenticated = {})
    }
}

@Composable
private fun OnboardingArea(onFinished: () -> Unit, onExit: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = CondoOnboardingGraph) {
        // Leaving the first step signs out (back to login): there is no app without a condominium.
        condoOnboardingGraph(navController = navController, onFinished = onFinished, onExit = onExit)
    }
}

@Composable
private fun MainArea() {
    val navController = rememberNavController()
    // Set by screens outside the shell that send the user to one of its tabs.
    var requestedTab by remember { mutableStateOf<MainTab?>(null) }
    val openBooking = { bookingId: String -> navController.navigate(BookingDetailRoute(bookingId)) }
    val backToExplore = {
        navController.popBackStack(MainRoute, inclusive = false)
        requestedTab = MainTab.EXPLORE
    }
    NavHost(navController = navController, startDestination = MainRoute) {
        composable<MainRoute> {
            MainShellRoot(
                navigation = ShellNavigation(
                    onAddCondo = { navController.navigate(CondoOnboardingGraph) },
                    onCreateSpot = { condoId -> navController.navigate(SpotWizardRoute(condoId = condoId)) },
                    onEditSpot = { spotId -> navController.navigate(SpotWizardRoute(spotId = spotId)) },
                    onOpenSpot = { condoId, spotId, period -> navController.navigate(spotDetailRoute(condoId, spotId, period)) },
                    onOpenBooking = openBooking,
                    onRequests = { navController.navigate(RequestsRoute) },
                    onAgenda = { spotId -> navController.navigate(AgendaRoute(spotId)) },
                ),
                requestedTab = requestedTab,
                onTabRequestHandled = { requestedTab = null },
            )
        }
        hostingGraph(navController, onOpenBooking = openBooking)
        bookingsGraph(navController, onExplore = backToExplore)
        exploreGraph(
            navController,
            onFinished = { navController.popBackStack(MainRoute, inclusive = false) },
            onViewBooking = { bookingId ->
                navController.navigate(BookingDetailRoute(bookingId)) { popUpTo<MainRoute>() }
            },
        )
        condoOnboardingGraph(
            navController = navController,
            onFinished = { navController.popBackStack(MainRoute, inclusive = false) },
            onExit = { navController.navigateUp() },
        )
    }
}

/** Edge state "Sem conexão" (board 17) when the first load of the condominiums fails. */
@Composable
private fun MembershipErrorScreen(onRetry: () -> Unit, onSignOut: () -> Unit) {
    KbScreen(
        showZebra = true,
        bottomBar = {
            KbButton(text = "Tentar de novo", onClick = onRetry, size = KbButtonSize.Large, modifier = Modifier.fillMaxWidth())
            KbButton(
                text = "Sair da conta",
                onClick = onSignOut,
                variant = KbButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        KbPanel(title = "Sem conexão") {
            KbTag("Sem conexão", tone = KbTone.Danger)
            KbText("Sinal perdido", KerbTheme.typography.md)
            KbText(
                text = "Não deu para carregar seus condomínios. Confira a internet e tente de novo.",
                style = KerbTheme.typography.body,
                color = KerbTheme.colors.inkMuted,
            )
        }
    }
}
