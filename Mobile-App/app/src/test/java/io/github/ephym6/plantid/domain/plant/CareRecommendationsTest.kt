package io.github.ephym6.plantid.domain.plant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareRecommendationsTest {

    private val complete = CareData(
        lightLux = 500..3000,
        temperatureCelsius = 15..30,
        humidityPercent = 50,
        wateringIntervalDays = 7,
    )

    @Test
    fun `complete source data gives every tip in a fixed order`() {
        assertEquals(
            listOf(
                CareTip.Light(level = null, lux = 500..3000),
                CareTip.Watering(7),
                CareTip.Temperature(15..30),
                CareTip.Humidity(50),
            ),
            CareRecommendations.tipsFor(complete),
        )
    }

    @Test
    fun `no care data gives no tips`() {
        assertTrue(CareRecommendations.tipsFor(null).isEmpty())
        assertTrue(CareRecommendations.tipsFor(CareData()).isEmpty())
    }

    @Test
    fun `missing values are left out, not filled in`() {
        assertEquals(
            listOf(CareTip.Watering(14)),
            CareRecommendations.tipsFor(CareData(wateringIntervalDays = 14)),
        )
        assertEquals(
            listOf(CareTip.Light(level = null, lux = 2000..6000), CareTip.Humidity(55)),
            CareRecommendations.tipsFor(CareData(lightLux = 2000..6000, humidityPercent = 55)),
        )
    }

    @Test
    fun `implausible values are dropped rather than shown`() {
        val tips = CareRecommendations.tipsFor(
            CareData(
                lightLux = 3000..500, // min greater than max (empty range)
                temperatureCelsius = -100..20,
                humidityPercent = 150,
                wateringIntervalDays = 0,
            ),
        )
        assertTrue("unexpected tips: $tips", tips.isEmpty())

        assertTrue(CareRecommendations.tipsFor(CareData(lightLux = -5..100)).isEmpty())
        assertTrue(CareRecommendations.tipsFor(CareData(humidityPercent = -1)).isEmpty())
        assertTrue(CareRecommendations.tipsFor(CareData(wateringIntervalDays = -3)).isEmpty())
        assertTrue(CareRecommendations.tipsFor(CareData(temperatureCelsius = 30..15)).isEmpty())
    }

    @Test
    fun `single-value ranges and boundary values are kept`() {
        assertEquals(
            listOf(CareTip.Light(level = null, lux = 1000..1000), CareTip.Temperature(20..20), CareTip.Humidity(0)),
            CareRecommendations.tipsFor(
                CareData(lightLux = 1000..1000, temperatureCelsius = 20..20, humidityPercent = 0),
            ),
        )
        assertEquals(listOf(CareTip.Humidity(100)), CareRecommendations.tipsFor(CareData(humidityPercent = 100)))
    }

    @Test
    fun `light keeps the source's plain-language level, with or without lux`() {
        assertEquals(
            listOf(CareTip.Light(level = "Bright Indirect", lux = 500..3000)),
            CareRecommendations.tipsFor(CareData(lightLevel = "Bright Indirect", lightLux = 500..3000)),
        )
        assertEquals(
            listOf(CareTip.Light(level = "Medium", lux = null)),
            CareRecommendations.tipsFor(CareData(lightLevel = " Medium ", lightLux = 3000..500)),
        )
        assertTrue(CareRecommendations.tipsFor(CareData(lightLevel = "  ")).isEmpty())
    }
}
