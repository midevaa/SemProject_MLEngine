package io.github.ephym6.plantid.domain.identification

import kotlinx.serialization.Serializable
import java.io.File

/**
 * The boundary between the app and whatever identifies plants (DECISIONS D6).
 * The UI depends only on this interface, never on model-specific APIs.
 *
 * Implementations:
 * - `ModelIdentificationService`: the ML team's PlantID model, run on the phone
 *   with LiteRT. Used in every app build.
 * - `MockIdentificationService`: deterministic, clearly labelled demo outcomes,
 *   used only by device tests.
 *
 * Implementations must be main-safe: do image decoding, preprocessing and
 * inference off the main thread. They should return [IdentificationOutcome.Failed]
 * rather than throw, and must never fall back to a fake successful result.
 */
interface IdentificationService {

    /** Describes the identifier, for labelling results ("demo") and diagnostics. */
    val modelInfo: ModelInfo

    suspend fun identify(photo: File): IdentificationOutcome
}

/**
 * @param isDemo true for the mock. Every result from a demo identifier must be
 *   visibly labelled as demo output, never presented as a real prediction.
 */
@Serializable
data class ModelInfo(
    val name: String,
    val version: String?,
    val isDemo: Boolean,
)
