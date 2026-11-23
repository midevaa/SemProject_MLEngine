package io.github.ephym6.plantid.ui

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryStatus
import io.github.ephym6.plantid.domain.identification.ClassScore
import io.github.ephym6.plantid.domain.identification.FailureReason
import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.domain.identification.PlantSummary
import io.github.ephym6.plantid.domain.plant.CareConfidence
import io.github.ephym6.plantid.domain.plant.CareData
import io.github.ephym6.plantid.domain.plant.CareInfo
import io.github.ephym6.plantid.domain.plant.CareRecommendations
import io.github.ephym6.plantid.domain.plant.ContentSource
import io.github.ephym6.plantid.domain.plant.PetSafety
import io.github.ephym6.plantid.domain.plant.PetSafetyBasis
import io.github.ephym6.plantid.domain.plant.Plant
import io.github.ephym6.plantid.domain.plant.PlantCatalog
import io.github.ephym6.plantid.data.identification.mock.DemoScenario
import io.github.ephym6.plantid.testing.assertTappablesAreAccessible
import io.github.ephym6.plantid.ui.about.AboutScreen
import io.github.ephym6.plantid.ui.care.CareGuideScreen
import io.github.ephym6.plantid.ui.care.CareGuideViewModel
import io.github.ephym6.plantid.ui.history.HistoryDetailScreen
import io.github.ephym6.plantid.ui.history.HistoryDetailViewModel
import io.github.ephym6.plantid.ui.history.HistoryScreen
import io.github.ephym6.plantid.ui.history.HistoryViewModel
import io.github.ephym6.plantid.ui.home.HomeScreen
import io.github.ephym6.plantid.ui.identify.IdentificationScreen
import io.github.ephym6.plantid.ui.identify.IdentificationViewModel
import io.github.ephym6.plantid.ui.identify.PhotoPreviewScreen
import io.github.ephym6.plantid.ui.identify.PhotoSourceOptions
import io.github.ephym6.plantid.ui.identify.SaveState
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Sprint 6: every main screen on a small phone (320 x 568 dp) with the system
 * font at 200 %. Primary actions must still be reachable by scrolling (nothing
 * pushed off-screen or clipped away), and tappables must be labelled and at
 * least 48 dp. Fictional data only.
 */
