package io.github.ephym6.plantid.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Sizes
import io.github.ephym6.plantid.ui.theme.Spacing

/** Main call to action on a screen. Use at most one per screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = Sizes.buttonMinHeight),
        enabled = enabled,
        contentPadding = buttonPadding(iconRes),
    ) {
        ButtonContent(text, iconRes)
    }
}

/** Alternative or less important action. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Sizes.buttonMinHeight),
        enabled = enabled,
        contentPadding = buttonPadding(iconRes),
    ) {
        ButtonContent(text, iconRes)
    }
}

private fun buttonPadding(@DrawableRes iconRes: Int?): PaddingValues =
    if (iconRes != null) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding

@Composable
private fun RowScope.ButtonContent(text: String, @DrawableRes iconRes: Int?) {
    if (iconRes != null) {
        // Decorative: the text label already describes the action.
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    }
    // No maxLines: long labels wrap instead of clipping at large font sizes.
    Text(text = text, textAlign = TextAlign.Center)
}

@PreviewLightDark
@Composable
private fun ButtonsPreview() {
    PlantIDTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg)) {
                PrimaryButton("Primary action", onClick = {}, iconRes = R.drawable.ic_eco, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.size(Spacing.md))
                SecondaryButton("Secondary action", onClick = {}, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.size(Spacing.md))
                PrimaryButton("Disabled", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
