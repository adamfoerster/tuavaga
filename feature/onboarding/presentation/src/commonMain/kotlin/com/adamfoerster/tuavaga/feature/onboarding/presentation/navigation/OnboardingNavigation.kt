package com.adamfoerster.tuavaga.feature.onboarding.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.create.CreateCondoRoot
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.join.JoinCondoRoot
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.resident.ResidentDataRoot
import com.adamfoerster.tuavaga.feature.onboarding.presentation.intro.IntroRoot
import kotlinx.serialization.Serializable

@Serializable
data object IntroRoute

@Serializable
data object CondoOnboardingGraph

@Serializable
data object JoinCondoRoute

@Serializable
data object CreateCondoRoute

@Serializable
data object ResidentDataRoute

/** @param onIntroFinished called after the intro is marked as seen; the caller moves on to login. */
fun NavGraphBuilder.introGraph(onIntroFinished: () -> Unit) {
    composable<IntroRoute> {
        IntroRoot(onFinished = onIntroFinished)
    }
}

/**
 * Join or create a condominium (boards 02, 03 and 20).
 *
 * @param onFinished the user now has the chosen condominium as the active one.
 * @param onExit leaves the flow from its first screen; `null` on the first onboarding, where there
 *   is nowhere to go back to (the back button is hidden).
 */
fun NavGraphBuilder.condoOnboardingGraph(
    navController: NavController,
    onFinished: () -> Unit,
    onExit: (() -> Unit)?,
) {
    navigation<CondoOnboardingGraph>(startDestination = JoinCondoRoute) {
        composable<JoinCondoRoute> {
            JoinCondoRoot(
                onResidentData = { navController.navigate(ResidentDataRoute) },
                onCreateCondo = { navController.navigate(CreateCondoRoute) },
                onFinished = onFinished,
                onBack = onExit,
            )
        }
        composable<CreateCondoRoute> {
            CreateCondoRoot(
                onResidentData = { navController.navigate(ResidentDataRoute) },
                onBack = { navController.navigateUp() },
            )
        }
        composable<ResidentDataRoute> {
            ResidentDataRoot(
                onFinished = onFinished,
                onBack = { navController.navigateUp() },
            )
        }
    }
}
