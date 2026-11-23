package io.github.ephym6.plantid.testing

import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryRepository
import io.github.ephym6.plantid.domain.history.NewHistoryEntry
import io.github.ephym6.plantid.domain.history.historyStatusOf
import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException

/** In-memory [HistoryRepository] for JVM tests. Set the fail flags to simulate storage errors. */
class FakeHistoryRepository : HistoryRepository {

    val records = MutableStateFlow<List<HistoryRecord>>(emptyList())
    val savedEntries = mutableListOf<NewHistoryEntry>()
    var failReads = false
    var failSave = false
    var failDelete = false
    var failClear = false
    var deleteCalls = 0

    override fun observeAll(): Flow<List<HistoryRecord>> = readable { records }

    override fun observeRecent(limit: Int): Flow<List<HistoryRecord>> = readable { records.map { it.take(limit) } }

    override fun observe(id: String): Flow<HistoryRecord?> = readable { records.map { list -> list.find { it.id == id } } }

    override fun observeIsSaved(id: String): Flow<Boolean> = readable { records.map { list -> list.any { it.id == id } } }

    override suspend fun save(entry: NewHistoryEntry): Result<Unit> {
        if (failSave) return Result.failure(IOException("disk full"))
        val status = historyStatusOf(entry.outcome) ?: return Result.failure(IllegalArgumentException())
        savedEntries += entry
        if (records.value.none { it.id == entry.id }) {
            val plant = (entry.outcome as? IdentificationOutcome.Identified)?.plant
            records.value = listOf(
                HistoryRecord(
                    id = entry.id,
                    identifiedAt = entry.identifiedAt,
                    status = status,
                    classId = (entry.outcome as? IdentificationOutcome.Identified)?.classId,
                    plantId = plant?.plantId,
                    commonName = plant?.commonName,
                    scientificName = plant?.scientificName,
                    model = entry.outcome.model,
                    photo = entry.photo,
                    confidence = (entry.outcome as? IdentificationOutcome.Identified)?.confidence,
                ),
            ) + records.value
        }
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        deleteCalls++
        if (failDelete) return Result.failure(IOException("locked"))
        records.value = records.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun clearAll(): Result<Unit> {
        if (failClear) return Result.failure(IOException("locked"))
        records.value = emptyList()
        return Result.success(Unit)
    }

    private fun <T> readable(source: () -> Flow<T>): Flow<T> = flow {
        if (failReads) throw IOException("database unreadable")
        source().collect { emit(it) }
    }
}
