package com.adamfoerster.tuavaga.feature.explore.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.presentation.detail.SpotDetailRoot
import com.adamfoerster.tuavaga.feature.explore.presentation.detail.SpotDetailViewModel
import com.adamfoerster.tuavaga.feature.explore.presentation.request.BookingRequestRoot
import com.adamfoerster.tuavaga.feature.explore.presentation.request.BookingRequestViewModel
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Period travels as ISO local date-times ("2026-10-10T08:00"), Brasília time. */
@Serializable
data class SpotDetailRoute(val condoId: String, val spotId: String, val start: String, val end: String)

@Serializable
data class BookingRequestRoute(val condoId: String, val spotId: String, val start: String, val end: String)

fun spotDetailRoute(condoId: String, spotId: String, period: BookingPeriod) =
    SpotDetailRoute(condoId, spotId, period.start.toString(), period.end.toString())

private fun period(start: String, end: String) = BookingPeriod(LocalDateTime.parse(start), LocalDateTime.parse(end))

/**
 * Spot detail and booking request (boards 07–09).
 * @param onFinished a booking was sent; the caller goes back to the main screens.
 */
fun NavGraphBuilder.exploreGraph(navController: NavController, onFinished: () -> Unit) {
    composable<SpotDetailRoute> { entry ->
        val route = entry.toRoute<SpotDetailRoute>()
        val viewModel = koinViewModel<SpotDetailViewModel> {
            parametersOf(route.condoId, route.spotId, period(route.start, route.end))
        }
        SpotDetailRoot(
            viewModel = viewModel,
            onBack = { navController.navigateUp() },
            onRequest = {
                navController.navigate(BookingRequestRoute(route.condoId, route.spotId, route.start, route.end))
            },
        )
    }
    composable<BookingRequestRoute> { entry ->
        val route = entry.toRoute<BookingRequestRoute>()
        val viewModel = koinViewModel<BookingRequestViewModel> {
            parametersOf(route.condoId, route.spotId, period(route.start, route.end))
        }
        BookingRequestRoot(
            viewModel = viewModel,
            onExit = { navController.navigateUp() },
            onFinished = onFinished,
        )
    }
}
