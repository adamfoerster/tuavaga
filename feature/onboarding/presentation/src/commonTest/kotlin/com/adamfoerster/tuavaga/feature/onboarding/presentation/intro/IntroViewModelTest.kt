package com.adamfoerster.tuavaga.feature.onboarding.presentation.intro

import com.adamfoerster.tuavaga.feature.onboarding.presentation.FakeAppPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IntroViewModelTest {

    private lateinit var preferences: FakeAppPreferencesRepository
    private lateinit var viewModel: IntroViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        preferences = FakeAppPreferencesRepository()
        viewModel = IntroViewModel(preferences)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun continueMarksIntroSeenAndFinishes() = runTest {
        viewModel.onAction(IntroAction.OnContinueClick)

        assertEquals(IntroEvent.Finished, viewModel.events.first())
        assertTrue(preferences.introSeen.value)
    }

    @Test
    fun skipMarksIntroSeenAndFinishes() = runTest {
        viewModel.onAction(IntroAction.OnSkipClick)

        assertEquals(IntroEvent.Finished, viewModel.events.first())
        assertTrue(preferences.introSeen.value)
    }

    @Test
    fun repeatedTapsSaveOnlyOnce() = runTest {
        viewModel.onAction(IntroAction.OnContinueClick)
        viewModel.onAction(IntroAction.OnSkipClick)
        viewModel.events.first()

        assertEquals(1, preferences.markIntroSeenCalls)
    }
}
