package io.github.ephym6.plantid.ui.care

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ephym6.plantid.domain.plant.CareRecommendations
import io.github.ephym6.plantid.domain.plant.CareTip
import io.github.ephym6.plantid.domain.plant.Plant
import io.github.ephym6.plantid.domain.plant.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Loads the care guide for one plant id. */
class CareGuideViewModel(
    private val plantId: String,
    private val repository: PlantRepository,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Ready(val plant: Plant, val tips: List<CareTip>) : UiState

        /** The plant isn't in the catalog, or the source has no usable care values for it. */
        data object NoCareGuide : UiState

        /** The bundled data couldn't be read. */
        data object Unavailable : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        if (_uiState.value == UiState.Unavailable) load()
    }

    private fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.catalog().fold(
                onSuccess = { catalog ->
                    val plant = catalog.byId(plantId)
                    val tips = CareRecommendations.tipsFor(plant?.care)
                    if (plant == null || tips.isEmpty()) {
                        UiState.NoCareGuide
                    } else {
                        UiState.Ready(plant, tips)
                    }
                },
                onFailure = { UiState.Unavailable },
            )
        }
    }

    companion object {
        fun factory(plantId: String, repository: PlantRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { CareGuideViewModel(plantId, repository) } }
    }
}
