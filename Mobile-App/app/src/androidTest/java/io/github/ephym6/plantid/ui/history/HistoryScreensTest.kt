package io.github.ephym6.plantid.ui.history

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryStatus
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

/** Spec §6.9 acceptance for the History list and a saved result (stateless screens, fictional data). */
@RunWith(AndroidJUnit4::class)
class HistoryScreensTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun str(@StringRes id: Int): String = composeRule.activity.getString(id)

    private val demo = ModelInfo("Demo", null, isDemo = true)
    private val identified = HistoryRecord(
        "a", Instant.EPOCH, HistoryStatus.Identified, "c", "plant_a", "Test Plant", "Test species (fictional)", demo,
        photo = File("/definitely/missing.jpg"),
        confidence = 0.83f,
    )
    private val uncertain = HistoryRecord("b", Instant.EPOCH, HistoryStatus.Uncertain, null, null, null, null, demo, null)

    private var cleared = 0
    private var opened: String? = null
    private var deleted = 0
    private var careGuideFor: String? = null

    private fun showList(state: HistoryViewModel.UiState) {
        composeRule.setContent {
            PlantIDTheme {
                HistoryScreen(
                    state = state,
                    clearFailed = false,
                    onIdentifyPlant = {},
                    onOpenRecord = { opened = it },
                    onClearAll = { cleared++ },
                    onDismissClearError = {},
                    onRetry = {},
                )
            }
        }
    }

    private fun showDetail(record: HistoryRecord) {
        composeRule.setContent {
            PlantIDTheme {
                HistoryDetailScreen(
                    state = HistoryDetailViewModel.UiState.Found(record),
                    deleted = false,
                    deleteFailed = false,
                    onBack = {},
                    onViewCareGuide = { careGuideFor = it },
                    onDelete = { deleted++ },
                    onDismissDeleteError = {},
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun recordsShowNameOrUnidentifiedStatusAndDemoTag() {
        showList(HistoryViewModel.UiState.Records(listOf(identified, uncertain)))

        composeRule.onNodeWithText("Test Plant").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.history_unidentified)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.history_status_identified)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.history_status_uncertain)).assertIsDisplayed()
        assertEquals(2, composeRule.onAllNodes(hasText(str(R.string.history_demo_tag))).fetchSemanticsNodes().size)

        composeRule.onNodeWithText("Test Plant").performClick()
        assertEquals("a", opened)
    }

    @Test
    fun clearAllAsksForConfirmation() {
        showList(HistoryViewModel.UiState.Records(listOf(identified, uncertain)))

        composeRule.onNodeWithContentDescription(str(R.string.history_clear_all)).performClick()
        composeRule.onNodeWithText(str(R.string.history_clear_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.action_cancel)).performClick()
        assertEquals(0, cleared)

        composeRule.onNodeWithContentDescription(str(R.string.history_clear_all)).performClick()
        composeRule.onNodeWithText(str(R.string.action_clear_all)).performClick()
        assertEquals(1, cleared)
    }

    @Test
    fun emptyHistoryHasNoClearButton() {
        showList(HistoryViewModel.UiState.Empty)

        composeRule.onNodeWithText(str(R.string.history_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(str(R.string.history_clear_all)).assertDoesNotExist()
    }

    @Test
    fun savedIdentifiedResultOffersCareGuideAndConfirmedDelete() {
        showDetail(identified)

        composeRule.onNodeWithText(str(R.string.demo_banner_title)).assertIsDisplayed()
        composeRule.onNodeWithText("Test Plant").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.result_confidence, "83%"))
            .performScrollTo().assertIsDisplayed()
        // Missing photo file: placeholder, no crash.
        composeRule.onNodeWithText(str(R.string.action_view_care_guide)).performScrollTo().performClick()
        assertEquals("plant_a", careGuideFor)

        composeRule.onNodeWithText(str(R.string.action_delete_from_history)).performScrollTo().performClick()
        composeRule.onNodeWithText(str(R.string.history_delete_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.action_cancel)).performClick()
        assertEquals(0, deleted)

        composeRule.onNodeWithText(str(R.string.action_delete_from_history)).performScrollTo().performClick()
        composeRule.onNodeWithText(str(R.string.action_delete)).performClick()
        assertEquals(1, deleted)
    }

    @Test
    fun savedUncertainResultHasNoPlantNameOrCareGuide() {
        showDetail(uncertain)

        composeRule.onNodeWithText(str(R.string.result_uncertain_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.action_view_care_guide)).assertDoesNotExist()
    }
}
