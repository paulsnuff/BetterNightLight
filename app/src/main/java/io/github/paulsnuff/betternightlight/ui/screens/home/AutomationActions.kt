package io.github.paulsnuff.betternightlight.ui.screens.home

import io.github.paulsnuff.betternightlight.domain.model.AutomationLocationSource
import io.github.paulsnuff.betternightlight.domain.model.AutomationTrigger
import io.github.paulsnuff.betternightlight.domain.model.PhaseTransition

data class AutomationActions(
    val onAutomationEnabledChange: (Boolean) -> Unit = {},
    val onTriggerChange: (AutomationTrigger) -> Unit = {},
    val onManualStartTimeChange: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    val onManualEndTimeChange: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    val onLocationSourceChange: (AutomationLocationSource) -> Unit = {},
    val onCoordinatesChange: (latitude: Double?, longitude: Double?) -> Unit = { _, _ -> },
    val onNightLightTemperatureChange: (Int) -> Unit = {},
    val onRequestDeviceLocation: () -> Unit = {},
    val onLocationPermissionDenied: () -> Unit = {},
    val onBoostEnabledChange: (Boolean) -> Unit = {},
    val onBoostTimeChange: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    val onBoostKelvinChange: (Int) -> Unit = {},
    val onPhaseTransitionChange: (PhaseTransition) -> Unit = {},
    val onOffEnabledChange: (Boolean) -> Unit = {},
    val onOffTimeChange: (hour: Int, minute: Int) -> Unit = { _, _ -> },
)
