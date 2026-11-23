package io.github.ephym6.plantid.data.identification.model

import io.github.ephym6.plantid.data.identification.model.DecisionRule.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp
import kotlin.math.ln

/** The ML team's rule from model_metadata.json: p = softmax(log(probs)/T); other or max(p) < threshold → not sure. */
class DecisionRuleTest {

    private val rule = DecisionRule(temperature = 0.7, threshold = 0.72, otherPlantIndex = 2)

    /** Reference implementation written straight from the metadata formula. */
    private fun reference(probs: FloatArray, t: Double): DoubleArray {
        val e = probs.map { exp(ln(it.toDouble()) / t) }
        return e.map { it / e.sum() }.toDoubleArray()
    }

    @Test
    fun `calibration matches the metadata formula and sums to one`() {
        val probs = floatArrayOf(0.6f, 0.3f, 0.05f, 0.05f)
        val calibrated = rule.calibrate(probs)
        val expected = reference(probs, 0.7)
        calibrated.indices.forEach { assertEquals(expected[it], calibrated[it], 1e-9) }
        assertEquals(1.0, calibrated.sum(), 1e-9)
        assertTrue("T < 1 sharpens the top answer", calibrated[0] > 0.6)
    }

    @Test
    fun `confident supported answer is identified`() {
        val decision = rule.decide(floatArrayOf(0.85f, 0.10f, 0.03f, 0.02f))
        assertTrue(decision is Decision.Identified)
        assertEquals(0, (decision as Decision.Identified).index)
    }

    @Test
    fun `top answer other_plant is unsupported even when confident`() {
        assertTrue(rule.decide(floatArrayOf(0.02f, 0.03f, 0.93f, 0.02f)) is Decision.Unsupported)
    }

    @Test
    fun `below the threshold is uncertain with up to three guesses, never other_plant`() {
        val decision = rule.decide(floatArrayOf(0.40f, 0.30f, 0.25f, 0.05f))
        assertTrue(decision is Decision.Uncertain)
        assertEquals(listOf(0, 1, 3), (decision as Decision.Uncertain).guesses)
    }

    @Test
    fun `threshold is compared after calibration, and equal to it counts as identified`() {
        // Raw top 0.65 is below 0.72, but after T = 0.7 it is about 0.765: identified.
        assertTrue(rule.decide(floatArrayOf(0.65f, 0.25f, 0.05f, 0.05f)) is Decision.Identified)
        // Raw top 0.6 becomes about 0.700 after calibration: still below 0.72, so not sure.
        assertTrue(rule.decide(floatArrayOf(0.6f, 0.3f, 0.05f, 0.05f)) is Decision.Uncertain)

        // Exactly at the threshold: the rule says "max(p) < threshold" is not sure, so equal is identified.
        val probs = floatArrayOf(0.5f, 0.25f, 0.05f, 0.2f)
        val top = rule.calibrate(probs)[0]
        assertTrue(DecisionRule(temperature = 0.7, threshold = top, otherPlantIndex = 2).decide(probs) is Decision.Identified)
        assertTrue(
            DecisionRule(temperature = 0.7, threshold = top + 1e-9, otherPlantIndex = 2).decide(probs) is Decision.Uncertain,
        )
    }

    @Test
    fun `zero probabilities don't break the maths`() {
        val decision = rule.decide(floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f))
        assertTrue(decision is Decision.Identified)
        assertTrue(decision.calibrated.all { it.isFinite() })
    }

    @Test
    fun `invalid settings and inputs are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { DecisionRule(0.0, 0.5, 0) }
        assertThrows(IllegalArgumentException::class.java) { DecisionRule(1.0, 1.5, 0) }
        assertThrows(IllegalArgumentException::class.java) { rule.decide(floatArrayOf(0.5f, 0.5f)) }
    }
}
