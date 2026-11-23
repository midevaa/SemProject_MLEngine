package io.github.ephym6.plantid.data.history

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ephym6.plantid.domain.history.HistoryStatus
import io.github.ephym6.plantid.domain.history.NewHistoryEntry
import io.github.ephym6.plantid.domain.identification.ClassScore
import io.github.ephym6.plantid.domain.identification.FailureReason
import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.domain.identification.PlantSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

/** Room + real photo copies. Uses an in-memory database; photos go to the app's history_photos dir and are removed after each test. */
@RunWith(AndroidJUnit4::class)
class RoomHistoryRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: PlantIdDatabase
    private lateinit var photos: HistoryPhotoStore
    private lateinit var repository: RoomHistoryRepository
    private val sources = File(context.cacheDir, "history_test_sources").apply { mkdirs() }

    private val demo = ModelInfo("Demo", null, isDemo = true)
    private val plant = PlantSummary("epipremnum_aureum", "Golden Pothos", "Epipremnum aureum", null)
    private val identified = IdentificationOutcome.Identified(
        demo, "Epipremnum_aureum", plant, scores = listOf(ClassScore("Epipremnum_aureum", 0.87f)),
    )
    private val time = Instant.parse("2026-11-19T10:15:30Z")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, PlantIdDatabase::class.java).build()
        photos = HistoryPhotoStore(context)
        repository = RoomHistoryRepository(database.historyDao(), photos)
    }

    @After
    fun tearDown() = runBlocking {
        database.close()
        photos.deleteAll()
        sources.deleteRecursively()
        Unit
    }

    private fun png(name: String, width: Int, height: Int): File {
        val file = File(sources, name)
        file.outputStream().use {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return file
    }

    private fun entry(id: String, outcome: IdentificationOutcome = identified, photo: File? = png("$id.png", 40, 30)) =
        NewHistoryEntry(id = id, identifiedAt = time, outcome = outcome, photo = photo)

    @Test
    fun savedIdentificationIsAStoredSnapshotWithAPhotoCopy() = runBlocking {
        assertTrue(repository.save(entry("a")).isSuccess)

        val record = repository.observe("a").first()!!
        assertEquals(HistoryStatus.Identified, record.status)
        assertEquals("Golden Pothos", record.commonName)
        assertEquals("Epipremnum aureum", record.scientificName)
        assertEquals("epipremnum_aureum", record.plantId)
        assertEquals("Epipremnum_aureum", record.classId)
        assertEquals(time, record.identifiedAt)
        assertTrue(record.model.isDemo)
        assertEquals(0.87f, record.confidence)
        assertTrue("photo copy exists", record.photo!!.isFile)
        assertEquals(true, repository.observeIsSaved("a").first())
    }

    @Test
    fun largePhotosAreDownscaledToAtMost1024Pixels() = runBlocking {
        repository.save(entry("big", photo = png("big.png", 3000, 2000)))

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(repository.observe("big").first()!!.photo!!.path, options)
        assertEquals(1024, maxOf(options.outWidth, options.outHeight))
        assertEquals("image/jpeg", options.outMimeType)
    }

    @Test
    fun savingTheSameEventTwiceKeepsOneRecord() = runBlocking {
        repository.save(entry("a"))
        repository.save(entry("a"))

        assertEquals(1, repository.observeAll().first().size)
    }

    @Test
    fun uncertainAndUnsupportedCanBeSavedWithoutAPlant() = runBlocking {
        repository.save(entry("u", IdentificationOutcome.Uncertain(demo)))
        repository.save(entry("n", IdentificationOutcome.Unsupported(demo)))

        val byId = repository.observeAll().first().associateBy { it.id }
        assertEquals(HistoryStatus.Uncertain, byId.getValue("u").status)
        assertNull(byId.getValue("u").commonName)
        assertNull("only identified results keep a confidence", byId.getValue("u").confidence)
        assertEquals(HistoryStatus.Unsupported, byId.getValue("n").status)
    }

    @Test
    fun failedOutcomesAreNeverSaved() = runBlocking {
        val result = repository.save(entry("f", IdentificationOutcome.Failed(demo, FailureReason.ProcessingFailed)))

        assertTrue(result.isFailure)
        assertTrue(repository.observeAll().first().isEmpty())
    }

    @Test
    fun recordIsKeptWithoutPhotoIfThePhotoIsGone() = runBlocking {
        repository.save(entry("a", photo = File(sources, "missing.png")))

        val record = repository.observe("a").first()
        assertNotNull(record)
        assertNull(record!!.photo)
    }

    @Test
    fun recordsAreNewestFirstAndRecentIsLimited() = runBlocking {
        repository.save(entry("old").copy(identifiedAt = time))
        repository.save(entry("new").copy(identifiedAt = time.plusSeconds(60)))
        repository.save(entry("mid").copy(identifiedAt = time.plusSeconds(30)))

        assertEquals(listOf("new", "mid", "old"), repository.observeAll().first().map { it.id })
        assertEquals(listOf("new", "mid"), repository.observeRecent(2).first().map { it.id })
    }

    @Test
    fun deleteRemovesTheRecordAndItsPhoto() = runBlocking {
        repository.save(entry("a"))
        val photo = repository.observe("a").first()!!.photo!!

        assertTrue(repository.delete("a").isSuccess)

        assertNull(repository.observe("a").first())
        assertFalse(photo.exists())
    }

    @Test
    fun clearAllRemovesEveryRecordAndPhoto() = runBlocking {
        repository.save(entry("a"))
        repository.save(entry("b"))
        val photoDir = repository.observe("a").first()!!.photo!!.parentFile!!

        assertTrue(repository.clearAll().isSuccess)

        assertTrue(repository.observeAll().first().isEmpty())
        assertFalse(photoDir.exists() && photoDir.listFiles()!!.isNotEmpty())
    }
}
