package io.github.ephym6.plantid.data.plant

import android.content.Context
import io.github.ephym6.plantid.domain.plant.PlantCatalog
import io.github.ephym6.plantid.domain.plant.PlantRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Reads the ML team's `classes.json` and `care_tips.json` (packaged from the
 * repository's `model/` folder) once, off the main thread, and caches the
 * catalog. Offline. A failure isn't cached, so a later call can retry.
 */
class AssetPlantRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PlantRepository {

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var cached: PlantCatalog? = null

    override suspend fun catalog(): Result<PlantCatalog> = mutex.withLock {
        cached?.let { return@withLock Result.success(it) }
        withContext(ioDispatcher) {
            try {
                val classes = read(PlantDataParser.CLASSES_ASSET)
                val care = read(PlantDataParser.CARE_TIPS_ASSET)
                Result.success(PlantDataParser.parse(classes, care).also { cached = it })
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: IllegalArgumentException) {
                Result.failure(e)
            }
        }
    }

    private fun read(asset: String) = appContext.assets.open(asset).bufferedReader().use { it.readText() }
}
