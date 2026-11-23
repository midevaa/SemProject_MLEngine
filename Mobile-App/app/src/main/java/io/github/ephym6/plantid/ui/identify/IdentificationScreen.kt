package io.github.ephym6.plantid.ui.identify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.domain.identification.FailureReason
import io.github.ephym6.plantid.domain.identification.IdentificationOutcome
import io.github.ephym6.plantid.domain.identification.ModelInfo
import io.github.ephym6.plantid.domain.identification.PlantSummary
import io.github.ephym6.plantid.ui.components.BackNavigationButton
import io.github.ephym6.plantid.ui.components.CenteredScrollableColumn
import io.github.ephym6.plantid.ui.components.ErrorState
import io.github.ephym6.plantid.ui.components.LoadingIndicator
import io.github.ephym6.plantid.ui.components.PlantIdTopAppBar
import io.github.ephym6.plantid.ui.components.PlantPhoto
import io.github.ephym6.plantid.ui.components.PrimaryButton
import io.github.ephym6.plantid.ui.components.SecondaryButton
import io.github.ephym6.plantid.ui.identify.IdentificationViewModel.UiState
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Spacing
import java.io.File

/**
 * "Analyzing your plant…" followed by the outcome. Each outcome looks and reads
 * differently, and none relies on colour alone:
 * - Identified: photo + plant name together, success banner.
 * - Uncertain: warning banner, **no species name**.
 * - Unsupported: info banner, no plant at all.
 * - Failed: error panel with Try again where retrying can help.
 * Demo results always carry a "Demo result" banner.
 *
 * Identified results offer the care guide. Every result except a failure can
 * be saved to history.
 */
