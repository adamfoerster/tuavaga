package com.adamfoerster.tuavaga.feature.onboarding.presentation.intro

sealed interface IntroAction {
    data object OnContinueClick : IntroAction
    data object OnSkipClick : IntroAction
}

sealed interface IntroEvent {
    /** The intro was marked as seen; move on to login. */
    data object Finished : IntroEvent
}
