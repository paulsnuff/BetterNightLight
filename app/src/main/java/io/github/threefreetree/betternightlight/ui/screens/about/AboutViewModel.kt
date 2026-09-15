package io.github.threefreetree.betternightlight.ui.screens.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.threefreetree.betternightlight.data.NightLightRepository
import io.github.threefreetree.betternightlight.data.NightLightStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AboutUiState(
    val status: NightLightStatus =
        NightLightStatus(
            isAvailable = false,
            isActivated = false,
            temperature = null,
            autoMode = null,
        ),
)

@HiltViewModel
class AboutViewModel
    @Inject
    constructor(
        nightLightRepository: NightLightRepository,
    ) : ViewModel() {
        val uiState: StateFlow<AboutUiState> =
            nightLightRepository.statusFlow
                .map { status -> AboutUiState(status = status) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = AboutUiState(status = nightLightRepository.getStatus()),
                )
    }
