package io.github.ephym6.plantid.domain.plant

/**
 * One piece of care guidance, holding only values that come from the source
 * data. Wording lives in reviewed string templates in the UI layer, so every
 * tip shown can be traced back to a source value (DECISIONS D20).
 */
sealed interface CareTip {
    /** [level] is the source's plain-language category; [lux] its measured range. Either may be missing. */
    data class Light(val level: String?, val lux: IntRange?) : CareTip
    data class Watering(val typicalIntervalDays: Int) : CareTip
    data class Temperature(val celsius: IntRange) : CareTip
    data class Humidity(val percent: Int) : CareTip
}

/**
 * The rule-based care recommender: turns a plant's [CareData] into the care
 * tips to show.
 *
 * Rules:
 * 1. Only values present in the source become tips. Nothing is filled in.
 * 2. Implausible values are dropped rather than shown: negative lux, a range
 *    whose minimum exceeds its maximum, a non-positive watering interval,
 *    humidity outside 0–100 %, temperatures outside -50–60 °C.
 * 3. Fixed order: light, watering, temperature, humidity.
 *
 * Pet safety is shown separately from care tips, from its own sources
 * (DECISIONS D41). No edibility or medical guidance is produced.
 */
object CareRecommendations {

    fun tipsFor(care: CareData?): List<CareTip> {
        if (care == null) return emptyList()
        return listOfNotNull(
            lightTip(care),
            care.wateringIntervalDays?.takeIf { it > 0 }?.let(CareTip::Watering),
            care.temperatureCelsius
                ?.takeIf { it.first <= it.last && it.first >= MIN_CELSIUS && it.last <= MAX_CELSIUS }
                ?.let(CareTip::Temperature),
            care.humidityPercent?.takeIf { it in 0..100 }?.let(CareTip::Humidity),
        )
    }

    private fun lightTip(care: CareData): CareTip.Light? {
        val lux = care.lightLux?.takeIf { it.first >= 0 && it.first <= it.last }
        val level = care.lightLevel?.trim()?.takeIf { it.isNotEmpty() }
        return if (lux == null && level == null) null else CareTip.Light(level, lux)
    }

    private const val MIN_CELSIUS = -50
    private const val MAX_CELSIUS = 60
}
