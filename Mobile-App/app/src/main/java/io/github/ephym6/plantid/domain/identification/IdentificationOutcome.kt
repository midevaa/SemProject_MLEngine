package io.github.ephym6.plantid.domain.identification

import kotlinx.serialization.Serializable

/**
 * Product-level result of one identification attempt (spec §6.5 statuses).
 *
 * How raw model output maps to these statuses (thresholds, uncertainty policy)
 * is an AI-integration decision (DECISIONS Q2). The UI must not invent one.
 *
 * Serializable so a finished result survives Android stopping the app
 * (restored as-is, not re-identified).
 */
@Serializable
sealed interface IdentificationOutcome {
    val model: ModelInfo

    /** A supported class passed the agreed decision rule. */
    @Serializable
    data class Identified(
        override val model: ModelInfo,
        /** Classifier label, e.g. a training class folder name. */
        val classId: String,
        val plant: PlantSummary,
        /** Calibrated scores if the model provides them; the answer's is shown as its confidence (D43). */
        val scores: List<ClassScore> = emptyList(),
    ) : IdentificationOutcome {
        /** Calibrated probability of this answer, or null if the identifier gives no scores (e.g. demo). */
        val confidence: Float? get() = scores.scoreFor(classId)
    }

    /**
     * The model returned something, but the decision rule says it is not
     * confident. Must never be displayed as a species identification.
     *
     * [candidates] are the model's top guesses (at most 3, best first), shown only
     * as unconfirmed "possible matches" (DECISIONS D37), never as a result.
     */
    @Serializable
    data class Uncertain(
        override val model: ModelInfo,
        val scores: List<ClassScore> = emptyList(),
        val candidates: List<PlantSummary> = emptyList(),
    ) : IdentificationOutcome

    /** No acceptable supported class: an unsupported plant, or not a plant at all. */
    @Serializable
    data class Unsupported(
        override val model: ModelInfo,
    ) : IdentificationOutcome

    /** The photo couldn't be processed or inference failed. */
    @Serializable
    data class Failed(
        override val model: ModelInfo,
        val reason: FailureReason,
    ) : IdentificationOutcome
}

/**
 * A model score for one class. For the PlantID model this is the calibrated
 * probability (temperature-scaled on the ML team's validation photos): an
 * estimate, never a guarantee.
 */
@Serializable
data class ClassScore(val classId: String, val score: Float)

/** The score for [classId], if present. */
fun List<ClassScore>.scoreFor(classId: String): Float? = firstOrNull { it.classId == classId }?.score

@Serializable
enum class FailureReason {
    /** The stored photo is missing or couldn't be decoded. */
    PhotoUnavailable,

    /** The identifier couldn't be loaded. */
    ModelUnavailable,

    /** Preprocessing or inference failed. */
    ProcessingFailed,

    /** No result within the app's time limit. */
    TimedOut,
}

/**
 * The plant information shown on a result. Separate from care guidance, which
 * maps a plant record to curated content (spec §7.3, Sprint 4).
 * Optional fields may be missing and the UI must cope.
 */
@Serializable
data class PlantSummary(
    val plantId: String,
    val commonName: String?,
    val scientificName: String?,
    val description: String?,
)
