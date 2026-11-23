package io.github.ephym6.plantid.data.identification.model

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

/**
 * The ML team's "not sure" rule, implemented exactly as written in
 * `model_metadata.json` (it resolves DECISIONS Q2):
 *
 * ```
 * p = softmax(log(probs) / temperature)
 * if argmax(p) == other_plant_index or max(p) < threshold: "not sure"
 * else: labels[argmax(p)]
 * ```
 *
 * App mapping (D37): top answer `other_plant` → **Unsupported**; below the
 * threshold → **Uncertain** with up to [MAX_GUESSES] guesses (never
 * `other_plant`); otherwise → **Identified**. The thresholds come from the
 * metadata file; the app does not invent them.
 */
data class DecisionRule(
    val temperature: Double,
    val threshold: Double,
    val otherPlantIndex: Int,
) {
    init {
        require(temperature > 0.0) { "temperature must be positive" }
        require(threshold in 0.0..1.0) { "threshold must be between 0 and 1" }
    }

    sealed interface Decision {
        /** Calibrated probabilities (after temperature), same order as the labels. */
        val calibrated: DoubleArray

        data class Identified(val index: Int, override val calibrated: DoubleArray) : Decision
        data class Uncertain(val guesses: List<Int>, override val calibrated: DoubleArray) : Decision
        data class Unsupported(override val calibrated: DoubleArray) : Decision
    }

    fun decide(probabilities: FloatArray): Decision {
        require(probabilities.isNotEmpty() && otherPlantIndex in probabilities.indices) {
            "expected ${otherPlantIndex + 1}+ probabilities, got ${probabilities.size}"
        }
        val calibrated = calibrate(probabilities)
        val top = calibrated.indices.maxBy { calibrated[it] }
        return when {
            top == otherPlantIndex -> Decision.Unsupported(calibrated)
            calibrated[top] < threshold -> Decision.Uncertain(
                guesses = calibrated.indices
                    .filter { it != otherPlantIndex }
                    .sortedByDescending { calibrated[it] }
                    .take(MAX_GUESSES),
                calibrated = calibrated,
            )
            else -> Decision.Identified(top, calibrated)
        }
    }

    /** softmax(log(p) / T), computed stably. Zero probabilities are clamped so log is finite. */
    internal fun calibrate(probabilities: FloatArray): DoubleArray {
        val logits = DoubleArray(probabilities.size) { ln(max(probabilities[it].toDouble(), MIN_PROBABILITY)) / temperature }
        val maxLogit = logits.max()
        val exps = DoubleArray(logits.size) { exp(logits[it] - maxLogit) }
        val sum = exps.sum()
        return DoubleArray(exps.size) { exps[it] / sum }
    }

    companion object {
        const val MAX_GUESSES = 3
        private const val MIN_PROBABILITY = 1e-12
    }
}
