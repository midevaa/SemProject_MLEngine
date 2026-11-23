package io.github.ephym6.plantid.ui.care

import io.github.ephym6.plantid.domain.plant.CareData
import io.github.ephym6.plantid.domain.plant.CareTip
import io.github.ephym6.plantid.domain.plant.Plant
import io.github.ephym6.plantid.domain.plant.PlantCatalog
import io.github.ephym6.plantid.domain.plant.PlantRepository
import io.github.ephym6.plantid.ui.care.CareGuideViewModel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CareGuideViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val withCare = Plant("a", "A", "Plant A", "Species a", null, CareData(wateringIntervalDays = 7))
    private val withoutCare = Plant("b", "B", "Plant B", null, null, care = null)
    private val catalog = PlantCatalog(listOf(withCare, withoutCare))

    private class FakeRepository(var result: Result<PlantCatalog>) : PlantRepository {
        override suspend fun catalog() = result
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `known plant with care values shows its tips and source`() = runTest(dispatcher) {
        val viewModel = CareGuideViewModel("a", FakeRepository(Result.success(catalog)))
        assertEquals(UiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(UiState.Ready(withCare, listOf(CareTip.Watering(7))), viewModel.uiState.value)
    }

    @Test
    fun `plant without care values has no care guide`() = runTest(dispatcher) {
        val viewModel = CareGuideViewModel("b", FakeRepository(Result.success(catalog)))
        advanceUntilIdle()

        assertEquals(UiState.NoCareGuide, viewModel.uiState.value)
    }

    @Test
    fun `unknown plant has no care guide and nothing is invented`() = runTest(dispatcher) {
        val viewModel = CareGuideViewModel("demo_plant_b", FakeRepository(Result.success(catalog)))
        advanceUntilIdle()

        assertEquals(UiState.NoCareGuide, viewModel.uiState.value)
    }

    @Test
    fun `unreadable data can be retried`() = runTest(dispatcher) {
        val repository = FakeRepository(Result.failure(IOException("broken asset")))
        val viewModel = CareGuideViewModel("a", repository)
        advanceUntilIdle()
        assertEquals(UiState.Unavailable, viewModel.uiState.value)

        repository.result = Result.success(catalog)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(UiState.Ready(withCare, listOf(CareTip.Watering(7))), viewModel.uiState.value)
    }
}
