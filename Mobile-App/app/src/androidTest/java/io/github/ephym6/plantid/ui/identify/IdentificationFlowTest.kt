package io.github.ephym6.plantid.ui.identify

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ephym6.plantid.PlantIdApplication
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.data.identification.mock.DemoScenario
import io.github.ephym6.plantid.data.identification.mock.MockIdentificationService
import io.github.ephym6.plantid.data.image.PhotoImportResult
import io.github.ephym6.plantid.ui.PlantIdApp
import io.github.ephym6.plantid.ui.navigation.navigateToPhotoPreview
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End to end through the real app shell and mock service: preview → choose a
 * demo outcome → Identify plant → Analyzing → result. Starts from a photo
 * imported through the real PhotoStore, because the system picker can't be
 * driven from a test.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class IdentificationFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val container get() = (composeRule.activity.application as PlantIdApplication).container
    private lateinit var navController: NavHostController
    private lateinit var source: File
    private lateinit var demoPlantName: String

    private fun str(@StringRes id: Int): String = composeRule.activity.getString(id)

    @Before
    fun setUp() {
        // Deterministic outcomes: these tests drive the demo identifier, not the real model.
        container.useDemoIdentifier(true)
        // The container is app-wide, so reset the demo scenario between tests.
        container.demoScenarios!!.select(DemoScenario.Identified)
        demoPlantName = runBlocking {
            container.plantRepository.catalog().getOrThrow()
                .byClassLabel(MockIdentificationService.DEMO_CLASS_LABEL)!!.commonName!!
        }
        source = File(composeRule.activity.cacheDir, "flow_test_source.png")
        source.outputStream().use {
            Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val photoId = runBlocking {
            (container.photoStore.importFromPicker(Uri.fromFile(source).toString()) as PhotoImportResult.Success).photoId
        }
        composeRule.setContent {
            PlantIDTheme {
                navController = rememberNavController()
                PlantIdApp(navController = navController)
            }
        }
        composeRule.runOnIdle { navController.navigateToPhotoPreview(photoId) }
        composeRule.waitUntilExactlyOneExists(hasText(str(R.string.preview_identify)) and isEnabled(), 5_000)
    }

    @After
    fun tearDown() {
        container.demoScenarios!!.select(DemoScenario.Identified)
        source.delete()
        container.useDemoIdentifier(false)
    }

    private fun identifyWith(scenario: DemoScenario) {
        composeRule.onNodeWithText(str(scenario.labelRes)).performScrollTo().performClick()
        composeRule.onNodeWithText(str(R.string.preview_identify)).performScrollTo().performClick()
        composeRule.onNodeWithText(str(R.string.identifying_message)).assertIsDisplayed()
    }

    @Test
    fun identifiedDemoOutcomeReachesTheResult() {
        identifyWith(DemoScenario.Identified)

        composeRule.waitUntilExactlyOneExists(hasText(demoPlantName), 5_000)
        composeRule.onNodeWithText(str(R.string.demo_banner_title)).assertIsDisplayed()
    }

    @Test
    fun uncertainDemoOutcomeShowsNoPlantAndOffersAnotherPhoto() {
        identifyWith(DemoScenario.Uncertain)

        composeRule.waitUntilExactlyOneExists(hasText(str(R.string.result_uncertain_title)), 5_000)
        composeRule.onNodeWithText(demoPlantName).assertDoesNotExist()

        composeRule.onNodeWithText(str(R.string.action_try_another_photo)).performScrollTo().performClick()
        composeRule.onNodeWithText(str(R.string.photo_source_title)).assertIsDisplayed()
    }

    @Test
    fun failureCanBeRetriedIntoASuccess() {
        identifyWith(DemoScenario.Failure)
        composeRule.waitUntilExactlyOneExists(hasText(str(R.string.result_failed_title)), 5_000)

        container.demoScenarios!!.select(DemoScenario.Identified)
        composeRule.onNodeWithText(str(R.string.action_try_again)).performScrollTo().performClick()

        composeRule.waitUntilExactlyOneExists(hasText(demoPlantName), 5_000)
    }

    @Test
    fun identifiedResultOpensTheCareGuideAndBackReturnsToTheResult() {
        identifyWith(DemoScenario.Identified)
        composeRule.waitUntilExactlyOneExists(hasText(demoPlantName), 5_000)

        composeRule.onNodeWithText(str(R.string.action_view_care_guide)).performScrollTo().performClick()
        composeRule.waitUntilExactlyOneExists(hasText(str(R.string.care_watering_title)), 5_000)
        composeRule.onNodeWithText(demoPlantName).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.care_source_title)).performScrollTo().assertIsDisplayed()

        Espresso.pressBack()
        composeRule.onNodeWithText(str(R.string.action_view_care_guide)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun doubleTappingBackOnlyLeavesOneScreen() {
        identifyWith(DemoScenario.Identified)
        composeRule.waitUntilExactlyOneExists(hasText(demoPlantName), 5_000)
        composeRule.onNodeWithText(str(R.string.action_view_care_guide)).performScrollTo().performClick()
        composeRule.waitUntilExactlyOneExists(hasText(str(R.string.care_watering_title)), 5_000)

        // Pause the clock so both taps land before the exit animation runs.
        composeRule.mainClock.autoAdvance = false
        val back = composeRule.onNodeWithContentDescription(str(R.string.action_back))
        back.performClick()
        back.performClick()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        // Still on the result, not popped further to the preview.
        composeRule.onNodeWithText(str(R.string.action_view_care_guide)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.preview_title)).assertDoesNotExist()
    }

    @Test
    fun plantWithoutACareRecordShowsNoCareGuide() {
        identifyWith(DemoScenario.MissingDetails)
        composeRule.waitUntilExactlyOneExists(hasText(str(R.string.action_view_care_guide)), 5_000)

        composeRule.onNodeWithText(str(R.string.action_view_care_guide)).performScrollTo().performClick()

        composeRule.waitUntilExactlyOneExists(hasText(str(R.string.care_none_title)), 5_000)
    }

    @Test
    fun backToHomeLeavesTheFlow() {
        identifyWith(DemoScenario.Unsupported)
        composeRule.waitUntilExactlyOneExists(hasText(str(R.string.result_unsupported_title)), 5_000)

        composeRule.onNodeWithText(str(R.string.action_back_to_home)).performScrollTo().performClick()

        composeRule.onNodeWithText(str(R.string.home_tagline)).assertIsDisplayed()
    }
}
