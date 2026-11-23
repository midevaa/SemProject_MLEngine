package io.github.ephym6.plantid.data.history

import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryRepository
import io.github.ephym6.plantid.domain.history.HistoryStatus
import io.github.ephym6.plantid.domain.history.NewHistoryEntry
import io.github.ephym6.plantid.domain.history.historyStatusOf
import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import io.github.ephym6.plantid.domain.identification.ModelInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant

/** [HistoryRepository] backed by Room (records) and [HistoryPhotoStore] (photos). */
class RoomHistoryRepository(
    private val dao: HistoryDao,
    private val photos: HistoryPhotoStore,
) : HistoryRepository {

    override fun observeAll(): Flow<List<HistoryRecord>> =
        dao.observeAll().map { list -> list.mapNotNull { it.toRecord() } }

    override fun observeRecent(limit: Int): Flow<List<HistoryRecord>> =
        dao.observeRecent(limit).map { list -> list.mapNotNull { it.toRecord() } }

    override fun observe(id: String): Flow<HistoryRecord?> =
        dao.observe(id).map { it?.toRecord() }.distinctUntilChanged()

    override fun observeIsSaved(id: String): Flow<Boolean> = dao.observeExists(id).distinctUntilChanged()

    override suspend fun save(entry: NewHistoryEntry): Result<Unit> = runCatchingNonCancellation {
        val status = requireNotNull(historyStatusOf(entry.outcome)) { "failed outcomes can't be saved" }
        if (dao.get(entry.id) != null) return@runCatchingNonCancellation // already saved
        val photoName = entry.photo?.let { photos.saveCopy(it, entry.id) }
        val plant = (entry.outcome as? IdentificationOutcome.Identified)?.plant
        val inserted = dao.insert(
            HistoryEntity(
                id = entry.id,
                identifiedAtMillis = entry.identifiedAt.toEpochMilli(),
                status = status.name,
                classId = (entry.outcome as? IdentificationOutcome.Identified)?.classId,
                plantId = plant?.plantId,
                commonName = plant?.commonName,
                scientificName = plant?.scientificName,
                modelName = entry.outcome.model.name,
                modelVersion = entry.outcome.model.version,
                isDemo = entry.outcome.model.isDemo,
                photoFileName = photoName,
                confidence = (entry.outcome as? IdentificationOutcome.Identified)?.confidence,
            ),
        )
        // A concurrent save won the race: don't leave a second copy's file behind.
        if (inserted == -1L && photoName != null && dao.get(entry.id)?.photoFileName != photoName) {
            photos.delete(photoName)
        }
    }

    override suspend fun delete(id: String): Result<Unit> = runCatchingNonCancellation {
        val photoName = dao.get(id)?.photoFileName
        dao.delete(id)
        photoName?.let { photos.delete(it) }
    }

    override suspend fun clearAll(): Result<Unit> = runCatchingNonCancellation {
        dao.deleteAll()
        photos.deleteAll()
    }

    private fun HistoryEntity.toRecord(): HistoryRecord? {
        // Skip rows with an unknown status rather than crash (e.g. written by a newer build).
        val status = HistoryStatus.entries.firstOrNull { it.name == status } ?: return null
        return HistoryRecord(
            id = id,
            identifiedAt = Instant.ofEpochMilli(identifiedAtMillis),
            status = status,
            classId = classId,
            plantId = plantId,
            commonName = commonName,
            scientificName = scientificName,
            model = ModelInfo(name = modelName, version = modelVersion, isDemo = isDemo),
            photo = photoFileName?.let(photos::fileFor),
            confidence = confidence,
        )
    }

    private inline fun runCatchingNonCancellation(block: () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
