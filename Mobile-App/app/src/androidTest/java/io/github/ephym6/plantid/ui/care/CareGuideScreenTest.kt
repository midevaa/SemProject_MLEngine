package io.github.ephym6.plantid.ui.care

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ephym6.plantid.PlantIdApplication
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.domain.plant.CareConfidence
import io.github.ephym6.plantid.domain.plant.CareData
import io.github.ephym6.plantid.domain.plant.CareInfo
import io.github.ephym6.plantid.domain.plant.CareRecommendations
import io.github.ephym6.plantid.domain.plant.ContentSource
import io.github.ephym6.plantid.domain.plant.PetSafety
import io.github.ephym6.plantid.domain.plant.PetSafetyBasis
import io.github.ephym6.plantid.domain.plant.Plant
import io.github.ephym6.plantid.ui.about.AboutScreen
import io.github.ephym6.plantid.ui.care.CareGuideViewModel.UiState
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec §6.8 acceptance for the care guide (incl. pet safety, D41), plus About's plant list and credits. */
@RunWith(AndroidJUnit4::class)
class CareGuideScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val source = ContentSource("Test source", "https://example.org/data", "CC BY-NC 4.0")
    private val fullCare = CareData(
        lightLevel = "Bright Indirect",
        lightLux = 500..3000,
        temperatureCelsius = 15..30,
        humidityPercent = 50,
        wateringIntervalDays = 7,
    )
    private var retries = 0

    private fun str(@StringRes id: Int): String = composeRule.activity.getString(id)

    private fun show(plant: Plant) = show(UiState.Ready(plant, CareRecommendations.tipsFor(plant.care)))

    private fun show(state: UiState) {
        composeRule.setContent {
            PlantIDTheme { CareGuideScreen(state = state, onBack = {}, onRetry = { retries++ }) }
        }
    }

    private fun plant(
        care: CareData? = fullCare,
        info: CareInfo? = CareInfo(source, CareConfidence.High, note = null, sourceSaid = null),
        pet: PetSafety? = null,
    ) = Plant("p", "p", "Test Plant", "Test species (fictional)", "Testaceae", care, info, pet)

    private fun scrollTo(text: String, substring: Boolean = false) =
        composeRule.onNodeWithText(text, substring = substring).performScrollTo().assertIsDisplayed()

    @Test
    fun completeCareGuideShowsEveryTipTheDisclaimerAndTheSource() {
        show(plant())

        composeRule.onNodeWithText("Test Plant").assertIsDisplayed()
        scrollTo(str(R.string.care_general_title))
        listOf(R.string.care_light_title, R.string.care_watering_title, R.string.care_temperature_title, R.string.care_humidity_title)
            .forEach { scrollTo(str(it)) }
        scrollTo("Bright Indirect")
        scrollTo(composeRule.activity.resources.getQuantityString(R.plurals.care_watering_interval, 7, 7))
        scrollTo(composeRule.activity.getString(R.string.care_temperature_range, 15, 30))
        scrollTo(composeRule.activity.getString(R.string.care_source_with_license, source.name, source.license))
        // High-confidence values: no "approximate" warning.
        composeRule.onNodeWithText(str(R.string.care_confidence_medium_title)).assertDoesNotExist()
    }

    @Test
    fun convertedValuesAreFlaggedAndShowTheSourcesOwnWording() {
        show(plant(info = CareInfo(source, CareConfidence.Medium, note = "Genus-level note.", sourceSaid = "Full sun; moist soil.")))

        scrollTo(str(R.string.care_confidence_medium_title))
        scrollTo(str(R.string.care_note_title))
        scrollTo("Genus-level note.")
        scrollTo(str(R.string.care_source_said_title))
        scrollTo("Full sun; moist soil.")
    }

    @Test
    fun toxicPlantShowsPetSafetyWithBasisSourceAndDisclaimer() {
        show(
            plant(
                pet = PetSafety(
                    toxicToDogs = true,
                    toxicToCats = true,
                    details = "Test symptoms.",
                    basis = PetSafetyBasis.ExtensionListed,
                    source = ContentSource("Test extension", null, null),
                ),
            ),
        )

        scrollTo(str(R.string.care_pet_summary_both), substring = true)
        scrollTo("Test symptoms.", substring = true)
        scrollTo(str(R.string.care_pet_basis_extension), substring = true)
        scrollTo(composeRule.activity.getString(R.string.care_pet_source, "Test extension"), substring = true)
        scrollTo(str(R.string.care_pet_disclaimer), substring = true)
    }

    @Test
    fun nonToxicAndInferredPetSafetyAreWordedHonestly() {
        show(plant(pet = PetSafety(false, false, null, PetSafetyBasis.GenusInferred, null)))

        scrollTo(str(R.string.care_pet_summary_none), substring = true)
        scrollTo(str(R.string.care_pet_basis_genus), substring = true)
        scrollTo(str(R.string.care_pet_disclaimer), substring = true)
    }

    @Test
    fun missingCareValuesAreLeftOutWithoutBreakingTheScreen() {
        show(plant(care = CareData(wateringIntervalDays = 14), info = null))

        scrollTo(str(R.string.care_watering_title))
        composeRule.onNodeWithText(str(R.string.care_light_title)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.care_temperature_title)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.care_pet_title), substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.care_source_title)).assertDoesNotExist()
    }

    @Test
    fun noCareRecordShowsAnHonestEmptyState() {
        show(UiState.NoCareGuide)

        composeRule.onNodeWithText(str(R.string.care_none_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.care_watering_title)).assertDoesNotExist()
    }

    @Test
    fun unreadableDataCanBeRetried() {
        show(UiState.Unavailable)

        composeRule.onNodeWithText(str(R.string.action_try_again)).performClick()
        assertEquals(1, retries)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aboutListsTheSupportedPlantsAndCreditsTheSources() {
        val repository = (composeRule.activity.application as PlantIdApplication).container.plantRepository
        val catalog = runBlocking { repository.catalog() }
        composeRule.setContent { PlantIDTheme { AboutScreen(versionName = "test", catalog = catalog) } }

        val plants = catalog.getOrThrow().plants
        scrollTo(composeRule.activity.resources.getQuantityString(R.plurals.about_supported_intro, plants.size, plants.size))
        plants.forEach { plant ->
            composeRule.waitUntilExactlyOneExists(hasText("${plant.commonName} (${plant.scientificName})", substring = true))
        }
        scrollTo(str(R.string.about_sources_title))
        catalog.getOrThrow().sources.forEach { scrollTo(it.name, substring = true) }
    }
}
