package io.github.ephym6.plantid.data.history

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * One saved identification. Columns are a snapshot of what was shown (see
 * HistoryRecord). Schema changes need a new database version plus a migration;
 * the exported schemas are in app/schemas/.
 */
@Entity(tableName = "history", indices = [Index("identified_at")])
data class HistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "identified_at") val identifiedAtMillis: Long,
    /** HistoryStatus name. */
    val status: String,
    @ColumnInfo(name = "class_id") val classId: String?,
    @ColumnInfo(name = "plant_id") val plantId: String?,
    @ColumnInfo(name = "common_name") val commonName: String?,
    @ColumnInfo(name = "scientific_name") val scientificName: String?,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "model_version") val modelVersion: String?,
    @ColumnInfo(name = "is_demo") val isDemo: Boolean,
    /** File name in the history photo directory, or null. */
    @ColumnInfo(name = "photo_file") val photoFileName: String?,
    /** Calibrated confidence of an identified result (added in schema 2; null for older records). */
    @ColumnInfo(name = "confidence") val confidence: Float? = null,
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY identified_at DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY identified_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE id = :id")
    fun observe(id: String): Flow<HistoryEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM history WHERE id = :id)")
    fun observeExists(id: String): Flow<Boolean>

    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun get(id: String): HistoryEntity?

    /** IGNORE makes saving idempotent: a second save of the same event is a no-op. Returns -1 then. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("DELETE FROM history")
    suspend fun deleteAll()
}

/**
 * Schema history: 1 = Sprint 5; 2 = adds `confidence` (Sprint 7, auto-migrated,
 * existing records keep a null confidence). Exported schemas: app/schemas/.
 */
@Database(
    entities = [HistoryEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class PlantIdDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        private const val NAME = "plantid.db"

        fun create(context: Context): PlantIdDatabase =
            Room.databaseBuilder(context.applicationContext, PlantIdDatabase::class.java, NAME).build()
    }
}
