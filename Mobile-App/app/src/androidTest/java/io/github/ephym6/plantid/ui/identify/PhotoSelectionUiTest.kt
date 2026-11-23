package io.github.ephym6.plantid.ui.identify

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.data.identification.mock.DemoScenario
import io.github.ephym6.plantid.ui.PlantIdApp
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * UI tests for the photo source sheet and the preview screen. The system photo
 * picker and camera are separate apps, so they are covered by the manual
 * checklist, not here.
 */
@RunWith(AndroidJUnit4::class)
class PhotoSelectionUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun str(@StringRes id: Int): String = composeRule.activity.getString(id)

    @Test
    fun identifyOnHomeOpensThePhotoSourceSheet() {
        composeRule.setContent { PlantIDTheme { PlantIdApp() } }

        composeRule.onNodeWithText(str(R.string.action_identify_plant)).performClick()

        composeRule.onNodeWithText(str(R.string.photo_source_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.photo_source_gallery)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.photo_source_camera)).assertIsDisplayed()
    }

    @Test
    fun identifyOnEmptyHistoryOpensThePhotoSourceSheet() {
        composeRule.setContent { PlantIDTheme { PlantIdApp() } }
        composeRule.onNode(hasText(str(R.string.nav_history)) and isSelectable())
            .performClick()

        composeRule.onNodeWithText(str(R.string.action_identify_plant)).performClick()

        composeRule.onNodeWithText(str(R.string.photo_source_title)).assertIsDisplayed()
    }

    @Test
    fun cameraOptionIsDisabledAndExplainedWithoutACamera() {
        var galleryTaps = 0
        composeRule.setContent {
            PlantIDTheme {
                PhotoSourceOptions(cameraAvailable = false, onTakePhoto = {}, onChooseFromPhotos = { galleryTaps++ })
            }
        }

        composeRule.onNodeWithText(str(R.string.photo_source_camera)).assertIsNotEnabled()
        composeRule.onNodeWithText(str(R.string.photo_source_camera_missing)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.photo_source_gallery)).assertIsEnabled().performClick()
        assertEquals(1, galleryTaps)
    }

    @Test
    fun previewWithoutAUsablePhotoOffersChooseAnotherAndBackButNotIdentify() {
        var chooseAnotherTaps = 0
        var backTaps = 0
        composeRule.setContent {
            PlantIDTheme {
                PhotoPreviewScreen(
                    photo = null,
                    onBack = { backTaps++ },
                    onChooseAnother = { chooseAnotherTaps++ },
                    onIdentify = {},
                )
            }
        }

        composeRule.onNodeWithText(str(R.string.preview_instruction)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.preview_identify)).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(str(R.string.preview_choose_another)).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(str(R.string.action_back)).performClick()

        assertEquals(1, chooseAnotherTaps)
        assertEquals(1, backTaps)
    }

    @Test
    fun previewWithInvalidPhotoIdExplainsThePhotoIsUnavailable() {
        composeRule.setContent {
            PlantIDTheme { PhotoPreviewScreen(photo = null, onBack = {}, onChooseAnother = {}, onIdentify = {}) }
        }

        composeRule.onNodeWithText(str(R.string.preview_image_error)).assertIsDisplayed()
    }

    @Test
    fun demoModeCardSelectsTheMockOutcome() {
        var selected: DemoScenario? = null
        composeRule.setContent {
            PlantIDTheme {
                PhotoPreviewScreen(
                    photo = null,
                    onBack = {},
                    onChooseAnother = {},
                    onIdentify = {},
                    demoScenario = DemoScenario.Identified,
                    onDemoScenarioSelected = { selected = it },
                )
            }
        }

        composeRule.onNodeWithText(str(R.string.demo_mode_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.demo_scenario_uncertain)).performScrollTo().performClick()
        assertEquals(DemoScenario.Uncertain, selected)
    }

    @Test
    fun demoModeCardIsHiddenWhenNoDemoIdentifierIsInUse() {
        composeRule.setContent {
            PlantIDTheme { PhotoPreviewScreen(photo = null, onBack = {}, onChooseAnother = {}, onIdentify = {}) }
        }

        composeRule.onNodeWithText(str(R.string.demo_mode_title)).assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun previewWithMissingFileExplainsThePhotoIsUnavailable() {
        // e.g. the system cleared PlantID's cache while the preview was in the back stack.
        val missing = File(composeRule.activity.cacheDir, "selected_photos/picked_missing.img")
        composeRule.setContent {
            PlantIDTheme { PhotoPreviewScreen(photo = missing, onBack = {}, onChooseAnother = {}, onIdentify = {}) }
        }

        composeRule.waitUntilExactlyOneExists(hasText(str(R.string.preview_image_error)), timeoutMillis = 5_000)
    }
}
