package io.github.ephym6.plantid.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryStatus
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.ui.components.BackNavigationButton
import io.github.ephym6.plantid.ui.components.CenteredScrollableColumn
import io.github.ephym6.plantid.ui.components.EmptyState
import io.github.ephym6.plantid.ui.components.ErrorState
import io.github.ephym6.plantid.ui.components.LoadingIndicator
import io.github.ephym6.plantid.ui.components.PlantIdTopAppBar
import io.github.ephym6.plantid.ui.components.PrimaryButton
import io.github.ephym6.plantid.ui.components.SecondaryButton
import io.github.ephym6.plantid.ui.history.HistoryDetailViewModel.UiState
import io.github.ephym6.plantid.ui.identify.DemoResultBanner
import io.github.ephym6.plantid.ui.identify.IdentifiedSummary
import io.github.ephym6.plantid.ui.identify.NoMatchKind
import io.github.ephym6.plantid.ui.identify.NoMatchSummary
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Spacing
import java.time.Instant

/**
 * A saved identification, shown as it looked when identified (snapshot), with
 * its care guide (if identified) and Delete (with confirmation).
 */
@Composable
fun HistoryDetailScreen(
    state: UiState,
    deleted: Boolean,
    deleteFailed: Boolean,
    onBack: () -> Unit,
    onViewCareGuide: (plantId: String) -> Unit,
    onDelete: () -> Unit,
    onDismissDeleteError: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(deleted) { if (deleted) currentOnBack() }

    Scaffold(
        modifier = modifier,
        topBar = {
            PlantIdTopAppBar(
                title = stringResource(R.string.history_detail_title),
                navigationIcon = { BackNavigationButton(onClick = onBack) },
            )
        },
    ) { padding ->
        when (state) {
            UiState.Loading -> CenteredScrollableColumn(Modifier.padding(padding)) {
                LoadingIndicator(message = stringResource(R.string.history_loading))
            }
            UiState.NotFound -> CenteredScrollableColumn(Modifier.padding(padding)) {
                EmptyState(
                    iconRes = R.drawable.ic_history,
                    title = stringResource(R.string.history_not_found_title),
                    message = stringResource(R.string.history_not_found_message),
                )
            }
            UiState.Error -> CenteredScrollableColumn(Modifier.padding(padding)) {
                ErrorState(
                    title = stringResource(R.string.history_load_error_title),
                    message = stringResource(R.string.history_load_error_message),
                    onRetry = onRetry,
                )
            }
            is UiState.Found -> RecordContent(
                record = state.record,
                onViewCareGuide = onViewCareGuide,
                onDeleteRequested = { confirmDelete = true },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (confirmDelete && state is UiState.Found) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (deleteFailed) {
        AlertDialog(
            onDismissRequest = onDismissDeleteError,
            title = { Text(stringResource(R.string.history_error_title)) },
            text = { Text(stringResource(R.string.history_delete_failed)) },
            confirmButton = { TextButton(onClick = onDismissDeleteError) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}

@Composable
private fun RecordContent(
    record: HistoryRecord,
    onViewCareGuide: (plantId: String) -> Unit,
    onDeleteRequested: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        if (record.model.isDemo) DemoResultBanner()
        Text(
            text = stringResource(R.string.history_identified_on, formatIdentifiedAt(record.identifiedAt)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (record.status) {
            HistoryStatus.Identified -> IdentifiedSummary(
                commonName = record.commonName,
                scientificName = record.scientificName,
                description = null,
                photo = record.photo,
                confidence = record.confidence,
            )
            HistoryStatus.Uncertain -> NoMatchSummary(NoMatchKind.Uncertain, record.photo)
            HistoryStatus.Unsupported -> NoMatchSummary(NoMatchKind.Unsupported, record.photo)
        }
        val plantId = record.plantId
        if (record.status == HistoryStatus.Identified && plantId != null) {
            PrimaryButton(
                text = stringResource(R.string.action_view_care_guide),
                onClick = { onViewCareGuide(plantId) },
                iconRes = R.drawable.ic_menu_book,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SecondaryButton(
            text = stringResource(R.string.action_delete_from_history),
            onClick = onDeleteRequested,
            iconRes = R.drawable.ic_delete,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewLightDark
@Composable
private fun HistoryDetailPreview() {
    PlantIDTheme {
        HistoryDetailScreen(
            state = UiState.Found(
                HistoryRecord(
                    "1", Instant.EPOCH, HistoryStatus.Identified, "c", "p", "Preview Plant",
                    "Preview species (fictional)", ModelInfo("Preview", null, isDemo = true), null,
                ),
            ),
            deleted = false,
            deleteFailed = false,
            onBack = {},
            onViewCareGuide = {},
            onDelete = {},
            onDismissDeleteError = {},
            onRetry = {},
        )
    }
}
