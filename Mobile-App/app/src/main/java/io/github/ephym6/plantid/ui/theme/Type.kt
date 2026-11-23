package io.github.ephym6.plantid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Display and headline styles use the platform serif (Noto Serif on Android) for a
// botanical feel. Titles, body and labels keep the default sans-serif for
// readability. Both are system fonts: no font files, no downloads.
// Sizes follow the Material 3 type scale and are in sp, so they respect the
// user's font-size setting.
private val Headline = FontFamily.Serif
private val Defaults = Typography()

val Typography = Typography(
    displayLarge = Defaults.displayLarge.copy(fontFamily = Headline),
    displayMedium = Defaults.displayMedium.copy(fontFamily = Headline),
    displaySmall = Defaults.displaySmall.copy(fontFamily = Headline),
    headlineLarge = Defaults.headlineLarge.copy(fontFamily = Headline, fontWeight = FontWeight.SemiBold),
    headlineMedium = Defaults.headlineMedium.copy(fontFamily = Headline, fontWeight = FontWeight.SemiBold),
    headlineSmall = Defaults.headlineSmall.copy(fontFamily = Headline, fontWeight = FontWeight.SemiBold),
    titleLarge = Defaults.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Defaults.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)
