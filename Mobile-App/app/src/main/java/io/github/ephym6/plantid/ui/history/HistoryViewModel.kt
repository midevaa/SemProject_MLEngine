package io.github.ephym6.plantid.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryRepository
import io.github.ephym6.plantid.domain.history.ResultsExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The History list: all saved identifications, newest first, plus Clear all. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val history: HistoryRepository,
    private val exporter: ResultsExporter,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data object Empty : UiState
        data class Records(val records: List<HistoryRecord>) : UiState

        /** History couldn't be read (spec §6.1 "local data read error"). */
        data object Error : UiState
    }

    private val reloads = MutableStateFlow(0)

    val uiState: StateFlow<UiState> = reloads
        .flatMapLatest {
            history.observeAll()
                .map { records -> if (records.isEmpty()) UiState.Empty else UiState.Records(records) }
                .catch { emit(UiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private val _clearFailed = MutableStateFlow(false)
    val clearFailed: StateFlow<Boolean> = _clearFailed.asStateFlow()

    fun retry() {
        reloads.value++
    }

    /** Call only after the user confirmed. */
    fun clearAll() {
        viewModelScope.launch {
            history.clearAll().onFailure { _clearFailed.value = true }
        }
    }

    fun dismissClearError() {
        _clearFailed.value = false
    }

    private val _exportUri = MutableStateFlow<String?>(null)

    /** Set when an export file is ready to share; the screen opens the share sheet, then calls [onExportHandled]. */
    val exportUri: StateFlow<String?> = _exportUri.asStateFlow()

    private val _exportFailed = MutableStateFlow(false)
    val exportFailed: StateFlow<Boolean> = _exportFailed.asStateFlow()

    private var exporting = false

    /** Exports every saved result as CSV (D45). Call only after the user confirmed. Repeated taps export once. */
    fun export() {
        val records = (uiState.value as? UiState.Records)?.records ?: return
        if (exporting) return
        exporting = true
        viewModelScope.launch {
            exporter.export(records).fold(
                onSuccess = { _exportUri.value = it },
                onFailure = { _exportFailed.value = true },
            )
            exporting = false
        }
    }

    fun onExportHandled() {
        _exportUri.value = null
    }

    /** The share sheet couldn't be opened, or the file couldn't be written. */
    fun onExportFailed() {
        _exportUri.value = null
        _exportFailed.value = true
    }

    fun dismissExportError() {
        _exportFailed.value = false
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(history: HistoryRepository, exporter: ResultsExporter): ViewModelProvider.Factory =
            viewModelFactory { initializer { HistoryViewModel(history, exporter) } }
    }
}
