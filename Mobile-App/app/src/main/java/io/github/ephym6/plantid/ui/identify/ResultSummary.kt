package io.github.ephym6.plantid.ui.identify

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.domain.identification.ClassScore
import io.github.ephym6.plantid.domain.identification.PlantSummary
import io.github.ephym6.plantid.domain.identification.scoreFor
import io.github.ephym6.plantid.ui.components.PlantIdCard
import io.github.ephym6.plantid.ui.components.PlantPhoto
import io.github.ephym6.plantid.ui.components.SecondaryButton
import io.github.ephym6.plantid.ui.components.StatusMessage
import io.github.ephym6.plantid.ui.components.StatusTone
import io.github.ephym6.plantid.ui.components.TextCard
import io.github.ephym6.plantid.ui.components.formatConfidence
import io.github.ephym6.plantid.ui.theme.Spacing
import java.io.File

// Building blocks shared by the live result and a saved history record, so a
// saved result looks the same as when it was identified.

/** Shown on every result produced by the demo identifier. */
@Composable
fun DemoResultBanner() {
    StatusMessage(
        tone = StatusTone.Warning,
        title = stringResource(R.string.demo_banner_title),
        message = stringResource(R.string.demo_banner_message),
    )
}

/**
 * Photo and plant name in one card (visually associated), the model's calibrated
 * [confidence] if known (D43), then the success status. Call inside a Column.
 */
@Composable
fun IdentifiedSummary(
    commonName: String?,
    scientificName: String?,
    description: String?,
    photo: File?,
    confidence: Float? = null,
) {
    PlantIdCard {
        PlantPhoto(
            photo = photo,
            contentDescription = stringResource(R.string.result_photo_description),
            unavailableMessage = stringResource(R.string.photo_unavailable_short),
            aspectRatio = 4f / 3f,
        )
        Text(
            text = commonName ?: scientificName ?: stringResource(R.string.result_name_fallback),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(top = Spacing.sm)
                .semantics { heading() },
        )
        if (commonName != null && scientificName != null) {
            Text(
                text = scientificName,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        confidence?.let { ConfidenceLine(it) }
    }
    StatusMessage(
        tone = StatusTone.Success,
        title = stringResource(R.string.result_identified_title),
        message = stringResource(R.string.result_identified_message),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    description?.let { TextCard(title = stringResource(R.string.result_description_title), body = it) }
}

enum class NoMatchKind { Uncertain, Unsupported }

/**
 * Photo plus the uncertain/unsupported status. Never shows a plant as the
 * result. For uncertain results the model's guesses ([candidates]) appear in a
 * separate "possible matches (not confirmed)" card with their calibrated
 * percentages (D43) but no care guide links (D37). Call inside a Column.
 */
@Composable
fun NoMatchSummary(
    kind: NoMatchKind,
    photo: File?,
    candidates: List<PlantSummary> = emptyList(),
    scores: List<ClassScore> = emptyList(),
) {
    PlantPhoto(
        photo = photo,
        contentDescription = stringResource(R.string.result_photo_description),
        unavailableMessage = stringResource(R.string.photo_unavailable_short),
        aspectRatio = 4f / 3f,
    )
    StatusMessage(
        tone = if (kind == NoMatchKind.Uncertain) StatusTone.Warning else StatusTone.Info,
        title = stringResource(
            if (kind == NoMatchKind.Uncertain) R.string.result_uncertain_title else R.string.result_unsupported_title,
        ),
        message = stringResource(
            if (kind == NoMatchKind.Uncertain) R.string.result_uncertain_message else R.string.result_unsupported_message,
        ),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    if (kind == NoMatchKind.Uncertain && candidates.isNotEmpty()) {
        PossibleMatches(candidates, scores)
    }
    Text(
        text = stringResource(R.string.home_scope_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ConfidenceLine(confidence: Float) {
    Text(
        text = stringResource(R.string.result_confidence, formatConfidence(confidence)),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = Spacing.xs),
    )
    Text(
        text = stringResource(R.string.result_confidence_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PossibleMatches(candidates: List<PlantSummary>, scores: List<ClassScore>) {
    PlantIdCard {
        Text(
            text = stringResource(R.string.result_candidates_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.result_candidates_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        candidates.forEach { plant ->
            val name = plant.commonName ?: plant.scientificName ?: return@forEach
            val label = if (plant.commonName != null && plant.scientificName != null) {
                "$name (${plant.scientificName})"
            } else {
                name
            }
            val score = scores.scoreFor(plant.plantId)
            Text(
                text = "•  " + if (score != null) {
                    stringResource(R.string.result_candidate_with_confidence, label, formatConfidence(score))
                } else {
                    label
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Save state of the current result. */
enum class SaveState { NotSaveable, NotSaved, Saving, Saved, Failed }

/** "Save to history" and its feedback. Saving the same result twice isn't possible. */
@Composable
fun SaveToHistoryControl(state: SaveState, onSave: () -> Unit) {
    when (state) {
        SaveState.NotSaveable -> Unit
        SaveState.NotSaved, SaveState.Saving -> SecondaryButton(
            text = stringResource(
                if (state == SaveState.Saving) R.string.history_saving else R.string.action_save_to_history,
            ),
            onClick = onSave,
            enabled = state == SaveState.NotSaved,
            iconRes = R.drawable.ic_bookmark,
            modifier = Modifier.fillMaxWidth(),
        )
        SaveState.Saved -> StatusMessage(
            tone = StatusTone.Success,
            title = stringResource(R.string.history_saved_title),
            message = stringResource(R.string.history_saved_message),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        SaveState.Failed -> {
            StatusMessage(
                tone = StatusTone.Error,
                title = stringResource(R.string.history_save_failed_title),
                message = stringResource(R.string.history_save_failed_message),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            SecondaryButton(
                text = stringResource(R.string.action_try_again),
                onClick = onSave,
                iconRes = R.drawable.ic_bookmark,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
