package io.github.ephym6.plantid.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryStatus
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.ui.history.HistoryRecordRow
import io.github.ephym6.plantid.ui.components.BrandHeader
import io.github.ephym6.plantid.ui.components.PrimaryButton
import io.github.ephym6.plantid.ui.components.SecondaryButton
import io.github.ephym6.plantid.ui.components.StatusMessage
import io.github.ephym6.plantid.ui.components.StatusTone
import io.github.ephym6.plantid.ui.components.TextCard
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Spacing
import java.time.Instant

/**
 * Home: introduction, the main "Identify a plant" action and, if any results
 * are saved, the most recent ones. With no history (or while it loads, or if it
 * can't be read) the section is simply not shown, never an empty container.
 *
 * @param recent null while loading or unavailable.
 * @param isDemo true while the demo identifier is in use; shows the demo note.
 */
@Composable
fun HomeScreen(
    recent: List<HistoryRecord>?,
    isDemo: Boolean,
    onIdentifyPlant: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRecord: (recordId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            BrandHeader()
            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(
                text = stringResource(R.string.action_identify_plant),
                onClick = onIdentifyPlant,
                iconRes = R.drawable.ic_photo_camera,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!recent.isNullOrEmpty()) {
                RecentIdentifications(recent, onOpenRecord, onOpenHistory)
            }
            if (isDemo) {
                StatusMessage(
                    tone = StatusTone.Info,
                    title = stringResource(R.string.home_demo_title),
                    message = stringResource(R.string.home_demo_message),
                )
            }
            TextCard(
                title = stringResource(R.string.home_scope_title),
                body = stringResource(R.string.home_scope_message),
            )
            SecondaryButton(
                text = stringResource(R.string.home_view_history),
                onClick = onOpenHistory,
                iconRes = R.drawable.ic_history,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RecentIdentifications(
    records: List<HistoryRecord>,
    onOpenRecord: (recordId: String) -> Unit,
    onViewAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_recent_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            TextButton(onClick = onViewAll) { Text(stringResource(R.string.action_view_all)) }
        }
        records.forEach { record ->
            HistoryRecordRow(record = record, onClick = { onOpenRecord(record.id) })
        }
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    val record = HistoryRecord(
        "1", Instant.EPOCH, HistoryStatus.Identified, "c", "p", "Preview Plant", "Preview species",
        ModelInfo("Preview", null, isDemo = true), null,
    )
    PlantIDTheme {
        HomeScreen(listOf(record), isDemo = true, onIdentifyPlant = {}, onOpenHistory = {}, onOpenRecord = {})
    }
}

@Preview(name = "Large font", fontScale = 1.6f, widthDp = 320)
@Composable
private fun HomeScreenLargeFontPreview() {
    PlantIDTheme { HomeScreen(null, isDemo = true, onIdentifyPlant = {}, onOpenHistory = {}, onOpenRecord = {}) }
}
