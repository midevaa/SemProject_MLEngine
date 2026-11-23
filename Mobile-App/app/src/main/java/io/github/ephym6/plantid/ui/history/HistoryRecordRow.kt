package io.github.ephym6.plantid.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryStatus
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.ui.components.PlantIdCard
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Sizes
import io.github.ephym6.plantid.ui.theme.Spacing
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One saved identification in a list (History, Home's recent section). The whole row opens it. */
@Composable
fun HistoryRecordRow(
    record: HistoryRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlantIdCard(modifier = modifier, onClick = onClick) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HistoryThumbnail(record.photo)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(text = record.displayName(), style = MaterialTheme.typography.titleMedium)
                HistoryStatusLabel(record.status)
                Text(
                    text = formatIdentifiedAt(record.identifiedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (record.model.isDemo) DemoTag()
        }
    }
}

/** Name for a record: common name, else scientific name, else "Unidentified plant" (spec §6.9). */
@Composable
fun HistoryRecord.displayName(): String =
    if (status == HistoryStatus.Identified) {
        commonName ?: scientificName ?: stringResource(R.string.result_name_fallback)
    } else {
        stringResource(R.string.history_unidentified)
    }

/** Status with its own icon and text, so it is never shown by colour alone. */
@Composable
fun HistoryStatusLabel(status: HistoryStatus, modifier: Modifier = Modifier) {
    val (iconRes, textRes, tint) = when (status) {
        HistoryStatus.Identified ->
            Triple(R.drawable.ic_check_circle, R.string.history_status_identified, MaterialTheme.colorScheme.primary)
        HistoryStatus.Uncertain ->
            Triple(R.drawable.ic_warning, R.string.history_status_uncertain, PlantIDTheme.extendedColors.warning)
        HistoryStatus.Unsupported ->
            Triple(R.drawable.ic_info, R.string.history_status_unsupported, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(Sizes.iconSmall))
        Text(text = stringResource(textRes), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DemoTag() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = PlantIDTheme.extendedColors.warningContainer,
        contentColor = PlantIDTheme.extendedColors.onWarningContainer,
    ) {
        Text(
            text = stringResource(R.string.history_demo_tag),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        )
    }
}

/** Small cropped thumbnail; a leaf placeholder if there's no photo or the file is missing. */
@Composable
private fun HistoryThumbnail(photo: File?) {
    var failed by remember(photo) { mutableStateOf(photo == null) }
    Box(
        modifier = Modifier
            .size(THUMBNAIL_SIZE)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null && !failed) {
            AsyncImage(
                model = photo,
                contentDescription = null, // the row's text describes the record
                contentScale = ContentScale.Crop,
                onState = { if (it is AsyncImagePainter.State.Error) failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (failed) {
            Icon(
                painter = painterResource(R.drawable.ic_eco),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.iconMedium),
            )
        }
    }
}

private val THUMBNAIL_SIZE = 64.dp

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

/** Date and time in the user's locale and time zone. */
fun formatIdentifiedAt(instant: Instant): String =
    dateTimeFormatter.withZone(ZoneId.systemDefault()).format(instant)

@PreviewLightDark
@Composable
private fun HistoryRecordRowPreview() {
    val model = ModelInfo("Preview", null, isDemo = true)
    PlantIDTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                HistoryRecordRow(
                    HistoryRecord("1", Instant.EPOCH, HistoryStatus.Identified, "c", "p", "Preview Plant", "Preview species", model, null),
                    onClick = {},
                )
                HistoryRecordRow(
                    HistoryRecord("2", Instant.EPOCH, HistoryStatus.Uncertain, null, null, null, null, model, null),
                    onClick = {},
                )
            }
        }
    }
}
