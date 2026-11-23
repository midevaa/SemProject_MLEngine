package io.github.ephym6.plantid.data.plant

import io.github.ephym6.plantid.domain.plant.CareConfidence
import io.github.ephym6.plantid.domain.plant.CareRecommendations
import io.github.ephym6.plantid.domain.plant.PetSafetyBasis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * The catalog is built at runtime from the ML team's files in the repository's
 * `model/` folder (packaged as assets). These tests run against the real files,
 * so a change there that the app can't handle fails here first.
 */
class PlantDataParserTest {

    private val modelDir = File("../../model")
    private val classesFile = File(modelDir, PlantDataParser.CLASSES_ASSET)
    private val careFile = File(modelDir, PlantDataParser.CARE_TIPS_ASSET)
    private val labelsFile = File(modelDir, "labels.txt")

    private fun realCatalog() = PlantDataParser.parse(classesFile.readText(), careFile.readText())

    private fun assumeModelFiles() =
        assumeTrue("model/ files not found", classesFile.isFile && careFile.isFile && labelsFile.isFile)

    @Test
    fun `every model label except other_plant is a plant, in output order`() {
        assumeModelFiles()
        val labels = labelsFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }

        val catalog = realCatalog()

        assertEquals(labels.filter { it != "other_plant" }, catalog.plants.map { it.classLabel })
        assertNull("other_plant is not a plant", catalog.byClassLabel("other_plant"))
        catalog.plants.forEach { plant ->
            assertNotNull("${plant.id} common name", plant.commonName)
            assertNotNull("${plant.id} scientific name", plant.scientificName)
        }
    }

    @Test
    fun `all 30 species have a complete care guide, a source and pet safety`() {
        assumeModelFiles()
        val catalog = realCatalog()

        assertEquals(30, catalog.plants.size)
        catalog.plants.forEach { plant ->
            assertEquals("${plant.id} tips", 4, CareRecommendations.tipsFor(plant.care).size)
            assertNotNull("${plant.id} light level", plant.care?.lightLevel)
            assertNotNull("${plant.id} care source", plant.careInfo?.source)
            assertNotNull("${plant.id} confidence", plant.careInfo?.confidence)
            val pet = plant.petSafety
            assertNotNull("${plant.id} pet safety", pet)
            assertTrue("${plant.id} pet basis recorded", pet!!.basis != PetSafetyBasis.Unknown)
            assertNotNull("${plant.id} pet source", pet.source)
        }
    }

    @Test
    fun `values are taken as given, with confidence, notes and the original wording`() {
        assumeModelFiles()
        val catalog = realCatalog()

        val pothos = catalog.byClassLabel("golden_pothos")!!
        assertEquals("Epipremnum aureum", pothos.scientificName)
        assertEquals(CareConfidence.High, pothos.careInfo?.confidence)
        assertEquals("FloraDB houseplant care sample", pothos.careInfo?.source?.name)
        assertEquals("CC BY-NC 4.0", pothos.careInfo?.source?.license)

        val basil = catalog.byClassLabel("basil")!!
        assertEquals(CareConfidence.Medium, basil.careInfo?.confidence)
        assertNotNull("converted values keep the source's wording", basil.careInfo?.sourceSaid)
        assertFalse(basil.petSafety!!.toxicToDogs)

        assertNotNull("genus-level class explains its values", catalog.byClassLabel("bougainvillea")?.careInfo?.note)
        assertEquals(PetSafetyBasis.ExtensionListed, catalog.byClassLabel("duranta")?.petSafety?.basis)
        assertEquals(PetSafetyBasis.GenusInferred, catalog.byClassLabel("ming_aralia")?.petSafety?.basis)
        assertEquals(CareConfidence.Low, catalog.byClassLabel("ming_aralia")?.careInfo?.confidence)
    }

    @Test
    fun `every cited source site has a proper name`() {
        assumeModelFiles()
        val knownNames = PlantDataParser.KNOWN_SOURCES.values.map { it.first } + PlantDataParser.ASPCA_VIA_FLORADB
        val unnamed = realCatalog().sources.filter { it.name !in knownNames }
        assertTrue("add these sites to KNOWN_SOURCES: $unnamed", unnamed.isEmpty())
    }

    @Test
    fun `sources are named by site, unknown sites by their address`() {
        assertEquals("ASPCA Animal Poison Control", PlantDataParser.sourceFor("https://www.aspca.org/x")?.name)
        assertEquals("UF/IFAS Gardening Solutions", PlantDataParser.sourceFor("https://gardeningsolutions.ifas.ufl.edu/p")?.name)
        assertEquals("example.org", PlantDataParser.sourceFor("https://example.org/a")?.name)
        assertNull(PlantDataParser.sourceFor(null))
    }

    @Test
    fun `missing optional fields are allowed`() {
        val catalog = PlantDataParser.parse(
            """[{"index":0,"slug":"x","common":"X"},{"index":1,"slug":"other_plant"}]""",
            """{"x":{"watering_frequency_days":7}}""",
        )
        val plant = catalog.plants.single()
        assertEquals(7, plant.care?.wateringIntervalDays)
        assertNull(plant.care?.lightLevel)
        assertNull("no toxicity values, no pet safety", plant.petSafety)
        assertNull(plant.careInfo?.source)
    }

    @Test
    fun `malformed or mismatched files are rejected`() {
        val classes = """[{"index":0,"slug":"x"}]"""
        assertThrows(IllegalArgumentException::class.java) { PlantDataParser.parse("not json", "{}") }
        assertThrows(IllegalArgumentException::class.java) { PlantDataParser.parse(classes, "not json") }
        assertThrows(IllegalArgumentException::class.java) {
            PlantDataParser.parse(classes, """{"unknown_label":{}}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PlantDataParser.parse("""[{"index":0,"slug":"x"},{"index":1,"slug":"x"}]""", "{}")
        }
    }
}
