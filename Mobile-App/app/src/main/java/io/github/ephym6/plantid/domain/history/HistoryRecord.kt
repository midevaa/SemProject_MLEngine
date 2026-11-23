package io.github.ephym6.plantid.domain.history

import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import io.github.ephym6.plantid.domain.identification.ModelInfo
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.Instant

/**
 * A saved identification event (spec §7.2), stored as a **snapshot**: the
 * names and status shown at the time, so old history doesn't silently change
 * if plant data is updated later (spec §6.9). [plantId] links to the current
 * care guide.
 */
data class HistoryRecord(
    val id: String,
    val identifiedAt: Instant,
    val status: HistoryStatus,
    val classId: String?,
    val plantId: String?,
    val commonName: String?,
    val scientificName: String?,
    val model: ModelInfo,
    /** The saved, downscaled photo copy, or null if none was kept. The file may be missing. */
    val photo: File?,
    /** Calibrated model confidence of an identified result, if the identifier gave one (D43). */
    val confidence: Float? = null,
)

/** Outcomes that can be saved. Failed attempts are never saved. */
enum class HistoryStatus { Identified, Uncertain, Unsupported }

/** What the result screen asks to save. [id] identifies the event, so saving twice is a no-op. */
data class NewHistoryEntry(
    val id: String,
    val identifiedAt: Instant,
    val outcome: IdentificationOutcome,
    val photo: File?,
)

/** The history status for [outcome], or null if it can't be saved (a failure). */
fun historyStatusOf(outcome: IdentificationOutcome): HistoryStatus? = when (outcome) {
    is IdentificationOutcome.Identified -> HistoryStatus.Identified
    is IdentificationOutcome.Uncertain -> HistoryStatus.Uncertain
    is IdentificationOutcome.Unsupported -> HistoryStatus.Unsupported
    is IdentificationOutcome.Failed -> null
}

/** Local, account-free identification history (spec §6.9, §9). */
interface HistoryRepository {
    /** Newest first. */
    fun observeAll(): Flow<List<HistoryRecord>>

    fun observeRecent(limit: Int): Flow<List<HistoryRecord>>

    /** Emits null once the record doesn't exist (e.g. after deletion). */
    fun observe(id: String): Flow<HistoryRecord?>

    fun observeIsSaved(id: String): Flow<Boolean>

    /**
     * Saves [entry] with a downscaled copy of its photo. Idempotent: saving the
     * same id again does nothing. If the photo can't be copied, the record is
     * still saved, without a photo.
     */
    suspend fun save(entry: NewHistoryEntry): Result<Unit>

    /** Deletes the record and its photo. */
    suspend fun delete(id: String): Result<Unit>

    /** Deletes every record and photo. Bundled plant data is not affected. */
    suspend fun clearAll(): Result<Unit>
}
