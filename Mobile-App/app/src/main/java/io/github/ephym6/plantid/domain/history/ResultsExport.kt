package io.github.ephym6.plantid.domain.history

import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Saved results as CSV for the research evaluation (DECISIONS D45). One row per
 * saved result, oldest first. No photos and nothing that identifies the tester:
 * only what the app showed and which model produced it. `true_label` and `notes`
 * are left blank for the tester to fill in, so accuracy is measured against
 * what the plant really is (see docs/EVALUATION_PROTOCOL.md).
 */
object ResultsCsv {

    val HEADER = listOf(
        "record_id",
        "identified_at_utc",
        "outcome",
        "predicted_label",
        "common_name",
        "scientific_name",
        "confidence",
        "model_name",
        "model_version",
        "demo",
        "true_label",
        "notes",
    )

    fun format(records: List<HistoryRecord>): String = buildString {
        appendRow(HEADER)
        records.sortedBy { it.identifiedAt }.forEach { record ->
            appendRow(
                listOf(
                    record.id,
                    DateTimeFormatter.ISO_INSTANT.format(record.identifiedAt),
                    when (record.status) {
                        HistoryStatus.Identified -> "identified"
                        HistoryStatus.Uncertain -> "not_sure"
                        HistoryStatus.Unsupported -> "no_supported_plant"
                    },
                    record.classId.orEmpty(),
                    record.commonName.orEmpty(),
                    record.scientificName.orEmpty(),
                    record.confidence?.let { String.format(Locale.ROOT, "%.4f", it) }.orEmpty(),
                    record.model.name,
                    record.model.version.orEmpty(),
                    record.model.isDemo.toString(),
                    "",
                    "",
                ),
            )
        }
    }

    /** RFC 4180: quote fields containing a comma, quote or line break; double inner quotes. */
    internal fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    private fun StringBuilder.appendRow(fields: List<String>) {
        fields.joinTo(this, separator = ",", transform = ::escape)
        append("\r\n")
    }
}

/** Writes an export file the user can share. Returns a shareable content URI. */
interface ResultsExporter {
    suspend fun export(records: List<HistoryRecord>): Result<String>
}
