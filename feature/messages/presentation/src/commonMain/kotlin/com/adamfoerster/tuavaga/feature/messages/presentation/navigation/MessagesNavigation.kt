package com.adamfoerster.tuavaga.feature.messages.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.adamfoerster.tuavaga.feature.messages.presentation.chat.ChatRoot
import com.adamfoerster.tuavaga.feature.messages.presentation.chat.ChatViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Board 23 · Chat da reserva. */
@Serializable
data class ChatRoute(val bookingId: String)

/**
 * The booking chat.
 * @param onOpenBooking opens the booking detail (another feature, wired by the app).
 */
fun NavGraphBuilder.messagesGraph(navController: NavController, onOpenBooking: (bookingId: String) -> Unit) {
    composable<ChatRoute> { entry ->
        val route = entry.toRoute<ChatRoute>()
        ChatRoot(
            viewModel = koinViewModel<ChatViewModel> { parametersOf(route.bookingId) },
            onBack = { navController.navigateUp() },
            onOpenBooking = { onOpenBooking(route.bookingId) },
        )
    }
}
