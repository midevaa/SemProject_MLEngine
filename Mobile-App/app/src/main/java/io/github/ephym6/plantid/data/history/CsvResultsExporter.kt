package io.github.ephym6.plantid.data.history

import android.content.Context
import androidx.core.content.FileProvider
import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.ResultsCsv
import io.github.ephym6.plantid.domain.history.ResultsExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Writes saved results to `cacheDir/exports/plantid-results-<time>.csv` and
 * returns a FileProvider URI. The file leaves the device only if the user sends
 * it somewhere from the share sheet. Only the newest export is kept.
 */
class CsvResultsExporter(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) : ResultsExporter {

    private val appContext = context.applicationContext
    private val directory = File(appContext.cacheDir, DIRECTORY_NAME)

    override suspend fun export(records: List<HistoryRecord>): Result<String> = withContext(ioDispatcher) {
        try {
            directory.mkdirs()
            directory.listFiles()?.forEach { it.delete() }
            val file = File(directory, "plantid-results-${now().format(FILE_TIME)}.csv")
            file.writeText(ResultsCsv.format(records), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(appContext, appContext.packageName + AUTHORITY_SUFFIX, file)
            Result.success(uri.toString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal companion object {
        /** Must match `res/xml/file_paths.xml`. */
        const val DIRECTORY_NAME = "exports"
        const val AUTHORITY_SUFFIX = ".fileprovider"
        private val FILE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
