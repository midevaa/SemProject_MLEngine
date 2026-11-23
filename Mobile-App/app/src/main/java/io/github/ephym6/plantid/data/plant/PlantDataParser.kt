package io.github.ephym6.plantid.data.plant

import io.github.ephym6.plantid.domain.plant.CareConfidence
import io.github.ephym6.plantid.domain.plant.CareData
import io.github.ephym6.plantid.domain.plant.CareInfo
import io.github.ephym6.plantid.domain.plant.ContentSource
import io.github.ephym6.plantid.domain.plant.PetSafety
import io.github.ephym6.plantid.domain.plant.PetSafetyBasis
import io.github.ephym6.plantid.domain.plant.Plant
import io.github.ephym6.plantid.domain.plant.PlantCatalog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI

/**
 * Builds the plant catalog from the ML team's files in `model/` (packaged as
 * assets, DECISIONS D40):
 * - `classes.json`: the model's classes (label, names, category);
 * - `care_tips.json`: care values and pet safety per label.
 *
 * Every class except `other_plant` becomes a plant. Values are taken as given:
 * nothing is filled in or invented. Pure Kotlin, so it's unit-tested against the
 * real files.
 */
internal object PlantDataParser {

    const val CLASSES_ASSET = "classes.json"
    const val CARE_TIPS_ASSET = "care_tips.json"
    private const val OTHER_PLANT = "other_plant"

    private val json = Json { ignoreUnknownKeys = true }

    /** @throws IllegalArgumentException if a file is malformed or the two don't match. */
    fun parse(classesJson: String, careTipsJson: String): PlantCatalog {
        val classes = decode<List<ClassDto>>(classesJson, CLASSES_ASSET)
        val care = decode<Map<String, CareDto>>(careTipsJson, CARE_TIPS_ASSET)
        val labels = classes.map { it.slug }
        require(labels.size == labels.toSet().size) { "$CLASSES_ASSET has duplicate labels" }
        val unknown = care.keys - labels.toSet()
        require(unknown.isEmpty()) { "$CARE_TIPS_ASSET has entries for unknown labels: $unknown" }

        val plants = classes.sortedBy { it.index }.filter { it.slug != OTHER_PLANT }.map { it.toPlant(care[it.slug]) }
        require(plants.isNotEmpty()) { "$CLASSES_ASSET has no plants" }
        return PlantCatalog(plants)
    }

    private inline fun <reified T> decode(text: String, file: String): T = try {
        json.decodeFromString<T>(text)
    } catch (e: kotlinx.serialization.SerializationException) {
        throw IllegalArgumentException("$file is malformed", e)
    }

    private fun ClassDto.toPlant(care: CareDto?) = Plant(
        id = slug,
        classLabel = slug,
        commonName = common ?: care?.commonName,
        scientificName = scientific?.takeUnless { it.isBlank() || it == "-" } ?: care?.scientificName,
        family = care?.family,
        care = care?.let {
            CareData(
                lightLevel = it.lightLevel?.takeIf(String::isNotBlank),
                lightLux = range(it.minLux, it.maxLux),
                temperatureCelsius = range(it.minTempCelsius, it.maxTempCelsius),
                humidityPercent = it.idealHumidityPercent,
                wateringIntervalDays = it.wateringFrequencyDays,
            )
        },
        careInfo = care?.let {
            CareInfo(
                source = sourceFor(it.sourceUrl),
                confidence = when (it.careConfidence?.lowercase()) {
                    "high" -> CareConfidence.High
                    "medium" -> CareConfidence.Medium
                    "low" -> CareConfidence.Low
                    else -> null
                },
                note = it.note?.takeIf(String::isNotBlank),
                sourceSaid = it.sourceSaid?.takeIf(String::isNotBlank),
            )
        },
        petSafety = care?.let { it.toPetSafety() },
    )

