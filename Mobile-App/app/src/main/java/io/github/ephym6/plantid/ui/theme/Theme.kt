package io.github.ephym6.plantid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LightColorScheme = lightColorScheme(
    primary = LeafGreen40,
    onPrimary = Color.White,
    primaryContainer = LeafGreen90,
    onPrimaryContainer = LeafGreen10,
    inversePrimary = LeafGreen80,
    secondary = Sage40,
    onSecondary = Color.White,
    secondaryContainer = Sage90,
    onSecondaryContainer = Sage10,
    tertiary = Teal40,
    onTertiary = Color.White,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Teal10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = NeutralLightBackground,
    onBackground = NeutralLightOnBackground,
    surface = NeutralLightBackground,
    onSurface = NeutralLightOnBackground,
    surfaceVariant = NeutralLightSurfaceVariant,
    onSurfaceVariant = NeutralLightOnSurfaceVariant,
    surfaceTint = LeafGreen40,
    inverseSurface = NeutralLightInverseSurface,
    inverseOnSurface = NeutralLightInverseOnSurface,
    outline = NeutralLightOutline,
    outlineVariant = NeutralLightOutlineVariant,
    surfaceBright = NeutralLightBackground,
    surfaceContainerLowest = NeutralLightContainerLowest,
    surfaceContainerLow = NeutralLightContainerLow,
    surfaceContainer = NeutralLightContainer,
    surfaceContainerHigh = NeutralLightContainerHigh,
    surfaceContainerHighest = NeutralLightContainerHighest,
)

internal val DarkColorScheme = darkColorScheme(
    primary = LeafGreen80,
    onPrimary = LeafGreen20,
    primaryContainer = LeafGreen30,
    onPrimaryContainer = LeafGreen90,
    inversePrimary = LeafGreen40,
    secondary = Sage80,
    onSecondary = Sage20,
    secondaryContainer = Sage30,
    onSecondaryContainer = Sage90,
    tertiary = Teal80,
    onTertiary = Teal20,
    tertiaryContainer = Teal30,
    onTertiaryContainer = Teal90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = NeutralDarkBackground,
    onBackground = NeutralDarkOnBackground,
    surface = NeutralDarkBackground,
    onSurface = NeutralDarkOnBackground,
    surfaceVariant = NeutralDarkSurfaceVariant,
    onSurfaceVariant = NeutralDarkOnSurfaceVariant,
    surfaceTint = LeafGreen80,
    inverseSurface = NeutralDarkInverseSurface,
    inverseOnSurface = NeutralDarkInverseOnSurface,
    outline = NeutralDarkOutline,
    outlineVariant = NeutralDarkOutlineVariant,
    surfaceDim = NeutralDarkBackground,
    surfaceContainerLowest = NeutralDarkContainerLowest,
    surfaceContainerLow = NeutralDarkContainerLow,
    surfaceContainer = NeutralDarkContainer,
    surfaceContainerHigh = NeutralDarkContainerHigh,
    surfaceContainerHighest = NeutralDarkContainerHighest,
)

/**
 * Colour roles that Material 3 does not provide. "Warning" is used for cautionary
 * states (for example, a future uncertain identification) so they don't borrow
 * the error colour.
 */
@Immutable
data class ExtendedColors(
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

internal val LightExtendedColors = ExtendedColors(
    warning = Amber40,
    onWarning = Color.White,
    warningContainer = Amber90,
    onWarningContainer = Amber10,
)

internal val DarkExtendedColors = ExtendedColors(
    warning = Amber80,
    onWarning = Amber20,
    warningContainer = Amber30,
    onWarningContainer = Amber90,
)

private val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/**
 * PlantID theme. Follows the system light/dark setting. Dynamic (wallpaper) colour
 * is intentionally not used so the botanical brand stays consistent (DECISIONS.md Q8).
 */
@Composable
fun PlantIDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}

/** Access to PlantID-specific theme values alongside [MaterialTheme]. */
object PlantIDTheme {
    val extendedColors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
