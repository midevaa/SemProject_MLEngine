package io.github.ephym6.plantid.ui.care

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.domain.plant.CareConfidence
import io.github.ephym6.plantid.domain.plant.CareData
import io.github.ephym6.plantid.domain.plant.CareInfo
import io.github.ephym6.plantid.domain.plant.CareRecommendations
import io.github.ephym6.plantid.domain.plant.CareTip
import io.github.ephym6.plantid.domain.plant.ContentSource
import io.github.ephym6.plantid.domain.plant.PetSafety
import io.github.ephym6.plantid.domain.plant.PetSafetyBasis
import io.github.ephym6.plantid.domain.plant.Plant
import io.github.ephym6.plantid.ui.care.CareGuideViewModel.UiState
import io.github.ephym6.plantid.ui.components.BackNavigationButton
import io.github.ephym6.plantid.ui.components.CenteredScrollableColumn
import io.github.ephym6.plantid.ui.components.EmptyState
import io.github.ephym6.plantid.ui.components.ErrorState
import io.github.ephym6.plantid.ui.components.LoadingIndicator
import io.github.ephym6.plantid.ui.components.PlantIdCard
import io.github.ephym6.plantid.ui.components.PlantIdTopAppBar
import io.github.ephym6.plantid.ui.components.StatusMessage
import io.github.ephym6.plantid.ui.components.StatusTone
import io.github.ephym6.plantid.ui.components.TextCard
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Sizes
import io.github.ephym6.plantid.ui.theme.Spacing
import java.text.NumberFormat

/**
 * Care guide for one plant (spec §6.8). Shows only tips the rule-based
 * recommender produced from source values, each worded by a reviewed template,
 * plus how confident the source is, the ML team's note, the source's own
 * wording for converted values, pet safety (D41) and the source. Missing values
 * are simply left out. Back returns to where the guide was opened from.
 */
