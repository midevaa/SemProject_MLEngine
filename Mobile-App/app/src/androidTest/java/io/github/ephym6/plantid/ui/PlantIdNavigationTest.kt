package io.github.ephym6.plantid.ui

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Sprint 1 acceptance: every top-level destination opens, and Back is predictable. */
@RunWith(AndroidJUnit4::class)
class PlantIdNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun str(@StringRes id: Int): String = composeRule.activity.getString(id)

    /** The bottom-bar tab (selectable), not a screen title with the same text. */
    private fun tab(@StringRes labelRes: Int): SemanticsNodeInteraction =
        composeRule.onNode(hasText(str(labelRes)) and isSelectable())

    private fun assertOnHome() {
        composeRule.onNodeWithText(str(R.string.home_tagline)).assertIsDisplayed()
        tab(R.string.nav_home).assertIsSelected()
    }

    @Before
    fun setUp() {
        composeRule.setContent { PlantIDTheme { PlantIdApp() } }
    }

    @Test
    fun startsOnHome() {
        assertOnHome()
        tab(R.string.nav_history).assertIsNotSelected()
        tab(R.string.nav_about).assertIsNotSelected()
    }

    @Test
    fun historyTabOpensHistory() {
        tab(R.string.nav_history).performClick()
        composeRule.onNodeWithText(str(R.string.history_empty_title)).assertIsDisplayed()
        tab(R.string.nav_history).assertIsSelected()
    }

    @Test
    fun aboutTabOpensAbout() {
        tab(R.string.nav_about).performClick()
        composeRule.onNodeWithText(str(R.string.about_purpose_title)).assertIsDisplayed()
        tab(R.string.nav_about).assertIsSelected()
    }

    @Test
    fun homeTabReturnsHome() {
        tab(R.string.nav_about).performClick()
        tab(R.string.nav_home).performClick()
        assertOnHome()
    }

    @Test
    fun viewHistoryButtonOpensHistory() {
        composeRule.onNodeWithText(str(R.string.home_view_history)).performScrollTo().performClick()
        composeRule.onNodeWithText(str(R.string.history_empty_title)).assertIsDisplayed()
        tab(R.string.nav_history).assertIsSelected()
    }

    @Test
    fun backFromTabReturnsHome() {
        tab(R.string.nav_history).performClick()
        Espresso.pressBack()
        assertOnHome()
    }

    @Test
    fun backAfterSwitchingTabsReturnsHomeNotPreviousTab() {
        tab(R.string.nav_history).performClick()
        tab(R.string.nav_about).performClick()
        Espresso.pressBack()
        assertOnHome()
    }
}
