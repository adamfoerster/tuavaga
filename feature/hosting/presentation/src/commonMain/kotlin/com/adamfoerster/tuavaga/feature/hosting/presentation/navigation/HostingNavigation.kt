package com.adamfoerster.tuavaga.feature.hosting.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.adamfoerster.tuavaga.feature.hosting.presentation.agenda.AgendaRoot
import com.adamfoerster.tuavaga.feature.hosting.presentation.requests.RequestsRoot
import com.adamfoerster.tuavaga.feature.hosting.presentation.wizard.SpotWizardRoot
import kotlinx.serialization.Serializable

/** Spot wizard: [spotId] edits, otherwise creates a spot (preselecting [condoId] when given). */
@Serializable
data class SpotWizardRoute(val spotId: String? = null, val condoId: String? = null)

/** Board 17 · Solicitações. */
@Serializable
data object RequestsRoute

/** Board 28 · Agenda da vaga. */
@Serializable
data class AgendaRoute(val spotId: String)

/**
 * Spot wizard, requests and agenda.
 * @param onOpenBooking opens a booking detail (another feature, wired by the app).
 */
fun NavGraphBuilder.hostingGraph(navController: NavController, onOpenBooking: (bookingId: String) -> Unit) {
    composable<SpotWizardRoute> { entry ->
        val route = entry.toRoute<SpotWizardRoute>()
        SpotWizardRoot(
            spotId = route.spotId,
            condoId = route.condoId,
            onDone = { navController.navigateUp() },
        )
    }
    composable<RequestsRoute> {
        RequestsRoot(onBack = { navController.navigateUp() }, onOpenBooking = onOpenBooking)
    }
    composable<AgendaRoute> { entry ->
        val route = entry.toRoute<AgendaRoute>()
        AgendaRoot(
            spotId = route.spotId,
            onBack = { navController.navigateUp() },
            onEdit = { navController.navigate(SpotWizardRoute(spotId = route.spotId)) },
            onRequests = { navController.navigate(RequestsRoute) },
            onOpenBooking = onOpenBooking,
        )
    }
}
