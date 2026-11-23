package io.github.ephym6.plantid.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Sizes
import io.github.ephym6.plantid.ui.theme.Spacing

/**
 * Indeterminate loading state with a short message. Never shows fake percentage
 * progress.
 */
@Composable
fun LoadingIndicator(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xl)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

/** Shown when a list or section has no content yet. Explains what will appear and, optionally, how to start. */
@Composable
fun EmptyState(
    @DrawableRes iconRes: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    StatePanel(
        iconRes = iconRes,
        iconContainer = MaterialTheme.colorScheme.primaryContainer,
        iconContent = MaterialTheme.colorScheme.onPrimaryContainer,
        title = title,
        message = message,
        modifier = modifier,
        actions = action,
    )
}

/**
 * Full-area error with a recovery path. Pass [onRetry] only when retrying can
 * actually help; otherwise provide a [secondaryAction] such as "Back to home".
 * Messages must be human-readable: no exception text, stack traces or file paths.
 */
@Composable
fun ErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = stringResource(R.string.action_try_again),
    secondaryAction: (@Composable () -> Unit)? = null,
) {
    StatePanel(
        iconRes = R.drawable.ic_error,
        iconContainer = MaterialTheme.colorScheme.errorContainer,
        iconContent = MaterialTheme.colorScheme.onErrorContainer,
        title = title,
        message = message,
        modifier = modifier,
        actions = if (onRetry == null && secondaryAction == null) {
            null
        } else {
            {
                if (onRetry != null) {
                    PrimaryButton(text = retryLabel, onClick = onRetry, modifier = Modifier.fillMaxWidth())
                }
                secondaryAction?.invoke()
            }
        },
    )
}

@Composable
private fun StatePanel(
    @DrawableRes iconRes: Int,
    iconContainer: Color,
    iconContent: Color,
    title: String,
    message: String,
    modifier: Modifier,
    actions: (@Composable () -> Unit)?,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = Sizes.statePanelMaxWidth)
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Surface(
                shape = CircleShape,
                color = iconContainer,
                contentColor = iconContent,
                modifier = Modifier.size(Sizes.iconLarge + Spacing.xxl),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Decorative: the title carries the meaning.
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.iconLarge),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actions != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    actions()
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun StateViewsPreview() {
    PlantIDTheme {
        Surface {
            Column {
                LoadingIndicator(message = "Loading message…")
                HorizontalDivider()
                EmptyState(
                    iconRes = R.drawable.ic_history,
                    title = "Nothing here yet",
                    message = "Explains what will appear here.",
                    action = { PrimaryButton(text = "Start", onClick = {}) },
                )
                HorizontalDivider()
                ErrorState(
                    title = "Something went wrong",
                    message = "Human-readable explanation.",
                    onRetry = {},
                    secondaryAction = { TextButton(onClick = {}) { Text("Back to home") } },
                )
            }
        }
    }
}
