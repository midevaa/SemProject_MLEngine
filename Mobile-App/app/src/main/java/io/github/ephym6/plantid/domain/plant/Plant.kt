package io.github.ephym6.plantid.domain.plant

import io.github.ephym6.plantid.domain.identification.PlantSummary

/**
 * A supported plant (spec §7.1). Not the same as an identification event: one
 * plant can be identified many times.
 *
 * @param classLabel the model's label for this plant (e.g. "golden_pothos").
 */
data class Plant(
    val id: String,
    val classLabel: String,
    val commonName: String?,
    val scientificName: String?,
    val family: String?,
    val care: CareData?,
    val careInfo: CareInfo? = null,
    val petSafety: PetSafety? = null,
)

/** Care values exactly as the source provides them. Any field may be missing. */
data class CareData(
    /** The source's light category, e.g. "Bright Indirect". */
    val lightLevel: String? = null,
    val lightLux: IntRange? = null,
    val temperatureCelsius: IntRange? = null,
    val humidityPercent: Int? = null,
    /** The source's typical watering interval. Not a schedule to follow regardless of conditions. */
    val wateringIntervalDays: Int? = null,
)

/** Where a plant's care values come from and how sure the ML team is of them. */
data class CareInfo(
    val source: ContentSource?,
    val confidence: CareConfidence?,
    /** The ML team's note, e.g. that a genus-level class uses one species' values. */
    val note: String?,
    /** For values converted from published advice: that advice in its original wording. */
    val sourceSaid: String?,
)

enum class CareConfidence { High, Medium, Low }

/**
 * Pet (dog and cat) toxicity as recorded by the ML team (DECISIONS D41). Shown
 * with how it was established and a "not veterinary advice" note.
 */
data class PetSafety(
    val toxicToDogs: Boolean,
    val toxicToCats: Boolean,
    val details: String?,
    val basis: PetSafetyBasis,
    val source: ContentSource?,
)

enum class PetSafetyBasis {
    /** Listed by the ASPCA. */
    AspcaVerified,

    /** Not in the ASPCA list; a university extension source lists a poison risk. */
    ExtensionListed,

    /** Inferred from the plant's genus; not confirmed for this species. */
    GenusInferred,

    Unknown,
}

/** Where information comes from. Shown with every care guide. */
data class ContentSource(
    val name: String,
    val url: String?,
    val license: String?,
)

/** All supported plants. */
data class PlantCatalog(val plants: List<Plant>) {
    fun byId(id: String): Plant? = plants.firstOrNull { it.id == id }
    fun byClassLabel(classLabel: String): Plant? = plants.firstOrNull { it.classLabel == classLabel }

    /** Every distinct source used for care values or pet safety, for credits. */
    val sources: List<ContentSource>
        get() = plants.flatMap { listOfNotNull(it.careInfo?.source, it.petSafety?.source) }
            .distinctBy { it.name }
}

/** Bundled plant data and care information. */
interface PlantRepository {
    /** The catalog, or a failure if the bundled data can't be read. */
    suspend fun catalog(): Result<PlantCatalog>
}

fun Plant.toSummary(): PlantSummary = PlantSummary(
    plantId = id,
    commonName = commonName,
    scientificName = scientificName,
    // The sources have no descriptions; none are invented.
    description = null,
)
