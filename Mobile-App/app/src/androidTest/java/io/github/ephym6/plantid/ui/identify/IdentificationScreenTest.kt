package io.github.ephym6.plantid.ui.identify

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.domain.identification.ClassScore
import io.github.ephym6.plantid.domain.identification.FailureReason
import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.domain.identification.PlantSummary
import io.github.ephym6.plantid.ui.identify.IdentificationViewModel.UiState
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec §6.4–6.7 acceptance for each identification state, using fictional demo data. */
@RunWith(AndroidJUnit4::class)
class IdentificationScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val demo = ModelInfo(name = "Demo", version = null, isDemo = true)
    private val real = ModelInfo(name = "Real", version = "1", isDemo = false)

    private val complete = PlantSummary("plant_a", "Test Plant A", "Test species (fictional)", "Test description.")
    private val minimal = PlantSummary("plant_b", "Test Plant B", null, null)

    private var retries = 0
    private var careGuideFor: String? = null
    private var identifyAnother = 0
    private var backToHome = 0

    private fun str(@StringRes id: Int): String = composeRule.activity.getString(id)

    private var saves = 0

    private fun show(state: UiState, saveState: SaveState = SaveState.NotSaveable) {
        composeRule.setContent {
            PlantIDTheme {
                IdentificationScreen(
                    state = state,
                    photo = null,
                    saveState = saveState,
                    onSave = { saves++ },
                    onBack = {},
                    onRetry = { retries++ },
                    onIdentifyAnother = { identifyAnother++ },
                    onBackToHome = { backToHome++ },
                    onViewCareGuide = { careGuideFor = it },
                )
            }
        }
    }

    private fun assertNoDemoPlantShown() {
        listOf(complete, minimal).forEach { plant ->
            composeRule.onNodeWithText(plant.commonName!!).assertDoesNotExist()
        }
    }

    @Test
    fun identifyingShowsTheLoadingMessage() {
        show(UiState.Identifying)
        composeRule.onNodeWithText(str(R.string.identifying_message)).assertIsDisplayed()
    }

    @Test
    fun identifiedShowsPlantNameAndIsLabelledDemo() {
        show(UiState.Finished(IdentificationOutcome.Identified(demo, "class_a", complete)))

        composeRule.onNodeWithText(str(R.string.demo_banner_title)).assertIsDisplayed()
        composeRule.onNodeWithText(complete.commonName!!).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(complete.scientificName!!).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.result_identified_title)).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText(str(R.string.result_description_title)).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText(str(R.string.action_view_care_guide)).performScrollTo().performClick()
        composeRule.onNodeWithText(str(R.string.action_identify_another)).performScrollTo().performClick()
        composeRule.onNodeWithText(str(R.string.action_back_to_home)).performScrollTo().performClick()
        assertEquals("plant_a", careGuideFor)
        assertEquals(1, identifyAnother)
        assertEquals(1, backToHome)
    }

    @Test
    fun identifiedWithMissingDetailsStillWorks() {
        show(UiState.Finished(IdentificationOutcome.Identified(demo, "class_b", minimal)))

        composeRule.onNodeWithText(minimal.commonName!!).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.result_description_title)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.action_identify_another)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun uncertainNeverShowsASpeciesName() {
        show(UiState.Finished(IdentificationOutcome.Uncertain(demo)))

        composeRule.onNodeWithText(str(R.string.action_view_care_guide)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.result_uncertain_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.result_identified_title)).assertDoesNotExist()
        assertNoDemoPlantShown()

        composeRule.onNodeWithText(str(R.string.action_try_another_photo)).performScrollTo().performClick()
        assertEquals(1, identifyAnother)
    }

    @Test
    fun uncertainShowsGuessesOnlyAsUnconfirmedPossibleMatches() {
        show(UiState.Finished(IdentificationOutcome.Uncertain(demo, candidates = listOf(complete, minimal))))

        composeRule.onNodeWithText(str(R.string.result_uncertain_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.result_candidates_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Test Plant A (Test species (fictional))", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Test Plant B", substring = true).performScrollTo().assertIsDisplayed()
        // Never presented as a result: no success status, no care guide.
        composeRule.onNodeWithText(str(R.string.result_identified_title)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.action_view_care_guide)).assertDoesNotExist()
    }

    @Test
    fun identifiedResultShowsTheModelConfidence() {
        val scores = listOf(ClassScore(complete.plantId, 0.87f), ClassScore("other", 0.13f))
        show(UiState.Finished(IdentificationOutcome.Identified(real, complete.plantId, complete, scores)))

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.result_confidence, "87%"))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.result_confidence_note)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun resultWithoutScoresShowsNoConfidence() {
        show(UiState.Finished(IdentificationOutcome.Identified(demo, "class_a", complete)))

        composeRule.onNodeWithText(str(R.string.result_confidence_note)).assertDoesNotExist()
    }

    @Test
    fun possibleMatchesShowTheirPercentages() {
        val scores = listOf(ClassScore(complete.plantId, 0.41f), ClassScore(minimal.plantId, 0.22f))
        show(UiState.Finished(IdentificationOutcome.Uncertain(real, scores, listOf(complete, minimal))))

        composeRule.onNodeWithText("Test Plant A (Test species (fictional)) — 41%", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Test Plant B — 22%", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.result_confidence_note)).assertDoesNotExist()
    }

    @Test
    fun uncertainWithoutGuessesHasNoPossibleMatchesCard() {
        show(UiState.Finished(IdentificationOutcome.Uncertain(demo)))

        composeRule.onNodeWithText(str(R.string.result_candidates_title)).assertDoesNotExist()
    }

    @Test
    fun unsupportedShowsNoPlant() {
        show(UiState.Finished(IdentificationOutcome.Unsupported(demo)))

        composeRule.onNodeWithText(str(R.string.result_unsupported_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.result_identified_title)).assertDoesNotExist()
        assertNoDemoPlantShown()
    }

    @Test
    fun processingFailureCanBeRetried() {
        show(UiState.Finished(IdentificationOutcome.Failed(demo, FailureReason.ProcessingFailed)))

        composeRule.onNodeWithText(str(R.string.result_failed_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.action_try_again)).performScrollTo().performClick()
        composeRule.onNodeWithText(str(R.string.action_back_to_home)).performScrollTo().performClick()

        assertEquals(1, retries)
        assertEquals(1, backToHome)
    }

    @Test
    fun missingPhotoFailureOffersAnotherPhotoInsteadOfRetry() {
        show(UiState.Finished(IdentificationOutcome.Failed(demo, FailureReason.PhotoUnavailable)))

        composeRule.onNodeWithText(str(R.string.result_failed_photo)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.action_try_again)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.action_try_another_photo)).performScrollTo().performClick()

        assertEquals(0, retries)
        assertEquals(1, identifyAnother)
    }

    @Test
    fun saveButtonSavesTheResult() {
        val result = UiState.Finished(IdentificationOutcome.Unsupported(demo))

        show(result, SaveState.NotSaved)
        composeRule.onNodeWithText(str(R.string.action_save_to_history)).performScrollTo().performClick()
        assertEquals(1, saves)
    }

    @Test
    fun savedResultShowsConfirmationInsteadOfTheButton() {
        show(UiState.Finished(IdentificationOutcome.Unsupported(demo)), SaveState.Saved)

        composeRule.onNodeWithText(str(R.string.history_saved_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.action_save_to_history)).assertDoesNotExist()
    }

    @Test
    fun failedSaveCanBeRetried() {
        show(UiState.Finished(IdentificationOutcome.Unsupported(demo)), SaveState.Failed)

        composeRule.onNodeWithText(str(R.string.history_save_failed_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.action_try_again)).performScrollTo().performClick()
        assertEquals(1, saves)
    }

    @Test
    fun failedIdentificationCannotBeSaved() {
        show(UiState.Finished(IdentificationOutcome.Failed(demo, FailureReason.ProcessingFailed)))

        composeRule.onNodeWithText(str(R.string.action_save_to_history)).assertDoesNotExist()
    }

    @Test
    fun resultsFromARealModelHaveNoDemoBanner() {
        show(UiState.Finished(IdentificationOutcome.Unsupported(real)))

        composeRule.onNodeWithText(str(R.string.demo_banner_title)).assertDoesNotExist()
    }
}
