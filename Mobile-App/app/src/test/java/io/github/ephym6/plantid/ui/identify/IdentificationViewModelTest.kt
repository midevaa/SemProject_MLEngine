package io.github.ephym6.plantid.ui.identify

import androidx.lifecycle.SavedStateHandle
import io.github.ephym6.plantid.domain.identification.ClassScore
import io.github.ephym6.plantid.domain.identification.FailureReason
import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import io.github.ephym6.plantid.domain.identification.IdentificationService
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.domain.identification.PlantSummary
import io.github.ephym6.plantid.ui.identify.IdentificationViewModel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.github.ephym6.plantid.testing.FakeHistoryRepository
import java.io.File
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class IdentificationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val photo = File("unused_by_fake.jpg")
    private val model = ModelInfo(name = "Fake", version = "test", isDemo = false)
    private val unsupported = IdentificationOutcome.Unsupported(model)
    private val history = FakeHistoryRepository()
    private val now = Instant.parse("2026-11-19T10:15:30Z")
    private var nextId = 0
    private fun newViewModel(service: IdentificationService) = IdentificationViewModel(
        photo = photo,
        service = service,
        history = history,
        savedState = SavedStateHandle(),
        clock = { now },
        newEventId = { "event-${++nextId}" },
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeService(
        override val modelInfo: ModelInfo,
        var behaviour: suspend (File) -> IdentificationOutcome,
    ) : IdentificationService {
        var calls = 0
        override suspend fun identify(photo: File): IdentificationOutcome {
            calls++
            return behaviour(photo)
        }
    }

    private fun failed(reason: FailureReason) = UiState.Finished(IdentificationOutcome.Failed(model, reason))

    @Test
    fun `starts identifying immediately and shows the service outcome`() = runTest(dispatcher) {
        val service = FakeService(model) { delay(500); unsupported }
        val viewModel = IdentificationViewModel(photo, service, history, SavedStateHandle())

        assertEquals(UiState.Identifying, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(UiState.Finished(unsupported), viewModel.uiState.value)
        assertEquals(1, service.calls)
    }

    @Test
    fun `missing photo fails without calling the service`() = runTest(dispatcher) {
        val service = FakeService(model) { unsupported }
        val viewModel = IdentificationViewModel(photo = null, service = service, history = history, savedState = SavedStateHandle())
        advanceUntilIdle()

        assertEquals(failed(FailureReason.PhotoUnavailable), viewModel.uiState.value)
        assertEquals(0, service.calls)
    }

    @Test
    fun `an exception becomes a processing failure, not a crash or fake success`() = runTest(dispatcher) {
        val service = FakeService(model) { error("inference exploded") }
        val viewModel = IdentificationViewModel(photo, service, history, SavedStateHandle())
        advanceUntilIdle()

        assertEquals(failed(FailureReason.ProcessingFailed), viewModel.uiState.value)
    }

    @Test
    fun `a request that never finishes times out`() = runTest(dispatcher) {
        val service = FakeService(model) { awaitCancellation() }
        val viewModel = IdentificationViewModel(photo, service, history, SavedStateHandle(), timeoutMillis = 1_000)

        advanceTimeBy(999)
        runCurrent()
        assertEquals(UiState.Identifying, viewModel.uiState.value)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(failed(FailureReason.TimedOut), viewModel.uiState.value)
    }

    @Test
    fun `retry while identifying is ignored`() = runTest(dispatcher) {
        val service = FakeService(model) { delay(500); unsupported }
        val viewModel = IdentificationViewModel(photo, service, history, SavedStateHandle())

        viewModel.retry()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(1, service.calls)
    }

    @Test
    fun `retry after a failure runs again and can succeed`() = runTest(dispatcher) {
        val service = FakeService(model) { IdentificationOutcome.Failed(model, FailureReason.ProcessingFailed) }
        val viewModel = IdentificationViewModel(photo, service, history, SavedStateHandle())
        advanceUntilIdle()

        service.behaviour = { delay(100); unsupported }
        viewModel.retry()
        assertEquals(UiState.Identifying, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(UiState.Finished(unsupported), viewModel.uiState.value)
        assertEquals(2, service.calls)
    }

    // ---- Saving to history ----

    @Test
    fun `a finished result can be saved, a failure can't`() = runTest(dispatcher) {
        val viewModel = newViewModel(FakeService(model) { unsupported })
        assertEquals(SaveState.NotSaveable, viewModel.saveState.value)
        advanceUntilIdle()
        assertEquals(SaveState.NotSaved, viewModel.saveState.value)

        val failing = newViewModel(FakeService(model) { IdentificationOutcome.Failed(model, FailureReason.ProcessingFailed) })
        advanceUntilIdle()
        assertEquals(SaveState.NotSaveable, failing.saveState.value)
        failing.save()
        advanceUntilIdle()
        assertTrue(history.savedEntries.isEmpty())
    }

    @Test
    fun `saving stores this event once with its photo, outcome and time`() = runTest(dispatcher) {
        val viewModel = newViewModel(FakeService(model) { unsupported })
        advanceUntilIdle()

        viewModel.save()
        viewModel.save() // repeated tap while saving
        assertEquals(SaveState.Saving, viewModel.saveState.value)
        advanceUntilIdle()
        viewModel.save() // tap after saved
        advanceUntilIdle()

        assertEquals(SaveState.Saved, viewModel.saveState.value)
        val entry = history.savedEntries.single()
        assertEquals("event-1", entry.id)
        assertEquals(unsupported, entry.outcome)
        assertEquals(photo, entry.photo)
        assertEquals(now, entry.identifiedAt)
        assertEquals(1, history.records.value.size)
    }

    @Test
    fun `a failed save can be tried again`() = runTest(dispatcher) {
        val viewModel = newViewModel(FakeService(model) { unsupported })
        advanceUntilIdle()
        history.failSave = true

        viewModel.save()
        advanceUntilIdle()
        assertEquals(SaveState.Failed, viewModel.saveState.value)

        history.failSave = false
        viewModel.save()
        advanceUntilIdle()
        assertEquals(SaveState.Saved, viewModel.saveState.value)
    }

    @Test
    fun `deleting the record elsewhere allows saving again`() = runTest(dispatcher) {
        val viewModel = newViewModel(FakeService(model) { unsupported })
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()

        history.delete("event-1")
        advanceUntilIdle()

        assertEquals(SaveState.NotSaved, viewModel.saveState.value)
    }

    @Test
    fun `retrying starts a new event with a new id`() = runTest(dispatcher) {
        val viewModel = newViewModel(FakeService(model) { unsupported })
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()

        viewModel.retry()
        assertEquals(SaveState.NotSaveable, viewModel.saveState.value)
        advanceUntilIdle()
        assertEquals("the new event isn't saved yet", SaveState.NotSaved, viewModel.saveState.value)

        viewModel.save()
        advanceUntilIdle()
        assertEquals(listOf("event-1", "event-2"), history.savedEntries.map { it.id })
    }

    // ---- Process death ----

    private fun recreate(service: IdentificationService, savedState: SavedStateHandle) = IdentificationViewModel(
        photo = photo,
        service = service,
        history = history,
        savedState = savedState,
        clock = { now.plusSeconds(3600) },
        newEventId = { "event-after-restart" },
    )

    @Test
    fun `a finished result comes back unchanged after process death, without re-identifying`() = runTest(dispatcher) {
        val savedState = SavedStateHandle()
        val identified = IdentificationOutcome.Identified(
            model, "Epipremnum_aureum", PlantSummary("epipremnum_aureum", "Golden Pothos", "Epipremnum aureum", null),
            scores = listOf(ClassScore("Epipremnum_aureum", 0.87f), ClassScore("other", 0.13f)),
        )
        val service = FakeService(model) { identified }
        IdentificationViewModel(photo, service, history, savedState, clock = { now }, newEventId = { "event-1" })
        advanceUntilIdle()

        val restored = recreate(service, savedState)

        assertEquals(UiState.Finished(identified), restored.uiState.value)
        assertEquals("confidence survives too", 0.87f, (identified as IdentificationOutcome.Identified).confidence)
        assertEquals("not identified again", 1, service.calls)
        advanceUntilIdle()
        restored.save()
        advanceUntilIdle()
        val entry = history.savedEntries.single()
        assertEquals("same event, so saving stays idempotent", "event-1", entry.id)
        assertEquals("original time kept", now, entry.identifiedAt)
    }

    @Test
    fun `a result saved before process death is still shown as saved`() = runTest(dispatcher) {
        val savedState = SavedStateHandle()
        val service = FakeService(model) { unsupported }
        val first = IdentificationViewModel(photo, service, history, savedState, clock = { now }, newEventId = { "event-1" })
        advanceUntilIdle()
        first.save()
        advanceUntilIdle()

        val restored = recreate(service, savedState)
        advanceUntilIdle()

        assertEquals(SaveState.Saved, restored.saveState.value)
    }

    @Test
    fun `a failure is restored too and can still be retried`() = runTest(dispatcher) {
        val savedState = SavedStateHandle()
        val service = FakeService(model) { IdentificationOutcome.Failed(model, FailureReason.TimedOut) }
        IdentificationViewModel(photo, service, history, savedState)
        advanceUntilIdle()

        val restored = recreate(service, savedState)
        assertEquals(failed(FailureReason.TimedOut), restored.uiState.value)
        assertEquals(SaveState.NotSaveable, restored.saveState.value)

        service.behaviour = { unsupported }
        restored.retry()
        advanceUntilIdle()
        assertEquals(UiState.Finished(unsupported), restored.uiState.value)
    }

    @Test
    fun `unreadable saved state falls back to identifying again`() = runTest(dispatcher) {
        val savedState = SavedStateHandle(mapOf(IdentificationViewModel.KEY_RESULT to "{not json"))
        val service = FakeService(model) { unsupported }

        val viewModel = recreate(service, savedState)
        assertEquals(UiState.Identifying, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(UiState.Finished(unsupported), viewModel.uiState.value)
        assertEquals(1, service.calls)
    }
}
