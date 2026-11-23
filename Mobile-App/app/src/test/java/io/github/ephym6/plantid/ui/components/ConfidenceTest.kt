package io.github.ephym6.plantid.ui.components

import io.github.ephym6.plantid.domain.identification.ClassScore
import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.domain.identification.PlantSummary
import io.github.ephym6.plantid.domain.identification.scoreFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/** D43: calibrated confidence shown as a whole percentage, never as certainty. */
class ConfidenceTest {

    private val us = Locale.US

    @Test
    fun `percentages are rounded to whole numbers`() {
        assertEquals("87%", formatConfidence(0.87f, us))
        assertEquals("72%", formatConfidence(0.7249f, us))
        assertEquals("99%", formatConfidence(0.994f, us))
        assertEquals("1%", formatConfidence(0.005f, us))
    }

    @Test
    fun `never shows 100 or 0 percent`() {
        assertEquals(">99%", formatConfidence(0.995f, us))
        assertEquals(">99%", formatConfidence(1.0f, us))
        assertEquals("<1%", formatConfidence(0.004f, us))
        assertEquals("<1%", formatConfidence(0.0f, us))
    }

    @Test
    fun `an identified result's confidence is its own class score`() {
        val model = ModelInfo("PlantID v1", "plantid_v1.tflite float16", isDemo = false)
        val plant = PlantSummary("golden_pothos", "Golden pothos", "Epipremnum aureum", null)
        val scores = listOf(ClassScore("golden_pothos", 0.91f), ClassScore("monstera", 0.05f))

        assertEquals(0.91f, IdentificationOutcome.Identified(model, "golden_pothos", plant, scores).confidence)
        assertNull("no scores (demo): no confidence", IdentificationOutcome.Identified(model, "golden_pothos", plant).confidence)
        assertEquals(0.05f, scores.scoreFor("monstera"))
        assertNull(scores.scoreFor("zz_plant"))
    }
}
