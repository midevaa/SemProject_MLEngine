package io.github.ephym6.plantid.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.domain.plant.ContentSource
import io.github.ephym6.plantid.domain.plant.Plant
import io.github.ephym6.plantid.domain.plant.PlantCatalog
import io.github.ephym6.plantid.ui.components.PlantIdCard
import io.github.ephym6.plantid.ui.components.PlantIdTopAppBar
import io.github.ephym6.plantid.ui.components.TextCard
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Spacing

/**
 * About: purpose, scope (the supported plants), prototype disclaimer, privacy,
 * data credits and version. Keep the privacy text in line with what the app
 * actually does (DECISIONS D11).
 *
 * @param catalog null while loading.
 */
@Composable
fun AboutScreen(
    versionName: String,
    catalog: Result<PlantCatalog>?,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { PlantIdTopAppBar(title = stringResource(R.string.about_title)) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            TextCard(
                title = stringResource(R.string.about_purpose_title),
                body = stringResource(R.string.about_purpose_body),
            )
            TextCard(
                title = stringResource(R.string.about_prototype_title),
                body = stringResource(R.string.about_prototype_body),
            )
            SupportedPlantsCard(catalog)
            TextCard(
                title = stringResource(R.string.about_privacy_title),
                body = stringResource(R.string.about_privacy_body),
            )
            catalog?.getOrNull()?.sources?.takeIf { it.isNotEmpty() }?.let { SourcesCard(it) }
            Text(
                text = stringResource(R.string.about_version, versionName),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun SupportedPlantsCard(catalog: Result<PlantCatalog>?) {
    PlantIdCard {
        Text(
            text = stringResource(R.string.about_supported_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        val plants = catalog?.getOrNull()?.plants
        when {
            catalog == null -> Unit // loading: the title alone, briefly
            plants == null -> BodyText(stringResource(R.string.about_supported_unavailable))
            else -> {
                BodyText(pluralStringResource(R.plurals.about_supported_intro, plants.size, plants.size))
                plants.sortedBy { it.commonName ?: it.scientificName }.forEach { plant ->
                    BodyText("•  " + plant.displayName())
                }
            }
        }
    }
}

/** Credits every source behind the care guides and pet safety, with licences where known. */
@Composable
private fun SourcesCard(sources: List<ContentSource>) {
    val names = sources.joinToString("; ") { if (it.license != null) "${it.name} (${it.license})" else it.name }
    TextCard(
        title = stringResource(R.string.about_sources_title),
        body = stringResource(R.string.about_sources_body, names),
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun Plant.displayName(): String = when {
    commonName != null && scientificName != null -> "$commonName ($scientificName)"
    else -> commonName ?: scientificName ?: id
}

@PreviewLightDark
@Composable
private fun AboutScreenPreview() {
    val catalog = PlantCatalog(
        plants = listOf(
            Plant("a", "A", "Preview Plant", "Preview species (fictional)", null, null),
        ),
    )
    PlantIDTheme { AboutScreen(versionName = "1.0", catalog = Result.success(catalog)) }
}
