package io.github.paulsnuff.betternightlight.domain.model

import io.github.paulsnuff.betternightlight.domain.DEFAULT_BOOST_TIME
import io.github.paulsnuff.betternightlight.domain.DEFAULT_MANUAL_END
import io.github.paulsnuff.betternightlight.domain.DEFAULT_MANUAL_START
import io.github.paulsnuff.betternightlight.domain.DEFAULT_OFF_TIME
import io.github.paulsnuff.betternightlight.domain.DEFAULT_SCHEDULE_TEMPERATURE_KELVIN

enum class AutomationTrigger {
    MANUAL,
    LOCATION,
}

enum class AutomationLocationSource {
    DEVICE,
    MANUAL,
}

enum class PhaseTransition {
    INSTANT,
    GRADUAL,
}

data class AutomationSchedule(
    val enabled: Boolean = false,
    val trigger: AutomationTrigger = AutomationTrigger.MANUAL,
    val locationSource: AutomationLocationSource = AutomationLocationSource.MANUAL,
    val manualStartTime: TimeOfDay = DEFAULT_MANUAL_START,
    val manualEndTime: TimeOfDay = DEFAULT_MANUAL_END,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val nightLightTemperatureKelvin: Int = DEFAULT_SCHEDULE_TEMPERATURE_KELVIN,
    val boostEnabled: Boolean = false,
    val boostTime: TimeOfDay = DEFAULT_BOOST_TIME,
    val boostKelvin: Int? = null,
    val phaseTransition: PhaseTransition = PhaseTransition.GRADUAL,
    val offEnabled: Boolean = false,
    val offTime: TimeOfDay = DEFAULT_OFF_TIME,
)
