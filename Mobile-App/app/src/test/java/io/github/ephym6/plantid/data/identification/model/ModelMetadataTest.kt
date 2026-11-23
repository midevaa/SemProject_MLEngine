package io.github.ephym6.plantid.data.identification.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.ByteOrder

class ModelMetadataTest {

    /** The ML team's files, packaged as app assets from the repository's root model/ folder. */
    private val modelDir = File("../../model")
    private val metadataFile = File(modelDir, ModelMetadata.METADATA_ASSET)
    private val labelsFile = File(modelDir, ModelMetadata.LABELS_ASSET)

    @Test
    fun `the shipped metadata and labels are consistent and parse`() {
        assumeTrue("model/ folder not found", metadataFile.isFile && labelsFile.isFile)

        val metadata = ModelMetadata.parse(metadataFile.readText(), labelsFile.readText())

        assertEquals(224, metadata.inputSize)
        assertEquals(31, metadata.labels.size)
        assertEquals(ModelMetadata.OTHER_PLANT_LABEL, metadata.labels[metadata.otherPlantIndex])
        assertEquals(0.7, metadata.rule.temperature, 1e-9)
        assertEquals(0.72, metadata.rule.threshold, 1e-9)
        assertEquals("PlantID v1", metadata.modelName)
        assertEquals(true, File(modelDir, metadata.modelFile).isFile)
    }

    @Test
    fun `inconsistent files are rejected`() {
        assumeTrue("model/ folder not found", metadataFile.isFile && labelsFile.isFile)
        val metadata = metadataFile.readText()
        val labels = labelsFile.readText()

        // One label missing: output shape no longer matches.
        val shorter = labels.lines().filter { it.isNotBlank() }.drop(1).joinToString("\n")
        assertThrows(IllegalArgumentException::class.java) { ModelMetadata.parse(metadata, shorter) }
        assertThrows(IllegalArgumentException::class.java) { ModelMetadata.parse("{ not json", labels) }
    }

    @Test
    fun `pixels are packed as raw RGB floats, row by row`() {
        val pixels = intArrayOf(0xFF102030.toInt(), 0x80FFFFFF.toInt())

        val buffer = ImagePreprocessor.toFloatRgbBuffer(pixels)

        assertEquals(ByteOrder.nativeOrder(), buffer.order())
        assertEquals(2 * 3 * 4, buffer.remaining())
        val values = FloatArray(6) { buffer.getFloat() }
        // 0–255, no normalisation; alpha ignored.
        assertEquals(listOf(16f, 32f, 48f, 255f, 255f, 255f), values.toList())
    }
}