@RunWith(AndroidJUnit4::class)
class LayoutStressTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun str(@StringRes id: Int): String = composeRule.activity.getString(id)

    private val demo = ModelInfo("Demo", null, isDemo = true)
    private val plant = PlantSummary("plant_a", "Test Plant With A Fairly Long Common Name", "Test species (fictional)", null)
    private val record = HistoryRecord(
        "a", Instant.EPOCH, HistoryStatus.Identified, "c", "plant_a", plant.commonName, plant.scientificName, demo, null,
    )
    private val care = CareData(
        lightLevel = "Bright Indirect to Direct Sun",
        lightLux = 500..3000,
        temperatureCelsius = 15..30,
        humidityPercent = 50,
        wateringIntervalDays = 7,
    )

    /** Renders [content] small and with large text, then checks [reachable] texts and accessibility. */
    private fun stress(@StringRes vararg reachable: Int, content: @Composable () -> Unit) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 568.dp)) then
                    DeviceConfigurationOverride.FontScale(2f),
            ) {
                PlantIDTheme { Surface { content() } }
            }
        }
        reachable.forEach { composeRule.onNodeWithText(str(it)).performScrollTo().assertIsDisplayed() }
        composeRule.assertTappablesAreAccessible()
    }

    @Test
    fun home() = stress(R.string.action_identify_plant, R.string.action_view_all, R.string.home_view_history) {
        HomeScreen(listOf(record), isDemo = true, onIdentifyPlant = {}, onOpenHistory = {}, onOpenRecord = {})
    }

    @Test
    fun photoSourceOptions() = stress(R.string.photo_source_camera, R.string.photo_source_gallery) {
        PhotoSourceOptions(cameraAvailable = true, onTakePhoto = {}, onChooseFromPhotos = {})
    }

    @Test
    fun photoPreviewWithDemoPicker() = stress(
        R.string.demo_scenario_slow,
        R.string.preview_identify,
        R.string.preview_choose_another,
    ) {
        PhotoPreviewScreen(
            photo = null,
            onBack = {},
            onChooseAnother = {},
            onIdentify = {},
            demoScenario = DemoScenario.Identified,
        )
    }

    @Test
    fun identifiedResult() = stress(
        R.string.result_confidence_note,
        R.string.action_view_care_guide,
        R.string.action_save_to_history,
        R.string.action_identify_another,
        R.string.action_back_to_home,
    ) {
        IdentificationScreen(
            state = IdentificationViewModel.UiState.Finished(
                IdentificationOutcome.Identified(demo, "c", plant, scores = listOf(ClassScore("c", 0.87f))),
            ),
            photo = null,
            saveState = SaveState.NotSaved,
            onSave = {},
            onBack = {},
            onRetry = {},
            onIdentifyAnother = {},
            onBackToHome = {},
            onViewCareGuide = {},
        )
    }

    @Test
    fun uncertainResult() = stress(
        R.string.result_uncertain_title,
        R.string.result_candidates_title,
        R.string.action_try_another_photo,
    ) {
        IdentificationScreen(
            state = IdentificationViewModel.UiState.Finished(
                IdentificationOutcome.Uncertain(
                    demo,
                    scores = listOf(ClassScore("plant_a", 0.41f), ClassScore("b", 0.22f), ClassScore("c", 0.09f)),
                    candidates = listOf(plant, plant.copy(plantId = "b"), plant.copy(plantId = "c")),
                ),
            ),
            photo = null,
            saveState = SaveState.NotSaved,
            onSave = {},
            onBack = {},
            onRetry = {},
            onIdentifyAnother = {},
            onBackToHome = {},
            onViewCareGuide = {},
        )
    }

    @Test
    fun failedResult() = stress(R.string.action_try_again, R.string.action_back_to_home) {
        IdentificationScreen(
            state = IdentificationViewModel.UiState.Finished(IdentificationOutcome.Failed(demo, FailureReason.TimedOut)),
            photo = null,
            saveState = SaveState.NotSaveable,
            onSave = {},
            onBack = {},
            onRetry = {},
            onIdentifyAnother = {},
            onBackToHome = {},
            onViewCareGuide = {},
        )
    }

    @Test
    fun careGuide() = stress(R.string.care_humidity_title, R.string.care_source_said_title, R.string.care_source_title) {
        CareGuideScreen(
            state = CareGuideViewModel.UiState.Ready(
                Plant(
                    "p", "P", plant.commonName, plant.scientificName, "Testaceae", care,
                    careInfo = CareInfo(
                        source = ContentSource("Test source with a reasonably long name", "https://example.org/a/long/path", "CC BY-NC 4.0"),
                        confidence = CareConfidence.Medium,
                        note = "A fairly long note from the team explaining which species these values come from.",
                        sourceSaid = "Full sun or partial shade; moist, well-drained soil; plant after the last frost.",
                    ),
                    petSafety = PetSafety(true, true, "Long symptom text that wraps across several lines.", PetSafetyBasis.ExtensionListed, ContentSource("Test extension", null, null)),
                ),
                CareRecommendations.tipsFor(care),
            ),
            onBack = {},
            onRetry = {},
        )
    }

    @Test
    fun historyList() = stress(R.string.history_status_identified) {
        HistoryScreen(HistoryViewModel.UiState.Records(listOf(record)), false, {}, {}, {}, {}, {})
    }

    @Test
    fun emptyHistory() = stress(R.string.action_identify_plant) {
        HistoryScreen(HistoryViewModel.UiState.Empty, false, {}, {}, {}, {}, {})
    }

    @Test
    fun savedResult() = stress(R.string.action_view_care_guide, R.string.action_delete_from_history) {
        HistoryDetailScreen(
            state = HistoryDetailViewModel.UiState.Found(record),
            deleted = false,
            deleteFailed = false,
            onBack = {},
            onViewCareGuide = {},
            onDelete = {},
            onDismissDeleteError = {},
            onRetry = {},
        )
    }

    @Test
    fun about() = stress(R.string.about_privacy_title, R.string.about_sources_title) {
        AboutScreen(
            versionName = "1.0",
            catalog = Result.success(
                PlantCatalog(
                    listOf(
                        Plant(
                            "p", "P", plant.commonName, plant.scientificName, null, null,
                            careInfo = CareInfo(ContentSource("Test source", null, "CC BY-NC 4.0"), CareConfidence.High, null, null),
                        ),
                    ),
                ),
            ),
        )
    }
}
