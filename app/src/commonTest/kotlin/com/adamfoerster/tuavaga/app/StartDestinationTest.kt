package com.adamfoerster.tuavaga.app

import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.user.User
import kotlin.test.Test
import kotlin.test.assertEquals

class StartDestinationTest {

    private val signedIn = SessionState.SignedIn(User(id = "u1", email = "morador@condominio.com", fullName = "Morador"))

    @Test
    fun firstLaunchShowsIntro() {
        assertEquals(StartDestination.INTRO, AppState(SessionState.SignedOut, introSeen = false).startDestination())
    }

    @Test
    fun signedOutAfterIntroGoesToLogin() {
        assertEquals(StartDestination.AUTH, AppState(SessionState.SignedOut, introSeen = true).startDestination())
    }

    @Test
    fun signedInSkipsIntroEvenIfNeverSeen() {
        assertEquals(StartDestination.HOME, AppState(signedIn, introSeen = false).startDestination())
    }
}
