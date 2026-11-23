package io.github.ephym6.plantid.data.image

import java.io.File

/**
 * Holds the photo the user is about to identify. Photos are copied into
 * PlantID's private cache so the app owns a readable copy. That copy stays
 * valid if Android restarts the app, and corrupt or unsupported files are
 * caught at selection time.
 *
 * URIs are passed as strings so callers (ViewModels) stay testable on the JVM
 * without Android framework classes.
 */
interface PhotoStore {

    /** Copies the photo at [sourceUri] (from the photo picker) and checks it can be decoded. */
    suspend fun importFromPicker(sourceUri: String): PhotoImportResult

    /** Creates an empty file for the camera app to write into, or null if that fails. */
    suspend fun createCaptureTarget(): CaptureTarget?

    /**
     * Finishes a camera capture started with [createCaptureTarget]. If [captured]
     * is false the file is deleted and [PhotoImportResult.Cancelled] is returned.
     */
    suspend fun completeCapture(photoId: String, captured: Boolean): PhotoImportResult

    /** Deletes every stored photo except [photoId]. Only the current photo is kept. */
    suspend fun keepOnly(photoId: String)

    /** The file for [photoId], or null if the id is not a valid photo id. Does no disk I/O. */
    fun fileFor(photoId: String): File?
}

/** Result of trying to use a photo. */
sealed interface PhotoImportResult {
    /** [photoId] is a plain file name, safe to pass as a navigation argument. */
    data class Success(val photoId: String) : PhotoImportResult
    data object Cancelled : PhotoImportResult
    data class Failed(val error: PhotoError) : PhotoImportResult
}

/** Where the camera app should save a new photo. */
data class CaptureTarget(val photoId: String, val uri: String)

/** Why a photo could not be used. The UI maps each to a human-readable message. */
enum class PhotoError {
    /** The file couldn't be read or decoded (damaged, or a format the device can't decode). */
    CouldNotOpen,

    /** No camera app could be launched. */
    CameraUnavailable,

    /** The system photo picker couldn't be launched. */
    PickerUnavailable,

    /** PlantID couldn't create a file for the new photo. */
    CouldNotPrepare,
}
