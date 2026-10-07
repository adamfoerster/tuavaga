package com.adamfoerster.tuavaga.feature.home.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.adamfoerster.tuavaga.feature.home.presentation.HomeRoot
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeGraph() {
    composable<HomeRoute> {
        HomeRoot()
    }
}
