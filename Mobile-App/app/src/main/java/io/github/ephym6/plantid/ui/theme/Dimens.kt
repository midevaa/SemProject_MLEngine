package io.github.ephym6.plantid.ui.theme

import androidx.compose.ui.unit.dp

/** Spacing scale (4 dp grid). Use these instead of ad-hoc dp values. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Horizontal padding between screen content and the screen edge. */
    val screenHorizontal = lg
}

/** Fixed component sizes. */
object Sizes {
    /** Minimum height for primary actions; above the 48 dp accessibility minimum. */
    val buttonMinHeight = 52.dp
    val iconSmall = 18.dp
    val iconMedium = 24.dp
    val iconLarge = 48.dp
    val brandMark = 48.dp
    val statePanelMaxWidth = 420.dp
}