@Composable
fun IdentificationScreen(
    state: UiState,
    photo: File?,
    saveState: SaveState,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onIdentifyAnother: () -> Unit,
    onBackToHome: () -> Unit,
    onViewCareGuide: (plantId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            PlantIdTopAppBar(
                title = stringResource(
                    if (state is UiState.Identifying) R.string.identifying_title else R.string.result_title,
                ),
                navigationIcon = { BackNavigationButton(onClick = onBack) },
            )
        },
    ) { padding ->
        when (state) {
            UiState.Identifying -> IdentifyingContent(photo, Modifier.padding(padding))
            is UiState.Finished -> ResultContent(
                outcome = state.outcome,
                photo = photo,
                saveState = saveState,
                onSave = onSave,
                onRetry = onRetry,
                onIdentifyAnother = onIdentifyAnother,
                onBackToHome = onBackToHome,
                onViewCareGuide = onViewCareGuide,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun IdentifyingContent(photo: File?, modifier: Modifier) {
    CenteredScrollableColumn(modifier = modifier.padding(horizontal = Spacing.screenHorizontal)) {
        PlantPhoto(
            photo = photo,
            contentDescription = stringResource(R.string.result_photo_description),
            unavailableMessage = stringResource(R.string.photo_unavailable_short),
            aspectRatio = 1f,
            modifier = Modifier.widthIn(max = THUMBNAIL_MAX_WIDTH),
        )
        // No cancel button: Back leaves the screen, which safely cancels the request.
        LoadingIndicator(message = stringResource(R.string.identifying_message))
    }
}

@Composable
private fun ResultContent(
    outcome: IdentificationOutcome,
    photo: File?,
    saveState: SaveState,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onIdentifyAnother: () -> Unit,
    onBackToHome: () -> Unit,
    onViewCareGuide: (plantId: String) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        if (outcome.model.isDemo) DemoResultBanner()
        when (outcome) {
            is IdentificationOutcome.Identified -> IdentifiedSummary(
                commonName = outcome.plant.commonName,
                scientificName = outcome.plant.scientificName,
                description = outcome.plant.description,
                photo = photo,
                confidence = outcome.confidence,
            )
            is IdentificationOutcome.Uncertain -> NoMatchSummary(NoMatchKind.Uncertain, photo, outcome.candidates, outcome.scores)
            is IdentificationOutcome.Unsupported -> NoMatchSummary(NoMatchKind.Unsupported, photo)
            is IdentificationOutcome.Failed -> FailedContent(outcome.reason, onRetry, onIdentifyAnother, onBackToHome)
        }
        if (outcome is IdentificationOutcome.Failed) return@Column

        if (outcome is IdentificationOutcome.Identified) {
            PrimaryButton(
                text = stringResource(R.string.action_view_care_guide),
                onClick = { onViewCareGuide(outcome.plant.plantId) },
                iconRes = R.drawable.ic_menu_book,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SaveToHistoryControl(state = saveState, onSave = onSave)
        if (outcome is IdentificationOutcome.Identified) {
            SecondaryButton(
                text = stringResource(R.string.action_identify_another),
                onClick = onIdentifyAnother,
                iconRes = R.drawable.ic_photo_camera,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.action_try_another_photo),
                onClick = onIdentifyAnother,
                iconRes = R.drawable.ic_photo_camera,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TextButton(onClick = onBackToHome, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_back_to_home))
        }
    }
}

@Composable
private fun FailedContent(
    reason: FailureReason,
    onRetry: () -> Unit,
    onIdentifyAnother: () -> Unit,
    onBackToHome: () -> Unit,
) {
    val photoGone = reason == FailureReason.PhotoUnavailable
    ErrorState(
        title = stringResource(R.string.result_failed_title),
        message = stringResource(
            when (reason) {
                FailureReason.PhotoUnavailable -> R.string.result_failed_photo
                FailureReason.ModelUnavailable -> R.string.result_failed_model
                FailureReason.ProcessingFailed -> R.string.result_failed_processing
                FailureReason.TimedOut -> R.string.result_failed_timed_out
            },
        ),
        // Retrying can't bring a missing photo back; offer a new photo instead.
        onRetry = if (photoGone) onIdentifyAnother else onRetry,
        retryLabel = stringResource(if (photoGone) R.string.action_try_another_photo else R.string.action_try_again),
        secondaryAction = {
            TextButton(onClick = onBackToHome) { Text(stringResource(R.string.action_back_to_home)) }
        },
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

private val THUMBNAIL_MAX_WIDTH = 200.dp

// ---- Previews (fictional demo data, no photo) ----

private val previewModel = ModelInfo(name = "Preview", version = null, isDemo = true)
private val previewPlant = PlantSummary("preview_plant", "Preview Plant", "Preview species (fictional)", "Preview description.")
private val previewPlantMinimal = PlantSummary("preview_minimal", "Preview Plant", null, null)

@Composable
private fun ResultPreview(state: UiState) {
    PlantIDTheme {
        IdentificationScreen(
            state = state,
            photo = null,
            saveState = if (state is UiState.Finished && state.outcome !is IdentificationOutcome.Failed) {
                SaveState.NotSaved
            } else {
                SaveState.NotSaveable
            },
            onSave = {},
            onBack = {},
            onRetry = {},
            onIdentifyAnother = {},
            onBackToHome = {},
            onViewCareGuide = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun IdentifyingPreview() = ResultPreview(UiState.Identifying)

@PreviewLightDark
@Composable
private fun IdentifiedPreview() = ResultPreview(
    UiState.Finished(IdentificationOutcome.Identified(previewModel, "preview_class", previewPlant)),
)

@PreviewLightDark
@Composable
private fun IdentifiedMissingDetailsPreview() = ResultPreview(
    UiState.Finished(IdentificationOutcome.Identified(previewModel, "preview_class", previewPlantMinimal)),
)

@PreviewLightDark
@Composable
private fun UncertainPreview() = ResultPreview(UiState.Finished(IdentificationOutcome.Uncertain(previewModel)))

@PreviewLightDark
@Composable
private fun UnsupportedPreview() = ResultPreview(UiState.Finished(IdentificationOutcome.Unsupported(previewModel)))

@PreviewLightDark
@Composable
private fun FailedPreview() = ResultPreview(
    UiState.Finished(IdentificationOutcome.Failed(previewModel, FailureReason.ProcessingFailed)),
)
