package io.github.ephym6.plantid.data.image

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * [PhotoStore] backed by `cacheDir/selected_photos/`. Nothing here leaves the
 * device: the only way out is the camera app writing *into* a capture file,
 * through a FileProvider URI that is shared with that app alone.
 *
 * Cache files can be cleared by the system. Callers must handle a missing
 * file (for example, show "photo no longer available").
 */
class CachePhotoStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PhotoStore {

    private val appContext = context.applicationContext
    private val directory = File(appContext.cacheDir, DIRECTORY_NAME)
    private val authority = appContext.packageName + AUTHORITY_SUFFIX

    override suspend fun importFromPicker(sourceUri: String): PhotoImportResult =
        withContext(ioDispatcher) {
            val target = newFile(PICKED_PREFIX, PICKED_SUFFIX)
                ?: return@withContext PhotoImportResult.Failed(PhotoError.CouldNotPrepare)
            val copied = try {
                appContext.contentResolver.openInputStream(sourceUri.toUri())?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                    true
                } ?: false
            } catch (_: IOException) {
                false
            } catch (_: SecurityException) {
                false
            }
            if (copied) {
                validate(target)
            } else {
                target.delete()
                PhotoImportResult.Failed(PhotoError.CouldNotOpen)
            }
        }

    override suspend fun createCaptureTarget(): CaptureTarget? = withContext(ioDispatcher) {
        val file = newFile(CAPTURE_PREFIX, CAPTURE_SUFFIX) ?: return@withContext null
        try {
            CaptureTarget(
                photoId = file.name,
                uri = FileProvider.getUriForFile(appContext, authority, file).toString(),
            )
        } catch (_: IllegalArgumentException) {
            file.delete()
            null
        }
    }

    override suspend fun completeCapture(photoId: String, captured: Boolean): PhotoImportResult =
        withContext(ioDispatcher) {
            val file = fileFor(photoId)
                ?: return@withContext PhotoImportResult.Failed(PhotoError.CouldNotOpen)
            if (captured) {
                validate(file)
            } else {
                file.delete()
                PhotoImportResult.Cancelled
            }
        }

    override suspend fun keepOnly(photoId: String) {
        withContext(ioDispatcher) {
            directory.listFiles()
                ?.filter { it.name != photoId }
                ?.forEach { it.delete() }
        }
    }

    override fun fileFor(photoId: String): File? =
        if (isValidPhotoId(photoId)) File(directory, photoId) else null

    /** Accepts the file only if it is non-empty and its image header can be decoded. */
    private fun validate(file: File): PhotoImportResult {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        if (file.length() > 0) BitmapFactory.decodeFile(file.path, options)
        return if (options.outWidth > 0 && options.outHeight > 0) {
            PhotoImportResult.Success(file.name)
        } else {
            file.delete()
            PhotoImportResult.Failed(PhotoError.CouldNotOpen)
        }
    }

    private fun newFile(prefix: String, suffix: String): File? = try {
        directory.mkdirs()
        File.createTempFile(prefix, suffix, directory)
    } catch (_: IOException) {
        null
    }

    internal companion object {
        /** Must match `res/xml/file_paths.xml`. */
        const val DIRECTORY_NAME = "selected_photos"

        /** Must match the provider authority in AndroidManifest.xml. */
        const val AUTHORITY_SUFFIX = ".fileprovider"

        private const val PICKED_PREFIX = "picked_"
        private const val PICKED_SUFFIX = ".img"
        private const val CAPTURE_PREFIX = "capture_"
        private const val CAPTURE_SUFFIX = ".jpg"

        /** Plain file names only. Rejects path separators and "..", so a route argument can't escape the directory. */
        private val PHOTO_ID = Regex("^[A-Za-z0-9_-]+\\.[A-Za-z0-9]+$")

        fun isValidPhotoId(photoId: String): Boolean = PHOTO_ID.matches(photoId)
    }
}
