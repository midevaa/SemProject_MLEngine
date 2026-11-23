package io.github.ephym6.plantid.ui.identify

import androidx.lifecycle.SavedStateHandle
import io.github.ephym6.plantid.data.image.CaptureTarget
import io.github.ephym6.plantid.data.image.PhotoError
import io.github.ephym6.plantid.data.image.PhotoImportResult
import io.github.ephym6.plantid.data.image.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoSelectionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val store = FakePhotoStore()
    private lateinit var savedState: SavedStateHandle
    private lateinit var viewModel: PhotoSelectionViewModel

    private val state get() = viewModel.uiState.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        savedState = SavedStateHandle()
        viewModel = PhotoSelectionViewModel(store, savedState)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `source chooser opens and closes`() {
        viewModel.openSourceChooser()
        assertTrue(state.isSourceChooserVisible)
        viewModel.dismissSourceChooser()
        assertFalse(state.isSourceChooserVisible)
    }

    // ---- Photo picker ----

    @Test
    fun `picked photo is imported and becomes the ready photo`() = runTest(dispatcher) {
        viewModel.openSourceChooser()
        viewModel.onPickerLaunched()
        assertFalse(state.isSourceChooserVisible)

        viewModel.onPhotoPicked("content://media/picked/1")
        assertTrue(state.isPreparing)
        advanceUntilIdle()

        assertFalse(state.isPreparing)
        assertEquals("picked_1.img", state.readyPhotoId)
        assertEquals("content://media/picked/1", store.importedSources.single())
        assertEquals("picked_1.img", store.keptOnly)
        assertNull(state.error)
    }

    @Test
    fun `cancelling the picker is not an error and does nothing`() = runTest(dispatcher) {
        viewModel.onPhotoPicked(null)
        advanceUntilIdle()

        assertTrue(store.importedSources.isEmpty())
        assertEquals(PhotoSelectionViewModel.UiState(), state)
    }

    @Test
    fun `unreadable picked photo shows an error and no preview`() = runTest(dispatcher) {
        store.importResult = PhotoImportResult.Failed(PhotoError.CouldNotOpen)

        viewModel.onPhotoPicked("content://media/broken")
        advanceUntilIdle()

        assertEquals(PhotoError.CouldNotOpen, state.error)
        assertNull(state.readyPhotoId)
        assertNull(store.keptOnly)

        viewModel.dismissError()
        assertNull(state.error)
    }

    @Test
    fun `picker unavailable shows an error`() {
        viewModel.openSourceChooser()
        viewModel.onPickerUnavailable()

        assertEquals(PhotoError.PickerUnavailable, state.error)
        assertFalse(state.isSourceChooserVisible)
    }

    @Test
    fun `a second photo while one is being prepared is ignored`() = runTest(dispatcher) {
        viewModel.onPhotoPicked("content://media/1")
        viewModel.onPhotoPicked("content://media/2")
        viewModel.openSourceChooser()
        advanceUntilIdle()

        assertEquals(listOf("content://media/1"), store.importedSources)
        assertFalse(state.isSourceChooserVisible)
    }

    @Test
    fun `ready photo is cleared once handled`() = runTest(dispatcher) {
        viewModel.onPhotoPicked("content://media/1")
        advanceUntilIdle()

        viewModel.onReadyPhotoHandled()
        assertNull(state.readyPhotoId)
    }

    // ---- Camera ----

    @Test
    fun `take a photo prepares a file and requests the camera`() = runTest(dispatcher) {
        viewModel.openSourceChooser()
        viewModel.requestCapture()
        assertFalse(state.isSourceChooserVisible)
        advanceUntilIdle()

        assertEquals(store.captureTarget, state.captureRequest)
        assertEquals("capture_1.jpg", savedState[PhotoSelectionViewModel.KEY_PENDING_CAPTURE])

        viewModel.onCaptureLaunched()
        assertNull(state.captureRequest)
    }

    @Test
    fun `saved capture becomes the ready photo`() = runTest(dispatcher) {
        viewModel.requestCapture()
        advanceUntilIdle()
        viewModel.onCaptureLaunched()

        viewModel.onCaptureResult(captured = true)
        advanceUntilIdle()

        assertEquals("capture_1.jpg", state.readyPhotoId)
        assertEquals(listOf("capture_1.jpg" to true), store.completedCaptures)
        assertNull(savedState.get<String>(PhotoSelectionViewModel.KEY_PENDING_CAPTURE))
    }

    @Test
    fun `cancelled capture discards the file without an error`() = runTest(dispatcher) {
        viewModel.requestCapture()
        advanceUntilIdle()
        viewModel.onCaptureLaunched()

        viewModel.onCaptureResult(captured = false)
        advanceUntilIdle()

        assertEquals(listOf("capture_1.jpg" to false), store.completedCaptures)
        assertNull(state.readyPhotoId)
        assertNull(state.error)
        assertFalse(state.isPreparing)
    }

    @Test
    fun `capture result after process death is still completed`() = runTest(dispatcher) {
        // Android killed PlantID while the camera was open; the ViewModel is recreated
        // from saved state and then receives the camera result.
        val restored = PhotoSelectionViewModel(
            store,
            SavedStateHandle(mapOf(PhotoSelectionViewModel.KEY_PENDING_CAPTURE to "capture_9.jpg")),
        )

        restored.onCaptureResult(captured = true)
        advanceUntilIdle()

        assertEquals("capture_9.jpg", restored.uiState.value.readyPhotoId)
    }

    @Test
    fun `capture result without a pending capture is ignored`() = runTest(dispatcher) {
        viewModel.onCaptureResult(captured = true)
        advanceUntilIdle()

        assertTrue(store.completedCaptures.isEmpty())
        assertNull(state.readyPhotoId)
    }

    @Test
    fun `camera app missing shows an error and discards the file`() = runTest(dispatcher) {
        viewModel.requestCapture()
        advanceUntilIdle()
        viewModel.onCaptureLaunched()

        viewModel.onCameraUnavailable()
        advanceUntilIdle()

        assertEquals(PhotoError.CameraUnavailable, state.error)
        assertEquals(listOf("capture_1.jpg" to false), store.completedCaptures)
        assertNull(savedState.get<String>(PhotoSelectionViewModel.KEY_PENDING_CAPTURE))
    }

    @Test
    fun `failure to create a capture file shows an error`() = runTest(dispatcher) {
        store.captureTarget = null

        viewModel.requestCapture()
        advanceUntilIdle()

        assertEquals(PhotoError.CouldNotPrepare, state.error)
        assertNull(state.captureRequest)
        assertFalse(state.isPreparing)
    }

    @Test
    fun `repeated take-a-photo taps create only one capture`() = runTest(dispatcher) {
        viewModel.requestCapture()
        viewModel.requestCapture()
        advanceUntilIdle()
        viewModel.requestCapture() // camera request still pending

        assertEquals(1, store.captureTargetsCreated)
    }

    private class FakePhotoStore : PhotoStore {
        var importResult: PhotoImportResult = PhotoImportResult.Success("picked_1.img")
        var captureTarget: CaptureTarget? = CaptureTarget("capture_1.jpg", "content://test.fileprovider/capture_1.jpg")
        val importedSources = mutableListOf<String>()
        val completedCaptures = mutableListOf<Pair<String, Boolean>>()
        var captureTargetsCreated = 0
        var keptOnly: String? = null

        override suspend fun importFromPicker(sourceUri: String): PhotoImportResult {
            importedSources += sourceUri
            return importResult
        }

        override suspend fun createCaptureTarget(): CaptureTarget? {
            captureTargetsCreated++
            return captureTarget
        }

        override suspend fun completeCapture(photoId: String, captured: Boolean): PhotoImportResult {
            completedCaptures += photoId to captured
            return if (captured) PhotoImportResult.Success(photoId) else PhotoImportResult.Cancelled
        }

        override suspend fun keepOnly(photoId: String) {
            keptOnly = photoId
        }

        override fun fileFor(photoId: String): File? = null
    }
}
