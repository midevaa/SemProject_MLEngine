package io.github.ephym6.plantid.ui.identify

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Sizes
import io.github.ephym6.plantid.ui.theme.Spacing

/** Bottom sheet asking where the plant photo should come from. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceSheet(
    cameraAvailable: Boolean,
    onTakePhoto: () -> Unit,
    onChooseFromPhotos: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        PhotoSourceOptions(
            cameraAvailable = cameraAvailable,
            onTakePhoto = onTakePhoto,
            onChooseFromPhotos = onChooseFromPhotos,
            modifier = Modifier.padding(
                start = Spacing.screenHorizontal,
                end = Spacing.screenHorizontal,
                bottom = Spacing.xl,
            ),
        )
    }
}

/** The sheet's content, separate so it can be previewed and tested on its own. */
@Composable
fun PhotoSourceOptions(
    cameraAvailable: Boolean,
    onTakePhoto: () -> Unit,
    onChooseFromPhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.photo_source_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(bottom = Spacing.xs)
                .semantics { heading() },
        )
        PhotoSourceOption(
            iconRes = R.drawable.ic_photo_camera,
            title = stringResource(R.string.photo_source_camera),
            detail = stringResource(
                if (cameraAvailable) R.string.photo_source_camera_detail else R.string.photo_source_camera_missing,
            ),
            enabled = cameraAvailable,
            onClick = onTakePhoto,
        )
        PhotoSourceOption(
            iconRes = R.drawable.ic_photo_library,
            title = stringResource(R.string.photo_source_gallery),
            detail = stringResource(R.string.photo_source_gallery_detail),
            enabled = true,
            onClick = onChooseFromPhotos,
        )
    }
}

@Composable
private fun PhotoSourceOption(
    @DrawableRes iconRes: Int,
    title: String,
    detail: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.buttonMinHeight + Spacing.lg)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(Sizes.iconMedium),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PhotoSourceOptionsPreview() {
    PlantIDTheme {
        Surface {
            PhotoSourceOptions(
                cameraAvailable = true,
                onTakePhoto = {},
                onChooseFromPhotos = {},
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PhotoSourceOptionsNoCameraPreview() {
    PlantIDTheme {
        Surface {
            PhotoSourceOptions(
                cameraAvailable = false,
                onTakePhoto = {},
                onChooseFromPhotos = {},
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}
