package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.join

import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.feature.onboarding.presentation.FakeActiveCondoRepository
import com.adamfoerster.tuavaga.feature.onboarding.presentation.FakeCondoRepository
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoOnboardingSession
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoTarget
import com.adamfoerster.tuavaga.feature.onboarding.presentation.preview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class JoinCondoViewModelTest {

    private lateinit var repository: FakeCondoRepository
    private lateinit var active: FakeActiveCondoRepository
    private lateinit var session: CondoOnboardingSession
    private lateinit var viewModel: JoinCondoViewModel

    @BeforeTest
    fun setUp() {
        // Standard dispatcher: the search is debounced, so tests drive virtual time.
        Dispatchers.setMain(StandardTestDispatcher())
        repository = FakeCondoRepository()
        active = FakeActiveCondoRepository()
        session = CondoOnboardingSession()
        viewModel = JoinCondoViewModel(repository, active, session)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun searchIsDebouncedToTheLastQuery() = runTest {
        viewModel.onAction(JoinCondoAction.OnQueryChange("Al"))
        advanceTimeBy(100)
        viewModel.onAction(JoinCondoAction.OnQueryChange("Alameda"))
        advanceUntilIdle()

        assertEquals(listOf("Alameda"), repository.searches)
        assertEquals(1, viewModel.state.value.results.size)
        assertTrue(viewModel.state.value.hasSearched)
    }

    @Test
    fun oneLetterDoesNotSearch() = runTest {
        viewModel.onAction(JoinCondoAction.OnQueryChange("A"))
        advanceUntilIdle()

        assertTrue(repository.searches.isEmpty())
    }

    @Test
    fun malformedInviteCodeIsRejectedLocally() = runTest {
        viewModel.onAction(JoinCondoAction.OnInviteCodeChange("AV-4K"))
        viewModel.onAction(JoinCondoAction.OnContinueClick)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.inviteError)
        assertTrue(repository.inviteLookups.isEmpty())
    }

    @Test
    fun unknownInviteCodeShowsNotFound() = runTest {
        repository.inviteResult = Result.Success(null)
        viewModel.onAction(JoinCondoAction.OnInviteCodeChange("av4k7x"))
        viewModel.onAction(JoinCondoAction.OnContinueClick)
        advanceUntilIdle()

        assertEquals(listOf("AV-4K7X"), repository.inviteLookups)
        assertEquals(JoinTexts.codeNotFound, viewModel.state.value.inviteError)
        assertNull(session.target)
    }

    @Test
    fun validInviteCodeGoesToResidentData() = runTest {
        repository.inviteResult = Result.Success(preview(id = "c9"))
        viewModel.onAction(JoinCondoAction.OnInviteCodeChange("AV-4K7Q"))
        viewModel.onAction(JoinCondoAction.OnContinueClick)

        assertEquals(JoinCondoEvent.GoToResidentData, viewModel.events.first())
        assertEquals("c9", assertIs<CondoTarget.Existing>(session.target).preview.id)
    }

    @Test
    fun selectedSearchResultGoesToResidentData() = runTest {
        viewModel.onAction(JoinCondoAction.OnQueryChange("Alameda"))
        advanceUntilIdle()
        viewModel.onAction(JoinCondoAction.OnSelect("c1"))
        viewModel.onAction(JoinCondoAction.OnContinueClick)

        assertEquals(JoinCondoEvent.GoToResidentData, viewModel.events.first())
        assertEquals("c1", assertIs<CondoTarget.Existing>(session.target).preview.id)
    }

    @Test
    fun condoTheUserAlreadyBelongsToJustBecomesActive() = runTest {
        repository.searchResult = Result.Success(listOf(preview(id = "c1", isMember = true)))
        viewModel.onAction(JoinCondoAction.OnQueryChange("Alameda"))
        advanceUntilIdle()
        viewModel.onAction(JoinCondoAction.OnSelect("c1"))
        viewModel.onAction(JoinCondoAction.OnContinueClick)

        assertEquals(JoinCondoEvent.Finished, viewModel.events.first())
        assertEquals("c1", active.activeCondoId.value)
        assertTrue(repository.joins.isEmpty())
    }

    @Test
    fun continueWithoutChoiceAsksForOne() = runTest {
        viewModel.onAction(JoinCondoAction.OnContinueClick)
        advanceUntilIdle()

        assertEquals(JoinTexts.chooseCondo, viewModel.state.value.error)
    }
}
