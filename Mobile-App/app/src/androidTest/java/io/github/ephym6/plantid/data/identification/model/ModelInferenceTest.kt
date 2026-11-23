package io.github.ephym6.plantid.data.identification.model

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ephym6.plantid.data.plant.AssetPlantRepository
import io.github.ephym6.plantid.domain.identification.FailureReason
import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Runs the real packaged model on the device. The test images are synthetic
 * (not plants), so no particular label is expected. This checks that the model
 * loads, runs and returns a valid outcome, and records inference time (spec
 * Sprint 7: measure on real devices). Timings are written to logcat (tag
 * PlantIdModel) and the instrumentation output.
 */
@RunWith(AndroidJUnit4::class)
class ModelInferenceTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val service = ModelIdentificationService(context, AssetPlantRepository(context))
    private val images = File(context.cacheDir, "model_inference_test").apply { mkdirs() }

    @After
    fun cleanUp() {
        images.deleteRecursively()
    }

    private fun image(name: String, width: Int, height: Int, color: Int): File {
        val file = File(images, name)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return file
    }

    @Test
    fun modelLoadsRunsAndReturnsAValidOutcome() = runBlocking {
        val photo = image("green.jpg", 1200, 900, Color.rgb(40, 120, 50))

        val outcome = service.identify(photo)

        assertFalse("model failed: $outcome", outcome is IdentificationOutcome.Failed)
        assertFalse(outcome.model.isDemo)
        assertTrue(outcome.model.version.orEmpty().contains("plantid_v1"))
        if (outcome is IdentificationOutcome.Uncertain) {
            assertTrue(outcome.candidates.size <= DecisionRule.MAX_GUESSES)
            assertTrue(outcome.candidates.none { it.plantId == ModelMetadata.OTHER_PLANT_LABEL })
        }
    }

    @Test
    fun missingPhotoFailsWithoutRunningTheModel() = runBlocking {
        val outcome = service.identify(File(images, "missing.jpg"))

        assertEquals(FailureReason.PhotoUnavailable, (outcome as IdentificationOutcome.Failed).reason)
    }

    @Test
    fun undecodableFileFailsCleanly() = runBlocking {
        val broken = File(images, "broken.jpg").apply { writeText("not an image") }

        val outcome = service.identify(broken)

        assertEquals(FailureReason.ProcessingFailed, (outcome as IdentificationOutcome.Failed).reason)
    }

    @Test
    fun measureInferenceTime() = runBlocking {
        val photos = listOf(
            image("portrait.jpg", 3000, 4000, Color.rgb(30, 140, 60)),
            image("landscape.jpg", 4000, 3000, Color.rgb(200, 180, 40)),
            image("small.jpg", 300, 300, Color.rgb(90, 60, 30)),
        )
        val first = measureTimeMillis { service.identify(photos[0]) } // includes loading the model
        val warm = photos.map { photo -> measureTimeMillis { service.identify(photo) } }

        val report = "first (incl. model load) ${first} ms; warm ${warm.joinToString()} ms " +
            "(each incl. decoding and preprocessing a photo)"
        Log.i("PlantIdModel", report)
        instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PlantID inference: $report\n") })
        // Generous sanity bound only; the real timeout is set from measured numbers.
        assertTrue(report, warm.all { it < 30_000 })
    }
}
