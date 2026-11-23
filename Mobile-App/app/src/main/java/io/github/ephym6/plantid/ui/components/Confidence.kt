package io.github.ephym6.plantid.ui.components

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Formats a calibrated model probability as a whole percentage (DECISIONS D43).
 * Never shows "100%" or "0%", which would suggest certainty: anything that rounds
 * to 100 shows as ">99%" and anything that rounds to 0 as "<1%".
 *
 * Rounds to a whole percent first, then caps, so float imprecision can't slip a
 * value past the caps (0.005f is stored as 0.00499...).
 */
fun formatConfidence(score: Float, locale: Locale = Locale.getDefault()): String {
    if (score.isNaN()) return "–"
    val percent = NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 0 }
    val whole = (score * 100f).roundToInt()
    return when {
        whole >= 100 -> ">" + percent.format(0.99)
        whole <= 0 -> "<" + percent.format(0.01)
        else -> percent.format(whole / 100.0)
    }
}
