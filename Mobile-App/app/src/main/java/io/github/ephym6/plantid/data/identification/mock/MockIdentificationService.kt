package io.github.ephym6.plantid.data.identification.mock

import io.github.ephym6.plantid.domain.identification.FailureReason
import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import io.github.ephym6.plantid.domain.identification.IdentificationService
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.domain.identification.PlantSummary
import io.github.ephym6.plantid.domain.plant.PlantRepository
import io.github.ephym6.plantid.domain.plant.toSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * DEMO ONLY. Returns a predictable outcome for the selected [DemoScenario]
 * after a short fixed delay, so the UI flow can be built and tested before the
 * real model exists (spec §8.1). It does **not** analyse the photo (it only
 * checks the file still exists), and its results are not predictions.
 * [modelInfo] is marked `isDemo` so the UI labels every result as demo output.
 *
 * "Identified" always returns the same supported plant ([DEMO_CLASS_LABEL])
 * from the bundled catalog, so the care guide can be demonstrated. "Missing
 * details" returns a fictional plant with no catalog entry ([DemoPlants]).
 * No scores are produced.
 */
class MockIdentificationService(
    private val plants: PlantRepository,
    private val responseDelayMillis: Long = DEFAULT_DELAY_MILLIS,
    private val slowDelayMillis: Long = SLOW_DELAY_MILLIS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : IdentificationService, DemoScenarioController {

    override val modelInfo = ModelInfo(name = "PlantID demo identifier", version = null, isDemo = true)

    private val _scenario = MutableStateFlow(DemoScenario.Identified)
    override val scenario: StateFlow<DemoScenario> = _scenario.asStateFlow()

    override fun select(scenario: DemoScenario) {
        _scenario.value = scenario
    }

    override suspend fun identify(photo: File): IdentificationOutcome {
        // Read once at the start so a change mid-request can't mix scenarios.
        val scenario = _scenario.value
        delay(if (scenario == DemoScenario.Slow) slowDelayMillis else responseDelayMillis)
        // Behave like a real identifier if the photo has gone (e.g. cache cleared).
        if (!withContext(ioDispatcher) { photo.isFile }) {
            return IdentificationOutcome.Failed(modelInfo, FailureReason.PhotoUnavailable)
        }
        return when (scenario) {
            DemoScenario.Identified, DemoScenario.Slow -> identifiedAs(DEMO_CLASS_LABEL)
            DemoScenario.MissingDetails ->
                IdentificationOutcome.Identified(modelInfo, DemoPlants.CLASS_UNLISTED, DemoPlants.unlisted)
            DemoScenario.Uncertain -> IdentificationOutcome.Uncertain(modelInfo)
            DemoScenario.Unsupported -> IdentificationOutcome.Unsupported(modelInfo)
            DemoScenario.Failure -> IdentificationOutcome.Failed(modelInfo, FailureReason.ProcessingFailed)
        }
    }

    /** Maps a class label to its catalog plant, the same way a real identifier must. */
    private suspend fun identifiedAs(classLabel: String): IdentificationOutcome {
        val plant = plants.catalog().getOrNull()?.byClassLabel(classLabel)
            ?: return IdentificationOutcome.Failed(modelInfo, FailureReason.ProcessingFailed)
        return IdentificationOutcome.Identified(modelInfo, classLabel, plant.toSummary())
    }

    companion object {
        const val DEFAULT_DELAY_MILLIS = 1_200L
        const val SLOW_DELAY_MILLIS = 6_000L

        /** The supported plant every "identified" demo result shows (Golden Pothos). */
        const val DEMO_CLASS_LABEL = "golden_pothos"
    }
}

/** Outcomes the mock can return. Covers the demo-data cases in spec §7.4. */
enum class DemoScenario {
    /** A supported plant from the catalog, with a care guide. */
    Identified,

    /** A fictional plant with only a common name and no care guide. */
    MissingDetails,

    /** Low confidence: no species shown. */
    Uncertain,

    /** No supported plant found. */
    Unsupported,

    /** Inference error; the user can retry. */
    Failure,

    /** Same as [Identified] but slow, to check the loading state. */
    Slow,
}

/** Lets the UI choose the mock's next outcome. Only available while the mock is in use. */
interface DemoScenarioController {
    val scenario: StateFlow<DemoScenario>
    fun select(scenario: DemoScenario)
}

/** A fictional plant record for the "missing details" demo. Not a real species. */
object DemoPlants {
    const val CLASS_UNLISTED = "demo_unlisted"

    /** Only a common name and no catalog entry: checks missing details and "no care guide". */
    val unlisted = PlantSummary(
        plantId = "demo_plant_b",
        commonName = "Demo Plant B",
        scientificName = null,
        description = null,
    )
}