    private fun CareDto.toPetSafety(): PetSafety? {
        val dogs = isToxicToDogs ?: return null
        val cats = isToxicToCats ?: return null
        val basis = when (toxicityStatus) {
            "aspca_verified" -> PetSafetyBasis.AspcaVerified
            "extension_listed" -> PetSafetyBasis.ExtensionListed
            "aspca_genus_inferred" -> PetSafetyBasis.GenusInferred
            else -> PetSafetyBasis.Unknown
        }
        // Entries without their own toxicity link take it from the FloraDB sample, which records ASPCA status
        // (verified, or inferred from the genus). Anything else gets no source rather than a guessed one.
        val fromFloraDb = sourceFor(sourceUrl)?.name == KNOWN_SOURCES.getValue("huggingface.co").first
        val source = toxicitySourceUrl?.let(::sourceFor)
            ?: if (fromFloraDb) ContentSource(ASPCA_VIA_FLORADB, null, null) else null
        return PetSafety(
            toxicToDogs = dogs >= 0.5,
            toxicToCats = cats >= 0.5,
            details = toxicitySymptoms?.takeIf(String::isNotBlank),
            basis = basis,
            source = source,
        )
    }

    private fun range(min: Int?, max: Int?): IntRange? = if (min != null && max != null) min..max else null

    /** Names the publishers the ML team cites; an unknown site is credited by its address. */
    internal fun sourceFor(url: String?): ContentSource? {
        if (url.isNullOrBlank()) return null
        val host = runCatching { URI(url).host }.getOrNull()?.removePrefix("www.") ?: return ContentSource(url, url, null)
        val known = KNOWN_SOURCES.entries.firstOrNull { (domain, _) -> host == domain || host.endsWith(".$domain") }
        return if (known != null) {
            ContentSource(known.value.first, url, known.value.second)
        } else {
            ContentSource(host, url, null)
        }
    }

    /** Domain → (name, licence). Licence only where it has been checked (FloraDB: dataset card, D23). */
    internal val KNOWN_SOURCES = mapOf(
        "huggingface.co" to ("FloraDB houseplant care sample" to "CC BY-NC 4.0"),
        "ces.ncsu.edu" to ("NC State Extension Gardener Plant Toolbox" to null),
        "missouribotanicalgarden.org" to ("Missouri Botanical Garden Plant Finder" to null),
        "ifas.ufl.edu" to ("UF/IFAS Gardening Solutions" to null),
        "aspca.org" to ("ASPCA Animal Poison Control" to null),
    )

    internal const val ASPCA_VIA_FLORADB = "ASPCA (as recorded in the FloraDB sample)"

    @Serializable
    private data class ClassDto(
        val index: Int,
        val slug: String,
        val common: String? = null,
        val scientific: String? = null,
        val category: String? = null,
    )

    @Serializable
    private data class CareDto(
        @SerialName("common_name") val commonName: String? = null,
        @SerialName("scientific_name") val scientificName: String? = null,
        val family: String? = null,
        @SerialName("light_requirement_level") val lightLevel: String? = null,
        @SerialName("watering_frequency_days") val wateringFrequencyDays: Int? = null,
        @SerialName("min_lux") val minLux: Int? = null,
        @SerialName("max_lux") val maxLux: Int? = null,
        @SerialName("min_temp_celsius") val minTempCelsius: Int? = null,
        @SerialName("max_temp_celsius") val maxTempCelsius: Int? = null,
        @SerialName("ideal_humidity_percent") val idealHumidityPercent: Int? = null,
        @SerialName("is_toxic_to_dogs") val isToxicToDogs: Double? = null,
        @SerialName("is_toxic_to_cats") val isToxicToCats: Double? = null,
        @SerialName("toxicity_symptoms") val toxicitySymptoms: String? = null,
        @SerialName("toxicity_status") val toxicityStatus: String? = null,
        @SerialName("care_confidence") val careConfidence: String? = null,
        @SerialName("source_url") val sourceUrl: String? = null,
        @SerialName("source_said") val sourceSaid: String? = null,
        @SerialName("toxicity_source_url") val toxicitySourceUrl: String? = null,
        val note: String? = null,
    )
}
