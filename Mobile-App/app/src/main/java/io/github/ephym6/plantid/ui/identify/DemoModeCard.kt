package io.github.ephym6.plantid.ui.identify

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.data.identification.mock.DemoScenario
import io.github.ephym6.plantid.ui.components.PlantIdCard
import io.github.ephym6.plantid.ui.theme.PlantIDTheme
import io.github.ephym6.plantid.ui.theme.Sizes
import io.github.ephym6.plantid.ui.theme.Spacing

/**
 * Lets testers choose the mock's next outcome. Shown only while the demo
 * identifier is in use, so it disappears once a real model is connected.
 */
@Composable
fun DemoModeCard(
    selected: DemoScenario,
    onSelect: (DemoScenario) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlantIdCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.demo_mode_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.demo_mode_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.selectableGroup()) {
            DemoScenario.entries.forEach { scenario ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.buttonMinHeight)
                        .selectable(
                            selected = scenario == selected,
                            onClick = { onSelect(scenario) },
                            role = Role.RadioButton,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // onClick = null: the whole row is the touch target.
                    RadioButton(selected = scenario == selected, onClick = null)
                    Text(
                        text = stringResource(scenario.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@get:StringRes
internal val DemoScenario.labelRes: Int
    get() = when (this) {
        DemoScenario.Identified -> R.string.demo_scenario_identified
        DemoScenario.MissingDetails -> R.string.demo_scenario_missing_details
        DemoScenario.Uncertain -> R.string.demo_scenario_uncertain
        DemoScenario.Unsupported -> R.string.demo_scenario_unsupported
        DemoScenario.Failure -> R.string.demo_scenario_failure
        DemoScenario.Slow -> R.string.demo_scenario_slow
    }

@PreviewLightDark
@Composable
private fun DemoModeCardPreview() {
    PlantIDTheme {
        Surface {
            DemoModeCard(
                selected = DemoScenario.Uncertain,
                onSelect = {},
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}
