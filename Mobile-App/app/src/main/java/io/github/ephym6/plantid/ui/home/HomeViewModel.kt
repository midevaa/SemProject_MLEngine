package io.github.ephym6.plantid.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

/** Home's recent identifications. Null while loading or if history can't be read (section hidden). */
class HomeViewModel(history: HistoryRepository) : ViewModel() {

    val recent: StateFlow<List<HistoryRecord>?> = history.observeRecent(RECENT_COUNT)
        .catch { emit(emptyList()) } // Home stays usable; History shows the error.
        .stateIn<List<HistoryRecord>?>(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    companion object {
        const val RECENT_COUNT = 3
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(history: HistoryRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { HomeViewModel(history) } }
    }
}
