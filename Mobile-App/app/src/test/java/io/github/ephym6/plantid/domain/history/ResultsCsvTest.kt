package io.github.ephym6.plantid.domain.history

import io.github.ephym6.plantid.domain.identification.ModelInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Locale

/** D45: the results export is what the evaluation's accuracy numbers are computed from. */
class ResultsCsvTest {

    private val model = ModelInfo("PlantID v1", "plantid_v1.tflite float16", isDemo = false)

    private fun record(
        id: String,
        at: String,
        status: HistoryStatus = HistoryStatus.Identified,
        label: String? = "golden_pothos",
        common: String? = "Golden pothos / Money plant",
        scientific: String? = "Epipremnum aureum",
        confidence: Float? = 0.87f,
    ) = HistoryRecord(id, Instant.parse(at), status, label, label, common, scientific, model, photo = null, confidence = confidence)

    private fun lines(csv: String) = csv.split("\r\n").filter { it.isNotEmpty() }

    @Test
    fun `header names every column, with blank true_label and notes for testers`() {
        val csv = ResultsCsv.format(emptyList())

        assertEquals(
            "record_id,identified_at_utc,outcome,predicted_label,common_name,scientific_name,confidence," +
                "model_name,model_version,demo,true_label,notes",
            lines(csv).single(),
        )
    }

    @Test
    fun `rows are oldest first with UTC times, outcome codes and confidence`() {
        val csv = ResultsCsv.format(
            listOf(
                record("b", "2026-11-23T10:00:00Z", HistoryStatus.Uncertain, label = null, common = null, scientific = null, confidence = null),
                record("a", "2026-11-23T09:30:15Z"),
                record("c", "2026-11-23T11:00:00Z", HistoryStatus.Unsupported, label = null, common = null, scientific = null, confidence = null),
            ),
        )
        val rows = lines(csv).drop(1)

        assertEquals(
            "a,2026-11-23T09:30:15Z,identified,golden_pothos,Golden pothos / Money plant,Epipremnum aureum,0.8700," +
                "PlantID v1,plantid_v1.tflite float16,false,,",
            rows[0],
        )
        assertEquals("b,2026-11-23T10:00:00Z,not_sure,,,,,PlantID v1,plantid_v1.tflite float16,false,,", rows[1])
        assertTrue(rows[2].startsWith("c,2026-11-23T11:00:00Z,no_supported_plant,"))
        assertTrue("lines end with CRLF (RFC 4180)", csv.endsWith("\r\n"))
    }

    @Test
    fun `confidence uses a dot whatever the phone's language`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY) // decimal comma
            val row = lines(ResultsCsv.format(listOf(record("a", "2026-11-23T09:00:00Z", confidence = 0.5f))))[1]
            assertTrue(row, row.contains(",0.5000,"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `fields with commas, quotes or line breaks are quoted`() {
        assertEquals("plain", ResultsCsv.escape("plain"))
        assertEquals("\"Chilli, sweet pepper\"", ResultsCsv.escape("Chilli, sweet pepper"))
        assertEquals("\"say \"\"hi\"\"\"", ResultsCsv.escape("say \"hi\""))
        assertEquals("\"two\nlines\"", ResultsCsv.escape("two\nlines"))
    }

    @Test
    fun `demo results are flagged so they can be excluded`() {
        val demo = record("d", "2026-11-23T09:00:00Z").copy(model = ModelInfo("PlantID demo identifier", null, isDemo = true))

        val row = lines(ResultsCsv.format(listOf(demo)))[1]

        assertTrue(row.contains(",PlantID demo identifier,,true,"))
        assertFalse("no photo data in the export", row.contains(".jpg"))
    }
}
