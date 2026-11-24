package io.github.ephym6.plantid.data.history

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryStatus
import io.github.ephym6.plantid.domain.identification.ModelInfo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.LocalDateTime

/** The export file is written privately and shared only through a FileProvider URI. */
@RunWith(AndroidJUnit4::class)
class CsvResultsExporterTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val exportDir = File(context.cacheDir, CsvResultsExporter.DIRECTORY_NAME)
    private var clock = LocalDateTime.of(2026, 11, 23, 10, 0, 0)
    private val exporter = CsvResultsExporter(context, now = { clock })

    private val record = HistoryRecord(
        "a", Instant.parse("2026-11-23T09:00:00Z"), HistoryStatus.Identified, "golden_pothos", "golden_pothos",
        "Golden pothos", "Epipremnum aureum", ModelInfo("PlantID v1", "plantid_v1.tflite float16", false),
        photo = null, confidence = 0.87f,
    )

    @After
    fun cleanUp() {
        exportDir.deleteRecursively()
    }

    @Test
    fun exportIsReadableThroughTheFileProvider() = runBlocking {
        val uri = Uri.parse(exporter.export(listOf(record)).getOrThrow())

        assertEquals("content", uri.scheme)
        assertEquals(context.packageName + ".fileprovider", uri.authority)
        val text = context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        assertTrue(text.startsWith("record_id,identified_at_utc,"))
        assertTrue(text.contains("a,2026-11-23T09:00:00Z,identified,golden_pothos,Golden pothos,Epipremnum aureum,0.8700,"))
    }

    @Test
    fun onlyTheNewestExportIsKept() = runBlocking {
        exporter.export(listOf(record))
        clock = clock.plusMinutes(5)
        exporter.export(listOf(record))

        val files = exportDir.listFiles()!!.map { it.name }
        assertEquals(listOf("plantid-results-20261123-100500.csv"), files)
    }
}
