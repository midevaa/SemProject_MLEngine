package io.github.ephym6.plantid.ui.identify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.data.identification.mock.DemoScenario
import io.github.ephym6.plantid.ui.components.BackNavigationButton
import io.github.ephym6.plantid.ui.components.PhotoLoadState
import io.github.ephym6.plantid.ui.components.PlantIdCard
import io.github.ephym6.plantid.ui.components.PlantIdTopAppBar
import io.github.ephym6.plantid.ui.components.PlantPhoto
import io.github.ephym6.plantid.ui.components.PrimaryButton
import io.github.ephym6.plantid.ui.components.SecondaryButton
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Spacing
import java.io.File

/**
 * Lets the user check the selected photo before identifying. "Identify plant"
 * is enabled only once the photo has actually loaded.
 *
 * @param photo the stored photo, or null if the id was invalid (shown as an error).
 * @param demoScenario non-null while the demo identifier is in use; shows the Demo mode picker.
 */
@Composable
fun PhotoPreviewScreen(
    photo: File?,
    onBack: () -> Unit,
    onChooseAnother: () -> Unit,
    onIdentify: () -> Unit,
    modifier: Modifier = Modifier,
    demoScenario: DemoScenario? = null,
    onDemoScenarioSelected: (DemoScenario) -> Unit = {},
) {
    var photoState by remember(photo) { mutableStateOf(PhotoLoadState.Loading) }

    Scaffold(
        modifier = modifier,
        topBar = {
            PlantIdTopAppBar(
                title = stringResource(R.string.preview_title),
                navigationIcon = { BackNavigationButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            PlantPhoto(
                photo = photo,
                contentDescription = stringResource(R.string.preview_image_description),
                unavailableMessage = stringResource(R.string.preview_image_error),
                onLoadStateChange = { photoState = it },
            )
            Text(
                text = stringResource(R.string.preview_instruction),
                style = MaterialTheme.typography.titleMedium,
            )
            PhotoTips()
            if (demoScenario != null) {
                DemoModeCard(selected = demoScenario, onSelect = onDemoScenarioSelected)
            }
            PrimaryButton(
                text = stringResource(R.string.preview_identify),
                onClick = onIdentify,
                enabled = photoState == PhotoLoadState.Loaded,
                iconRes = R.drawable.ic_eco,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = stringResource(R.string.preview_choose_another),
                onClick = onChooseAnother,
                iconRes = R.drawable.ic_photo_library,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PhotoTips() {
    val tips = listOf(
        R.string.preview_tip_focus,
        R.string.preview_tip_leaves,
        R.string.preview_tip_light,
        R.string.preview_tip_single,
    )
    PlantIdCard {
        Text(
            text = stringResource(R.string.preview_tips_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        tips.forEach { tipRes ->
            Text(
                text = "•  " + stringResource(tipRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PhotoPreviewScreenPreview() {
    // No real file in previews: shows the "photo unavailable" state.
    PlantIDTheme {
        PhotoPreviewScreen(
            photo = null,
            onBack = {},
            onChooseAnother = {},
            onIdentify = {},
            demoScenario = DemoScenario.Identified,
        )
    }
}
