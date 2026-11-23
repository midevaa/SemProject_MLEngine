package io.github.ephym6.plantid.data.history

import android.content.Context
import android.graphics.Bitmap
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.toBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Photos kept for saved history (DECISIONS D24): a downscaled JPEG copy, longest
 * side at most [MAX_DIMENSION_PX], in private app storage (`filesDir`, which the
 * system doesn't clear like the cache).
 *
 * Re-encoding drops EXIF metadata, including any GPS location from the camera.
 * Coil decodes the photo, applying its rotation, so the copy is upright.
 */
class HistoryPhotoStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, DIRECTORY_NAME)

    /** Saves a downscaled copy of [source] as `<recordId>.jpg`. Returns the file name, or null if it failed. */
    suspend fun saveCopy(source: File, recordId: String): String? {
        val request = ImageRequest.Builder(appContext)
            .data(source)
            .size(MAX_DIMENSION_PX)
            .precision(Precision.EXACT)
            .allowHardware(false) // software bitmap, so it can be re-encoded
            .build()
        val bitmap = (SingletonImageLoader.get(appContext).execute(request) as? SuccessResult)
            ?.image?.toBitmap() ?: return null
        return withContext(ioDispatcher) {
            val name = "$recordId$SUFFIX"
            val target = File(directory, name)
            try {
                directory.mkdirs()
                target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
                name
            } catch (_: IOException) {
                target.delete()
                null
            }
        }
    }

    /** The file for a stored name, or null if the name isn't a plain file name. No disk I/O. */
    fun fileFor(name: String): File? =
        if (FILE_NAME.matches(name)) File(directory, name) else null

    suspend fun delete(name: String) {
        withContext(ioDispatcher) { fileFor(name)?.delete() }
    }

    suspend fun deleteAll() {
        withContext(ioDispatcher) { directory.deleteRecursively() }
    }

    internal companion object {
        const val DIRECTORY_NAME = "history_photos"
        const val MAX_DIMENSION_PX = 1024
        const val JPEG_QUALITY = 85
        private const val SUFFIX = ".jpg"
        private val FILE_NAME = Regex("^[A-Za-z0-9-]+\\.jpg$")
    }
}
