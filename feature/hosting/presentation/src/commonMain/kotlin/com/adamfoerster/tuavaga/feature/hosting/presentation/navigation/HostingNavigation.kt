package com.adamfoerster.tuavaga.feature.hosting.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.adamfoerster.tuavaga.feature.hosting.presentation.wizard.SpotWizardRoot
import kotlinx.serialization.Serializable

/** Spot wizard: [spotId] edits, otherwise creates a spot (preselecting [condoId] when given). */
@Serializable
data class SpotWizardRoute(val spotId: String? = null, val condoId: String? = null)

fun NavGraphBuilder.hostingGraph(navController: NavController) {
    composable<SpotWizardRoute> { entry ->
        val route = entry.toRoute<SpotWizardRoute>()
        SpotWizardRoot(
            spotId = route.spotId,
            condoId = route.condoId,
            onDone = { navController.navigateUp() },
        )
    }
}
