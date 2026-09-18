package io.github.paulsnuff.betternightlight.ui.components.nightlightsupport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.paulsnuff.betternightlight.data.NightLightRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class NightLightSupportUiState(
    val isNightLightSupported: Boolean = false,
)

@HiltViewModel
class NightLightSupportViewModel
    @Inject
    constructor(
        nightLightRepository: NightLightRepository,
    ) : ViewModel() {
        val uiState: StateFlow<NightLightSupportUiState> =
            nightLightRepository.statusFlow
                .map { status -> NightLightSupportUiState(isNightLightSupported = status.isAvailable) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue =
                        NightLightSupportUiState(
                            isNightLightSupported = nightLightRepository.getStatus().isAvailable,
                        ),
                )
    }
