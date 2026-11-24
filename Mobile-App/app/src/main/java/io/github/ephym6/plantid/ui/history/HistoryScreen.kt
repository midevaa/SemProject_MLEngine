package io.github.ephym6.plantid.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.domain.history.HistoryRecord
import io.github.ephym6.plantid.domain.history.HistoryStatus
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.ui.components.CenteredScrollableColumn
import io.github.ephym6.plantid.ui.components.EmptyState
import io.github.ephym6.plantid.ui.components.ErrorState
import io.github.ephym6.plantid.ui.components.LoadingIndicator
import io.github.ephym6.plantid.ui.components.PlantIdTopAppBar
import io.github.ephym6.plantid.ui.components.PrimaryButton
import io.github.ephym6.plantid.ui.history.HistoryViewModel.UiState
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Spacing
import java.time.Instant

/**
 * History (spec §6.9): saved identifications newest first, or an empty state
 * that offers to identify a plant. "Clear history" asks for confirmation first.
 */
@Composable
fun HistoryScreen(
    state: UiState,
    clearFailed: Boolean,
    onIdentifyPlant: () -> Unit,
    onOpenRecord: (recordId: String) -> Unit,
    onClearAll: () -> Unit,
    onDismissClearError: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    exportFailed: Boolean = false,
    onExport: () -> Unit = {},
    onDismissExportError: () -> Unit = {},
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var confirmExport by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            PlantIdTopAppBar(
                title = stringResource(R.string.history_title),
                actions = {
                    if (state is UiState.Records) {
                        IconButton(onClick = { confirmExport = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_share),
                                contentDescription = stringResource(R.string.history_export),
                            )
                        }
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete_sweep),
                                contentDescription = stringResource(R.string.history_clear_all),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            UiState.Loading -> CenteredScrollableColumn(Modifier.padding(padding)) {
                LoadingIndicator(message = stringResource(R.string.history_loading))
            }
            UiState.Empty -> CenteredScrollableColumn(Modifier.padding(padding)) {
                EmptyState(
                    iconRes = R.drawable.ic_history,
                    title = stringResource(R.string.history_empty_title),
                    message = stringResource(R.string.history_empty_message),
                    action = {
                        PrimaryButton(
                            text = stringResource(R.string.action_identify_plant),
                            onClick = onIdentifyPlant,
                            iconRes = R.drawable.ic_photo_camera,
                        )
                    },
                )
            }
            UiState.Error -> CenteredScrollableColumn(Modifier.padding(padding)) {
                ErrorState(
                    title = stringResource(R.string.history_load_error_title),
                    message = stringResource(R.string.history_load_error_message),
                    onRetry = onRetry,
                )
            }
            is UiState.Records -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.records, key = { it.id }) { record ->
                    HistoryRecordRow(record = record, onClick = { onOpenRecord(record.id) })
                }
            }
        }
    }

    if (confirmClear && state is UiState.Records) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(painterResource(R.drawable.ic_delete_sweep), contentDescription = null) },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = {
                Text(pluralStringResource(R.plurals.history_clear_message, state.records.size, state.records.size))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) {
                    Text(stringResource(R.string.action_clear_all), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (confirmExport && state is UiState.Records) {
        AlertDialog(
            onDismissRequest = { confirmExport = false },
            icon = { Icon(painterResource(R.drawable.ic_share), contentDescription = null) },
            title = { Text(stringResource(R.string.history_export_title)) },
            text = {
                Text(pluralStringResource(R.plurals.history_export_message, state.records.size, state.records.size))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmExport = false
                    onExport()
                }) { Text(stringResource(R.string.action_export)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmExport = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (exportFailed) {
        AlertDialog(
            onDismissRequest = onDismissExportError,
            title = { Text(stringResource(R.string.history_error_title)) },
            text = { Text(stringResource(R.string.history_export_failed)) },
            confirmButton = { TextButton(onClick = onDismissExportError) { Text(stringResource(R.string.action_ok)) } },
        )
    }

    if (clearFailed) {
        AlertDialog(
            onDismissRequest = onDismissClearError,
            title = { Text(stringResource(R.string.history_error_title)) },
            text = { Text(stringResource(R.string.history_clear_failed)) },
            confirmButton = { TextButton(onClick = onDismissClearError) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}

@PreviewLightDark
@Composable
private fun HistoryScreenRecordsPreview() {
    val model = ModelInfo("Preview", null, isDemo = true)
    PlantIDTheme {
        HistoryScreen(
            state = UiState.Records(
                listOf(
                    HistoryRecord("1", Instant.EPOCH, HistoryStatus.Identified, "c", "p", "Preview Plant", "Preview species", model, null),
                    HistoryRecord("2", Instant.EPOCH, HistoryStatus.Unsupported, null, null, null, null, model, null),
                ),
            ),
            clearFailed = false,
            onIdentifyPlant = {},
            onOpenRecord = {},
            onClearAll = {},
            onDismissClearError = {},
            onRetry = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HistoryScreenEmptyPreview() {
    PlantIDTheme {
        HistoryScreen(UiState.Empty, false, {}, {}, {}, {}, {})
    }
}
