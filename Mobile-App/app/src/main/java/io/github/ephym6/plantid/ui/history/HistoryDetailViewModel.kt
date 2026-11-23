package io.github.ephym6.plantid.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryRepository
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

/** One saved record, and deleting it. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryDetailViewModel(
    private val recordId: String,
    private val history: HistoryRepository,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Found(val record: HistoryRecord) : UiState
        data object NotFound : UiState
        data object Error : UiState
    }

    private val reloads = MutableStateFlow(0)

    val uiState: StateFlow<UiState> = reloads
        .flatMapLatest {
            history.observe(recordId)
                .map { record -> if (record == null) UiState.NotFound else UiState.Found(record) }
                .catch { emit(UiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private val _deleted = MutableStateFlow(false)

    /** True once the record has been deleted; the screen then navigates back. */
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private val _deleteFailed = MutableStateFlow(false)
    val deleteFailed: StateFlow<Boolean> = _deleteFailed.asStateFlow()

    private var deleting = false

    fun retry() {
        reloads.value++
    }

    /** Call only after the user confirmed. Repeated taps delete once. */
    fun delete() {
        if (deleting || _deleted.value) return
        deleting = true
        viewModelScope.launch {
            history.delete(recordId).fold(
                onSuccess = { _deleted.value = true },
                onFailure = { _deleteFailed.value = true },
            )
            deleting = false
        }
    }

    fun dismissDeleteError() {
        _deleteFailed.value = false
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(recordId: String, history: HistoryRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { HistoryDetailViewModel(recordId, history) } }
    }
}
