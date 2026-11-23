package io.github.ephym6.plantid.data.image

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real-file tests for [CachePhotoStore] (copying, validation, FileProvider, cleanup). */
@RunWith(AndroidJUnit4::class)
class CachePhotoStoreTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = CachePhotoStore(context)
    private val photoDirectory = File(context.cacheDir, CachePhotoStore.DIRECTORY_NAME)
    private val sources = File(context.cacheDir, "test_photo_sources").apply { mkdirs() }

    @After
    fun cleanUp() {
        photoDirectory.deleteRecursively()
        sources.deleteRecursively()
    }

    private fun writePng(file: File): File {
        val bitmap = Bitmap.createBitmap(8, 6, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    private fun storedFiles(): List<String> = photoDirectory.listFiles()?.map { it.name }.orEmpty()

    @Test
    fun importingAValidPhotoCopiesItIntoTheStore() = runBlocking {
        val source = writePng(File(sources, "plant.png"))

        val result = store.importFromPicker(Uri.fromFile(source).toString())

        assertTrue(result is PhotoImportResult.Success)
        val stored = store.fileFor((result as PhotoImportResult.Success).photoId)
        assertNotNull(stored)
        assertEquals(photoDirectory, stored!!.parentFile)
        assertEquals(source.length(), stored.length())
    }

    @Test
    fun importingACorruptFileFailsAndLeavesNothingBehind() = runBlocking {
        val source = File(sources, "broken.jpg").apply { writeText("this is not an image") }

        val result = store.importFromPicker(Uri.fromFile(source).toString())

        assertEquals(PhotoImportResult.Failed(PhotoError.CouldNotOpen), result)
        assertTrue(storedFiles().isEmpty())
    }

    @Test
    fun importingAMissingFileFails() = runBlocking {
        val result = store.importFromPicker(Uri.fromFile(File(sources, "missing.png")).toString())

        assertEquals(PhotoImportResult.Failed(PhotoError.CouldNotOpen), result)
        assertTrue(storedFiles().isEmpty())
    }

    @Test
    fun captureTargetIsSharedThroughTheFileProvider() = runBlocking {
        val target = store.createCaptureTarget()!!
        val uri = Uri.parse(target.uri)

        assertEquals("content", uri.scheme)
        assertEquals(context.packageName + ".fileprovider", uri.authority)
        assertTrue(store.fileFor(target.photoId)!!.exists())
    }

    @Test
    fun cancelledCaptureDeletesTheFile() = runBlocking {
        val target = store.createCaptureTarget()!!

        val result = store.completeCapture(target.photoId, captured = false)

        assertEquals(PhotoImportResult.Cancelled, result)
        assertFalse(store.fileFor(target.photoId)!!.exists())
    }

    @Test
    fun captureReportedSavedButEmptyIsRejected() = runBlocking {
        val target = store.createCaptureTarget()!!

        val result = store.completeCapture(target.photoId, captured = true)

        assertEquals(PhotoImportResult.Failed(PhotoError.CouldNotOpen), result)
        assertFalse(store.fileFor(target.photoId)!!.exists())
    }

    @Test
    fun savedCaptureIsAccepted() = runBlocking {
        val target = store.createCaptureTarget()!!
        writePng(store.fileFor(target.photoId)!!) // what the camera app would do

        val result = store.completeCapture(target.photoId, captured = true)

        assertEquals(PhotoImportResult.Success(target.photoId), result)
    }

    @Test
    fun keepOnlyDeletesEveryOtherPhoto() = runBlocking {
        val first = store.importFromPicker(Uri.fromFile(writePng(File(sources, "a.png"))).toString())
        val second = store.importFromPicker(Uri.fromFile(writePng(File(sources, "b.png"))).toString())
        val keep = (second as PhotoImportResult.Success).photoId

        store.keepOnly(keep)

        assertEquals(listOf(keep), storedFiles())
        assertFalse(store.fileFor((first as PhotoImportResult.Success).photoId)!!.exists())
    }
}
