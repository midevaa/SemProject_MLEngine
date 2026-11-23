package io.github.ephym6.plantid.ui.identify

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ephym6.plantid.PlantIdApplication
import io.github.ephym6.plantid.data.image.CaptureTarget
import io.github.ephym6.plantid.data.image.PhotoError
import io.github.ephym6.plantid.data.image.PhotoImportResult
import io.github.ephym6.plantid.data.image.PhotoStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for choosing a plant photo (camera or photo picker). Activity-scoped
 * and shared by every screen that offers "Identify a plant" or "Choose another
 * photo".
 *
 * One-off UI actions are modelled as state that the UI clears once it has
 * acted: [UiState.captureRequest] (launch the camera) and
 * [UiState.readyPhotoId] (navigate to the preview).
 */
class PhotoSelectionViewModel(
    private val photoStore: PhotoStore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    data class UiState(
        val isSourceChooserVisible: Boolean = false,
        /** Creating a capture file, or copying/checking a photo. Blocks new requests. */
        val isPreparing: Boolean = false,
        /** Set when the camera should be launched; the UI calls [onCaptureLaunched]. */
        val captureRequest: CaptureTarget? = null,
        val error: PhotoError? = null,
        /** Set when a photo is ready to preview; the UI calls [onReadyPhotoHandled]. */
        val readyPhotoId: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * The capture file the camera app is writing to. Saved so that a result
     * delivered after Android killed PlantID (common while the camera is open)
     * can still be completed.
     */
    private var pendingCaptureId: String?
        get() = savedStateHandle[KEY_PENDING_CAPTURE]
        set(value) {
            savedStateHandle[KEY_PENDING_CAPTURE] = value
        }

    private val isBusy: Boolean
        get() = _uiState.value.let { it.isPreparing || it.captureRequest != null }

    fun openSourceChooser() {
        if (isBusy) return
        _uiState.update { it.copy(isSourceChooserVisible = true) }
    }

    fun dismissSourceChooser() {
        _uiState.update { it.copy(isSourceChooserVisible = false) }
    }

    /** "Take a photo": prepares a file, then asks the UI to launch the camera. */
    fun requestCapture() {
        if (isBusy) return
        _uiState.update { it.copy(isSourceChooserVisible = false, isPreparing = true) }
        viewModelScope.launch {
            val target = photoStore.createCaptureTarget()
            if (target == null) {
                _uiState.update { it.copy(isPreparing = false, error = PhotoError.CouldNotPrepare) }
            } else {
                pendingCaptureId = target.photoId
                _uiState.update { it.copy(isPreparing = false, captureRequest = target) }
            }
        }
    }

    fun onCaptureLaunched() {
        _uiState.update { it.copy(captureRequest = null) }
    }

    fun onCameraUnavailable() {
        val photoId = pendingCaptureId
        pendingCaptureId = null
        _uiState.update { it.copy(captureRequest = null, error = PhotoError.CameraUnavailable) }
        if (photoId != null) {
            viewModelScope.launch { photoStore.completeCapture(photoId, captured = false) }
        }
    }

    /** Result from the camera app. [captured] is false if the user backed out. */
    fun onCaptureResult(captured: Boolean) {
        val photoId = pendingCaptureId ?: return
        pendingCaptureId = null
        usePhoto { photoStore.completeCapture(photoId, captured) }
    }

    /** "Choose from photos": the UI launches the picker itself. */
    fun onPickerLaunched() {
        _uiState.update { it.copy(isSourceChooserVisible = false) }
    }

    fun onPickerUnavailable() {
        _uiState.update { it.copy(isSourceChooserVisible = false, error = PhotoError.PickerUnavailable) }
    }

    /** Result from the photo picker. Null means the user cancelled, which is not an error. */
    fun onPhotoPicked(sourceUri: String?) {
        if (sourceUri == null) return
        usePhoto { photoStore.importFromPicker(sourceUri) }
    }

    fun onReadyPhotoHandled() {
        _uiState.update { it.copy(readyPhotoId = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun usePhoto(produce: suspend () -> PhotoImportResult) {
        if (_uiState.value.isPreparing) return
        _uiState.update { it.copy(isSourceChooserVisible = false, isPreparing = true) }
        viewModelScope.launch {
            when (val result = produce()) {
                is PhotoImportResult.Success -> {
                    // Only the newest photo is kept; the previous preview (if any) is replaced.
                    photoStore.keepOnly(result.photoId)
                    _uiState.update { it.copy(isPreparing = false, readyPhotoId = result.photoId) }
                }
                PhotoImportResult.Cancelled ->
                    _uiState.update { it.copy(isPreparing = false) }
                is PhotoImportResult.Failed ->
                    _uiState.update { it.copy(isPreparing = false, error = result.error) }
            }
        }
    }

    companion object {
        internal const val KEY_PENDING_CAPTURE = "pending_capture_photo_id"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PlantIdApplication
                PhotoSelectionViewModel(app.container.photoStore, createSavedStateHandle())
            }
        }
    }
}
