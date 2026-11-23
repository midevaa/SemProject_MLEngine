package io.github.ephym6.plantid.ui.history

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
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
 * Save → Home recent → open → delete, through the real app and database.
 *
 * NOTE: this uses the installed debug app's real history and clears it before
 * and after each test. Don't run device tests on a phone whose debug-build
 * history you want to keep.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class HistoryFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val container get() = (composeRule.activity.application as PlantIdApplication).container
    private lateinit var navController: NavHostController
    private lateinit var source: File
    private lateinit var plantName: String

    private fun str(@StringRes id: Int): String = composeRule.activity.getString(id)

    @Before
    fun setUp() {
        // Deterministic outcomes: these tests drive the demo identifier, not the real model.
        container.useDemoIdentifier(true)
        runBlocking { container.historyRepository.clearAll() }
        container.demoScenarios!!.select(DemoScenario.Identified)
        plantName = runBlocking {
            container.plantRepository.catalog().getOrThrow()
                .byClassLabel(MockIdentificationService.DEMO_CLASS_LABEL)!!.commonName!!
        }
        source = File(composeRule.activity.cacheDir, "history_flow_source.png")
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
        composeRule.onNodeWithText(str(R.string.preview_identify)).performScrollTo().performClick()
        composeRule.waitUntilExactlyOneExists(hasText(plantName), 5_000)
    }

    @After
    fun tearDown() {
        runBlocking { container.historyRepository.clearAll() }
        container.demoScenarios!!.select(DemoScenario.Identified)
        source.delete()
        container.useDemoIdentifier(false)
    }

    @Test
    fun savedResultAppearsOnHomeAndInHistoryAndCanBeDeleted() {
        composeRule.onNodeWithText(str(R.string.action_save_to_history)).performScrollTo().performClick()
        composeRule.waitUntilExactlyOneExists(hasText(str(R.string.history_saved_title)), 5_000)
        composeRule.onNodeWithText(str(R.string.action_save_to_history)).assertDoesNotExist()

        composeRule.onNodeWithText(str(R.string.action_back_to_home)).performScrollTo().performClick()
        composeRule.waitUntilExactlyOneExists(hasText(str(R.string.home_recent_title)), 5_000)

        composeRule.onNodeWithText(plantName).performScrollTo().performClick()
        composeRule.waitUntilExactlyOneExists(hasText(str(R.string.history_detail_title)), 5_000)

        composeRule.onNodeWithText(str(R.string.action_delete_from_history)).performScrollTo().performClick()
        composeRule.onNodeWithText(str(R.string.action_delete)).performClick()

        // Back on Home, and the section is gone with the only record.
        composeRule.waitUntilDoesNotExist(hasText(str(R.string.home_recent_title)), 5_000)
        composeRule.onNodeWithText(str(R.string.home_tagline)).assertIsDisplayed()
    }

    @Test
    fun unsavedResultsDontAppearInHistory() {
        composeRule.onNodeWithText(str(R.string.action_back_to_home)).performScrollTo().performClick()

        composeRule.onNodeWithText(str(R.string.home_tagline)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.home_recent_title)).assertDoesNotExist()
    }
}
