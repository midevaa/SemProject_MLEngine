package io.github.ephym6.plantid.ui.identify

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ephym6.plantid.domain.history.HistoryRepository
import io.github.ephym6.plantid.domain.history.NewHistoryEntry
import io.github.ephym6.plantid.domain.history.historyStatusOf
import io.github.ephym6.plantid.domain.identification.FailureReason
import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import io.github.ephym6.plantid.domain.identification.IdentificationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Runs one identification for one photo, and saves its result to history on
 * request. Scoped to the identification screen's back-stack entry: it starts
 * exactly once (rotation doesn't restart it), leaving the screen cancels it,
 * and [retry]/[save] are ignored while busy, so repeated taps never start
 * concurrent work or duplicate records.
 *
 * Any failure, including an exception or a timeout, becomes
 * [IdentificationOutcome.Failed], never a fake success.
 *
 * Each finished outcome is one identification event with its own id and time.
 * "Saved" is read back from history, so it stays correct across rotation.
 *
 * The finished outcome and its event are kept in [savedState], so if Android
 * stops the app in the background the same result comes back. It isn't
 * re-identified (which could show a different result or allow saving the same
 * photo twice).
 */
class IdentificationViewModel(
    private val photo: File?,
    private val service: IdentificationService,
    private val history: HistoryRepository,
    private val savedState: SavedStateHandle,
    private val timeoutMillis: Long = TIMEOUT_MILLIS,
    private val clock: () -> Instant = Instant::now,
    private val newEventId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    sealed interface UiState {
        data object Identifying : UiState
        data class Finished(val outcome: IdentificationOutcome) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Identifying)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow(SaveState.NotSaveable)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private var event: NewHistoryEntry? = null
    private var savedObserver: Job? = null

    init {
        val restored = restoreResult()
        if (restored == null) {
            identify()
        } else {
            _uiState.value = UiState.Finished(restored.outcome)
            if (restored.eventId != null && restored.identifiedAtMillis != null) {
                startEvent(restored.outcome, restored.eventId, Instant.ofEpochMilli(restored.identifiedAtMillis))
            }
        }
    }

    fun retry() {
        if (_uiState.value is UiState.Identifying) return
        identify()
    }

    fun save() {
        val entry = event ?: return
        if (_saveState.value != SaveState.NotSaved && _saveState.value != SaveState.Failed) return
        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            _saveState.value = history.save(entry).fold(
                onSuccess = { SaveState.Saved },
                onFailure = { SaveState.Failed },
            )
        }
    }

    private fun identify() {
        _uiState.value = UiState.Identifying
        _saveState.value = SaveState.NotSaveable
        event = null
        savedObserver?.cancel()
        savedState.remove<String>(KEY_RESULT)
        viewModelScope.launch {
            val outcome = runIdentification()
            val saveable = historyStatusOf(outcome) != null
            val eventId = if (saveable) newEventId() else null
            val identifiedAt = clock()
            storeResult(SavedResult(outcome, eventId, identifiedAt.toEpochMilli()))
            _uiState.value = UiState.Finished(outcome)
            if (eventId != null) startEvent(outcome, eventId, identifiedAt)
        }
    }

    private fun startEvent(outcome: IdentificationOutcome, eventId: String, identifiedAt: Instant) {
        val entry = NewHistoryEntry(id = eventId, identifiedAt = identifiedAt, outcome = outcome, photo = photo)
        event = entry
        _saveState.value = SaveState.NotSaved
        savedObserver = viewModelScope.launch {
            history.observeIsSaved(entry.id)
                .catch { /* reading the saved flag failed: keep the current state */ }
                .collect { saved ->
                    if (saved) {
                        _saveState.value = SaveState.Saved
                    } else if (_saveState.value == SaveState.Saved) {
                        // Deleted from history (e.g. on another screen): it can be saved again.
                        _saveState.value = SaveState.NotSaved
                    }
                }
        }
    }

    private suspend fun runIdentification(): IdentificationOutcome {
        val photo = photo ?: return failed(FailureReason.PhotoUnavailable)
        return try {
            withTimeoutOrNull(timeoutMillis) { service.identify(photo) }
                ?: failed(FailureReason.TimedOut)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Deliberately not logged: no approved diagnostics approach yet (spec §6.7).
            failed(FailureReason.ProcessingFailed)
        }
    }

    private fun failed(reason: FailureReason) = IdentificationOutcome.Failed(service.modelInfo, reason)

    /** What is kept across process death. */
    @Serializable
    private data class SavedResult(
        val outcome: IdentificationOutcome,
        val eventId: String?,
        val identifiedAtMillis: Long?,
    )

    private fun storeResult(result: SavedResult) {
        savedState[KEY_RESULT] = json.encodeToString(SavedResult.serializer(), result)
    }

    /** Null if there is nothing saved or it can't be read (then we identify again). */
    private fun restoreResult(): SavedResult? {
        val text = savedState.get<String>(KEY_RESULT) ?: return null
        return try {
            json.decodeFromString(SavedResult.serializer(), text)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    companion object {
        /**
         * Safety net against an endless loading state. Provisional: set it from
         * measured on-device inference time in Sprint 7 (spec: measure, don't invent).
         */
        const val TIMEOUT_MILLIS = 30_000L

        internal const val KEY_RESULT = "identification_result"
        private val json = Json { ignoreUnknownKeys = true }

        fun factory(
            photo: File?,
            service: IdentificationService,
            history: HistoryRepository,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { IdentificationViewModel(photo, service, history, createSavedStateHandle()) }
            }
    }
}
