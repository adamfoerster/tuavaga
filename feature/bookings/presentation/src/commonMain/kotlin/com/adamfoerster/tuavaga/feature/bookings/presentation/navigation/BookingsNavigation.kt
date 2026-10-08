package com.adamfoerster.tuavaga.feature.bookings.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.adamfoerster.tuavaga.feature.bookings.presentation.check.CheckKind
import com.adamfoerster.tuavaga.feature.bookings.presentation.check.CheckRoot
import com.adamfoerster.tuavaga.feature.bookings.presentation.check.CheckViewModel
import com.adamfoerster.tuavaga.feature.bookings.presentation.detail.BookingDetailRoot
import com.adamfoerster.tuavaga.feature.bookings.presentation.detail.BookingDetailViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class BookingDetailRoute(val bookingId: String)

@Serializable
data class CheckInRoute(val bookingId: String)

@Serializable
data class CheckOutRoute(val bookingId: String)

/**
 * Booking detail, check-in and check-out (boards 10, 24, 25).
 * @param onExplore look for another spot (rejected, cancelled or expired booking).
 * @param onOpenChat opens the booking chat (another feature, wired by the app).
 */
fun NavGraphBuilder.bookingsGraph(
    navController: NavController,
    onExplore: () -> Unit,
    onOpenChat: (bookingId: String) -> Unit,
) {
    composable<BookingDetailRoute> { entry ->
        val route = entry.toRoute<BookingDetailRoute>()
        BookingDetailRoot(
            viewModel = koinViewModel<BookingDetailViewModel> { parametersOf(route.bookingId) },
            onBack = { navController.navigateUp() },
            onCheckIn = { navController.navigate(CheckInRoute(route.bookingId)) },
            onCheckOut = { navController.navigate(CheckOutRoute(route.bookingId)) },
            onExplore = onExplore,
            onMessage = { onOpenChat(route.bookingId) },
        )
    }
    composable<CheckInRoute> { entry ->
        val route = entry.toRoute<CheckInRoute>()
        CheckRoot(
            viewModel = koinViewModel<CheckViewModel> { parametersOf(CheckKind.IN, route.bookingId) },
            onBack = { navController.navigateUp() },
        )
    }
    composable<CheckOutRoute> { entry ->
        val route = entry.toRoute<CheckOutRoute>()
        CheckRoot(
            viewModel = koinViewModel<CheckViewModel> { parametersOf(CheckKind.OUT, route.bookingId) },
            onBack = { navController.navigateUp() },
        )
    }
}
