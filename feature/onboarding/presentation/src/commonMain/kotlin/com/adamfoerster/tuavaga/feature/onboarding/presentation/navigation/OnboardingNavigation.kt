package com.adamfoerster.tuavaga.feature.onboarding.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.adamfoerster.tuavaga.feature.onboarding.presentation.intro.IntroRoot
import kotlinx.serialization.Serializable

@Serializable
data object IntroRoute

/** @param onIntroFinished called after the intro is marked as seen; the caller moves on to login. */
fun NavGraphBuilder.onboardingGraph(onIntroFinished: () -> Unit) {
    composable<IntroRoute> {
        IntroRoot(onFinished = onIntroFinished)
    }
}
