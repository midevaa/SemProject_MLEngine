package io.github.ephym6.plantid.testing

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue

/**
 * Basic accessibility checks on what's currently shown:
 * - every tappable element has something for a screen reader to announce
 *   (its own or merged text, or a content description);
 * - every tappable element is at least [minTouchTarget] in both directions
 *   (48 dp per Android accessibility guidance).
 */
fun ComposeTestRule.assertTappablesAreAccessible(minTouchTarget: Dp = 48.dp) {
    waitForIdle()
    val minPx = with(density) { minTouchTarget.toPx() } - 0.5f // allow rounding
    val problems = onAllNodes(hasClickAction()).fetchSemanticsNodes().mapNotNull { node ->
        val label = node.spokenLabel()
        // Layout size is unclipped (buttons below the fold still count) and includes Material's
        // minimum touch-target space; touch bounds add any extra touch expansion.
        val width = maxOf(node.layoutInfo.width.toFloat(), node.touchBoundsInRoot.width)
        val height = maxOf(node.layoutInfo.height.toFloat(), node.touchBoundsInRoot.height)
        when {
            label.isBlank() -> "unlabeled tappable at ${node.boundsInRoot}"
            width < minPx || height < minPx ->
                "'$label' touch target ${toDp(width)} x ${toDp(height)} is below $minTouchTarget"
            else -> null
        }
    }
    assertTrue("Accessibility problems:\n" + problems.joinToString("\n"), problems.isEmpty())
}

private fun ComposeTestRule.toDp(px: Float): Dp = with(density) { px.toDp() }

private fun SemanticsNode.spokenLabel(): String {
    val description = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
    val text = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
    return (description + text).joinToString(" ").trim()
}