@Composable
fun CareGuideScreen(
    state: UiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            PlantIdTopAppBar(
                title = stringResource(R.string.care_title),
                navigationIcon = { BackNavigationButton(onClick = onBack) },
            )
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (state) {
            UiState.Loading -> CenteredScrollableColumn(contentModifier) {
                LoadingIndicator(message = stringResource(R.string.care_loading))
            }
            is UiState.Ready -> CareGuideContent(state.plant, state.tips, contentModifier)
            UiState.NoCareGuide -> CenteredScrollableColumn(contentModifier) {
                EmptyState(
                    iconRes = R.drawable.ic_eco,
                    title = stringResource(R.string.care_none_title),
                    message = stringResource(R.string.care_none_message),
                )
            }
            UiState.Unavailable -> CenteredScrollableColumn(contentModifier) {
                ErrorState(
                    title = stringResource(R.string.care_error_title),
                    message = stringResource(R.string.care_error_message),
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun CareGuideContent(plant: Plant, tips: List<CareTip>, modifier: Modifier) {
    val info = plant.careInfo
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        PlantHeader(plant)
        StatusMessage(
            tone = StatusTone.Info,
            title = stringResource(R.string.care_general_title),
            message = stringResource(R.string.care_general_message),
        )
        when (info?.confidence) {
            CareConfidence.Medium -> StatusMessage(
                tone = StatusTone.Warning,
                title = stringResource(R.string.care_confidence_medium_title),
                message = stringResource(R.string.care_confidence_medium_message),
            )
            CareConfidence.Low -> StatusMessage(
                tone = StatusTone.Warning,
                title = stringResource(R.string.care_confidence_low_title),
                message = stringResource(R.string.care_confidence_low_message),
            )
            CareConfidence.High, null -> Unit
        }
        info?.note?.let { TextCard(title = stringResource(R.string.care_note_title), body = it) }
        tips.forEach { tip -> CareTipCard(tip) }
        info?.sourceSaid?.let { TextCard(title = stringResource(R.string.care_source_said_title), body = it) }
        plant.petSafety?.let { PetSafetyCard(it) }
        info?.source?.let { SourceCard(it) }
    }
}

@Composable
private fun PlantHeader(plant: Plant) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = plant.commonName ?: plant.scientificName ?: stringResource(R.string.result_name_fallback),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (plant.commonName != null && plant.scientificName != null) {
            Text(
                text = plant.scientificName,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        plant.family?.let { family ->
            Text(
                text = stringResource(R.string.care_family, family),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CareTipCard(tip: CareTip) {
    val numbers = remember { NumberFormat.getIntegerInstance() }
    when (tip) {
        is CareTip.Light -> {
            val luxText = tip.lux?.let { lux ->
                if (lux.first == lux.last) {
                    stringResource(R.string.care_light_single, numbers.format(lux.first))
                } else {
                    stringResource(R.string.care_light_range, numbers.format(lux.first), numbers.format(lux.last))
                }
            }
            TipCard(
                iconRes = R.drawable.ic_light_mode,
                title = stringResource(R.string.care_light_title),
                // Plain-language level first; the lux range supports it.
                value = tip.level ?: luxText.orEmpty(),
                note = listOfNotNull(
                    if (tip.level != null && luxText != null) stringResource(R.string.care_light_measured, luxText) else null,
                    if (luxText != null) stringResource(R.string.care_light_note) else null,
                ).joinToString(" ").ifEmpty { null },
            )
        }
        is CareTip.Watering -> TipCard(
            iconRes = R.drawable.ic_water_drop,
            title = stringResource(R.string.care_watering_title),
            value = pluralStringResource(R.plurals.care_watering_interval, tip.typicalIntervalDays, tip.typicalIntervalDays),
            note = stringResource(R.string.care_watering_note),
        )
        is CareTip.Temperature -> TipCard(
            iconRes = R.drawable.ic_thermostat,
            title = stringResource(R.string.care_temperature_title),
            value = if (tip.celsius.first == tip.celsius.last) {
                stringResource(R.string.care_temperature_single, tip.celsius.first)
            } else {
                stringResource(R.string.care_temperature_range, tip.celsius.first, tip.celsius.last)
            },
            note = stringResource(R.string.care_temperature_note),
        )
        is CareTip.Humidity -> TipCard(
            iconRes = R.drawable.ic_humidity_percentage,
            title = stringResource(R.string.care_humidity_title),
            value = stringResource(R.string.care_humidity_value, tip.percent),
            note = stringResource(R.string.care_humidity_note),
        )
    }
}

@Composable
private fun TipCard(
    @DrawableRes iconRes: Int,
    title: String,
    value: String,
    note: String?,
) {
    PlantIdCard {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Sizes.iconMedium),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(text = value, style = MaterialTheme.typography.titleMedium)
                note?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Pet safety (D41): summary, the source's details, how it was established, the
 * source and a "not veterinary advice" note. Toxic plants use the warning tone
 * (with its own icon and text, not colour alone).
 */
@Composable
private fun PetSafetyCard(pet: PetSafety) {
    val anyToxic = pet.toxicToDogs || pet.toxicToCats
    StatusMessage(
        tone = if (anyToxic) StatusTone.Warning else StatusTone.Info,
        title = stringResource(R.string.care_pet_title) + ": " + stringResource(
            when {
                pet.toxicToDogs && pet.toxicToCats -> R.string.care_pet_summary_both
                pet.toxicToDogs -> R.string.care_pet_summary_dogs
                pet.toxicToCats -> R.string.care_pet_summary_cats
                else -> R.string.care_pet_summary_none
            },
        ),
        message = listOfNotNull(
            pet.details,
            stringResource(
                when (pet.basis) {
                    PetSafetyBasis.AspcaVerified -> R.string.care_pet_basis_aspca
                    PetSafetyBasis.ExtensionListed -> R.string.care_pet_basis_extension
                    PetSafetyBasis.GenusInferred -> R.string.care_pet_basis_genus
                    PetSafetyBasis.Unknown -> R.string.care_pet_basis_unknown
                },
            ),
            pet.source?.let { stringResource(R.string.care_pet_source, it.name) },
            stringResource(R.string.care_pet_disclaimer),
        ).joinToString("\n\n"),
    )
}

@Composable
private fun SourceCard(source: ContentSource) {
    PlantIdCard {
        Text(
            text = stringResource(R.string.care_source_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = if (source.license != null) {
                stringResource(R.string.care_source_with_license, source.name, source.license)
            } else {
                source.name
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        source.url?.let { url ->
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Previews (fictional data) ----

private val previewCare = CareData(
    lightLevel = "Bright Indirect",
    lightLux = 500..3000,
    temperatureCelsius = 15..30,
    humidityPercent = 50,
    wateringIntervalDays = 7,
)
private val previewPlant = Plant(
    id = "preview",
    classLabel = "preview",
    commonName = "Preview Plant",
    scientificName = "Preview species (fictional)",
    family = "Previewaceae",
    care = previewCare,
    careInfo = CareInfo(
        source = ContentSource("Preview source", "https://example.org", null),
        confidence = CareConfidence.Medium,
        note = "Preview note.",
        sourceSaid = "Preview wording from the source.",
    ),
    petSafety = PetSafety(true, true, "Preview symptoms.", PetSafetyBasis.ExtensionListed, ContentSource("Preview", null, null)),
)

@PreviewLightDark
@Composable
private fun CareGuideReadyPreview() {
    PlantIDTheme {
        CareGuideScreen(
            state = UiState.Ready(previewPlant, CareRecommendations.tipsFor(previewCare)),
            onBack = {},
            onRetry = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun CareGuideNonePreview() {
    PlantIDTheme { CareGuideScreen(state = UiState.NoCareGuide, onBack = {}, onRetry = {}) }
}
