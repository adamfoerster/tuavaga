package com.adamfoerster.tuavaga.feature.notifications.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.adamfoerster.tuavaga.feature.notifications.presentation.NotificationsRoot
import kotlinx.serialization.Serializable

/** Board 26 · Notificações. */
@Serializable
data object NotificationsRoute

/** @param onOpenBooking opens the booking a notification is about (another feature, wired by the app). */
fun NavGraphBuilder.notificationsGraph(navController: NavController, onOpenBooking: (bookingId: String) -> Unit) {
    composable<NotificationsRoute> {
        NotificationsRoot(onBack = { navController.navigateUp() }, onOpenBooking = onOpenBooking)
    }
}
