package io.github.ephym6.plantid.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Sizes
import io.github.ephym6.plantid.ui.theme.Spacing

/**
 * Tone of a [StatusMessage]. Every tone has its own icon and spoken label, so the
 * status is never communicated by colour alone.
 */
enum class StatusTone(@param:DrawableRes val iconRes: Int, @param:StringRes val labelRes: Int) {
    Info(R.drawable.ic_info, R.string.status_info),
    Success(R.drawable.ic_check_circle, R.string.status_success),
    Warning(R.drawable.ic_warning, R.string.status_warning),
    Error(R.drawable.ic_error, R.string.status_error),
}

/** Inline message banner: icon + title + optional supporting text. */
@Composable
fun StatusMessage(
    tone: StatusTone,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val (container, content) = tone.colors()
    Surface(
        // Merge so screen readers announce "Warning, <title>, <message>" as one item.
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                painter = painterResource(tone.iconRes),
                contentDescription = stringResource(tone.labelRes),
                modifier = Modifier.size(Sizes.iconMedium),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                if (message != null) {
                    Text(text = message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun StatusTone.colors(): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val extended = PlantIDTheme.extendedColors
    return when (this) {
        StatusTone.Info -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        StatusTone.Success -> scheme.primaryContainer to scheme.onPrimaryContainer
        StatusTone.Warning -> extended.warningContainer to extended.onWarningContainer
        StatusTone.Error -> scheme.errorContainer to scheme.onErrorContainer
    }
}

@PreviewLightDark
@Composable
private fun StatusMessagePreview() {
    PlantIDTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                StatusTone.entries.forEach { tone ->
                    StatusMessage(tone = tone, title = "${tone.name} title", message = "Supporting message text.")
                }
                StatusMessage(tone = StatusTone.Info, title = "Title only")
            }
        }
    }
}
