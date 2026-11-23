package io.github.ephym6.plantid.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG 2.1 contrast checks for the colour pairs PlantID actually uses, in both
 * themes. Text needs 4.5:1 (AA); icons and other non-text UI need 3:1. Fails
 * if a colour change makes any pair harder to read.
 */
class ColorContrastTest {

    private data class Pair(val use: String, val foreground: Color, val background: Color, val minimum: Double)

    private fun pairs(scheme: ColorScheme, extended: ExtendedColors) = listOf(
        Pair("primary button text", scheme.onPrimary, scheme.primary, TEXT),
        Pair("success banner", scheme.onPrimaryContainer, scheme.primaryContainer, TEXT),
        Pair("info banner", scheme.onTertiaryContainer, scheme.tertiaryContainer, TEXT),
        Pair("error banner", scheme.onErrorContainer, scheme.errorContainer, TEXT),
        Pair("warning / demo banner", extended.onWarningContainer, extended.warningContainer, TEXT),
        Pair("body text", scheme.onBackground, scheme.background, TEXT),
        Pair("secondary text", scheme.onSurfaceVariant, scheme.background, TEXT),
        Pair("card secondary text", scheme.onSurfaceVariant, scheme.surfaceContainerLow, TEXT),
        Pair("card text", scheme.onSurface, scheme.surfaceContainerLow, TEXT),
        Pair("photo frame / source option text", scheme.onSurfaceVariant, scheme.surfaceContainerHigh, TEXT),
        Pair("text buttons, brand name", scheme.primary, scheme.background, TEXT),
        Pair("destructive dialog action", scheme.error, scheme.surfaceContainerHigh, TEXT),
        Pair("care tip and status icons", scheme.primary, scheme.surfaceContainerLow, NON_TEXT),
        Pair("uncertain status icon", extended.warning, scheme.surfaceContainerLow, NON_TEXT),
        Pair("photo error icon", scheme.error, scheme.surfaceContainerHigh, NON_TEXT),
        Pair("outlined button border", scheme.outline, scheme.background, NON_TEXT),
    )

    @Test
    fun `light theme meets WCAG AA`() = check("light", pairs(LightColorScheme, LightExtendedColors))

    @Test
    fun `dark theme meets WCAG AA`() = check("dark", pairs(DarkColorScheme, DarkExtendedColors))

    private fun check(theme: String, pairs: List<Pair>) {
        val failures = pairs.mapNotNull { pair ->
            val ratio = contrast(pair.foreground, pair.background)
            if (ratio < pair.minimum) "${pair.use}: %.2f:1 (needs %.1f:1)".format(ratio, pair.minimum) else null
        }
        assertTrue("$theme theme contrast failures:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** WCAG relative luminance of an opaque sRGB colour. */
    private fun luminance(color: Color): Double {
        fun channel(c: Float): Double = if (c <= 0.04045f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private companion object {
        const val TEXT = 4.5
        const val NON_TEXT = 3.0
    }
}
