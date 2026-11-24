package io.github.ephym6.plantid.ui.history

import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryStatus
import io.github.ephym6.plantid.domain.history.ResultsExporter
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.testing.FakeHistoryRepository
import io.github.ephym6.plantid.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** History list, saved result and Home's recent section. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelsTest {

    private val dispatcher = StandardTestDispatcher()
    private val history = FakeHistoryRepository()

    private class FakeExporter : ResultsExporter {
        var result: Result<String> = Result.success("content://test.fileprovider/exports/results.csv")
        val exported = mutableListOf<List<HistoryRecord>>()
        override suspend fun export(records: List<HistoryRecord>): Result<String> {
            exported += records
            return result
        }
    }

    private val exporter = FakeExporter()
    private val model = ModelInfo("Demo", null, isDemo = true)

    private fun record(id: String, minute: Long = 0) = HistoryRecord(
        id = id,
        identifiedAt = Instant.EPOCH.plusSeconds(minute * 60),
        status = HistoryStatus.Identified,
        classId = "c",
        plantId = "p",
        commonName = "Plant $id",
        scientificName = null,
        model = model,
        photo = null,
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** stateIn(WhileSubscribed) only runs while someone collects, like the UI. */
    private fun TestScope.keepCollecting(flow: kotlinx.coroutines.flow.Flow<*>) {
        backgroundScope.launch(dispatcher) { flow.collect() }
    }

    // ---- History list ----

    @Test
    fun `history list shows empty, then records`() = runTest(dispatcher) {
        val viewModel = HistoryViewModel(history, exporter)
        keepCollecting(viewModel.uiState)
        assertEquals(HistoryViewModel.UiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(HistoryViewModel.UiState.Empty, viewModel.uiState.value)

        history.records.value = listOf(record("a"))
        advanceUntilIdle()
        assertEquals(HistoryViewModel.UiState.Records(listOf(record("a"))), viewModel.uiState.value)
    }

    @Test
    fun `history read error is shown and can be retried`() = runTest(dispatcher) {
        history.failReads = true
        val viewModel = HistoryViewModel(history, exporter)
        keepCollecting(viewModel.uiState)
        advanceUntilIdle()
        assertEquals(HistoryViewModel.UiState.Error, viewModel.uiState.value)

        history.failReads = false
        history.records.value = listOf(record("a"))
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(HistoryViewModel.UiState.Records(listOf(record("a"))), viewModel.uiState.value)
    }

    @Test
    fun `clear all empties history, and a failure is reported`() = runTest(dispatcher) {
        history.records.value = listOf(record("a"), record("b"))
        val viewModel = HistoryViewModel(history, exporter)
        keepCollecting(viewModel.uiState)

        history.failClear = true
        viewModel.clearAll()
        advanceUntilIdle()
        assertTrue(viewModel.clearFailed.value)
        assertEquals(2, history.records.value.size)
        viewModel.dismissClearError()
        assertFalse(viewModel.clearFailed.value)

        history.failClear = false
        viewModel.clearAll()
        advanceUntilIdle()
        assertEquals(HistoryViewModel.UiState.Empty, viewModel.uiState.value)
    }

    // ---- Saved result ----

    @Test
    fun `saved result is found, and shows not found once deleted`() = runTest(dispatcher) {
        history.records.value = listOf(record("a"))
        val viewModel = HistoryDetailViewModel("a", history)
        keepCollecting(viewModel.uiState)
        advanceUntilIdle()
        assertEquals(HistoryDetailViewModel.UiState.Found(record("a")), viewModel.uiState.value)

        viewModel.delete()
        viewModel.delete() // repeated tap
        advanceUntilIdle()

        assertTrue(viewModel.deleted.value)
        assertEquals(1, history.deleteCalls)
        assertEquals(HistoryDetailViewModel.UiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun `failed delete is reported and the record stays`() = runTest(dispatcher) {
        history.records.value = listOf(record("a"))
        history.failDelete = true
        val viewModel = HistoryDetailViewModel("a", history)
        keepCollecting(viewModel.uiState)

        viewModel.delete()
        advanceUntilIdle()

        assertTrue(viewModel.deleteFailed.value)
        assertFalse(viewModel.deleted.value)
        assertEquals(HistoryDetailViewModel.UiState.Found(record("a")), viewModel.uiState.value)
    }

    @Test
    fun `unknown record id shows not found`() = runTest(dispatcher) {
        val viewModel = HistoryDetailViewModel("missing", history)
        keepCollecting(viewModel.uiState)
        advanceUntilIdle()

        assertEquals(HistoryDetailViewModel.UiState.NotFound, viewModel.uiState.value)
    }

    // ---- Home ----

    @Test
    fun `home shows at most the 3 most recent records`() = runTest(dispatcher) {
        history.records.value = listOf(record("d", 4), record("c", 3), record("b", 2), record("a", 1))
        val viewModel = HomeViewModel(history)
        keepCollecting(viewModel.recent)
        assertNull("loading", viewModel.recent.value)
        advanceUntilIdle()

        assertEquals(listOf("d", "c", "b"), viewModel.recent.value?.map { it.id })
    }

    @Test
    fun `home hides the section if history can't be read`() = runTest(dispatcher) {
        history.failReads = true
        val viewModel = HomeViewModel(history)
        keepCollecting(viewModel.recent)
        advanceUntilIdle()

        assertEquals(emptyList<HistoryRecord>(), viewModel.recent.value)
    }

    // ---- Export (D45) ----

    @Test
    fun `export shares every saved result once`() = runTest(dispatcher) {
        history.records.value = listOf(record("a"), record("b"))
        val viewModel = HistoryViewModel(history, exporter)
        keepCollecting(viewModel.uiState)
        advanceUntilIdle()

        viewModel.export()
        viewModel.export() // repeated tap
        advanceUntilIdle()

        assertEquals(listOf(listOf(record("a"), record("b"))), exporter.exported)
        assertEquals("content://test.fileprovider/exports/results.csv", viewModel.exportUri.value)
        viewModel.onExportHandled()
        assertNull(viewModel.exportUri.value)
    }

    @Test
    fun `nothing to export when history is empty`() = runTest(dispatcher) {
        val viewModel = HistoryViewModel(history, exporter)
        keepCollecting(viewModel.uiState)
        advanceUntilIdle()

        viewModel.export()
        advanceUntilIdle()

        assertTrue(exporter.exported.isEmpty())
    }

    @Test
    fun `a failed export is reported`() = runTest(dispatcher) {
        history.records.value = listOf(record("a"))
        exporter.result = Result.failure(java.io.IOException("disk full"))
        val viewModel = HistoryViewModel(history, exporter)
        keepCollecting(viewModel.uiState)
        advanceUntilIdle()

        viewModel.export()
        advanceUntilIdle()

        assertTrue(viewModel.exportFailed.value)
        assertNull(viewModel.exportUri.value)
        viewModel.dismissExportError()
        assertFalse(viewModel.exportFailed.value)
    }
}
