package io.github.paulsnuff.betternightlight.domain

import io.github.paulsnuff.betternightlight.data.NightLightOverrideRepository
import io.github.paulsnuff.betternightlight.data.NightLightRepository
import io.github.paulsnuff.betternightlight.data.SavedNightLightOverride
import io.github.paulsnuff.betternightlight.domain.model.AutomationSchedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NightLightAutomationManager
    @Inject
    constructor(
        private val nightLightRepository: NightLightRepository,
        private val nightLightController: NightLightController,
        private val nightLightOverrideRepository: NightLightOverrideRepository,
        private val clock: Clock,
    ) {
        private val mutex = Mutex()

        suspend fun applyStrength(schedule: AutomationSchedule) =
            mutex.withLock {
                if (!schedule.enabled) return@withLock
                saveOverrideIfNeeded()
                requireWrite(nightLightController.setAutoMode(NightLightController.AUTO_MODE_DISABLED), "auto_mode")
                val strength = computeStrength(schedule)
                if (strength != null) {
                    requireWrite(nightLightController.setActivated(true), "activated")
                    requireWrite(nightLightController.setColorTemperature(strength), "color temperature")
                } else {
                    requireWrite(nightLightController.setActivated(false), "activated")
                }
            }

        suspend fun restoreAndClearOverride() =
            mutex.withLock {
                val saved = nightLightOverrideRepository.savedNightLightOverrideFlow.first()
                if (saved != null) {
                    requireWrite(nightLightController.setActivated(saved.wasActivated), "activated")
                    requireWrite(
                        nightLightController.setColorTemperature(saved.temperature),
                        "color temperature",
                    )
                    requireWrite(nightLightController.setAutoMode(saved.autoMode), "auto mode")
                    nightLightOverrideRepository.clearNightLightOverride()
                }
            }

        private suspend fun saveOverrideIfNeeded() {
            val saved = nightLightOverrideRepository.savedNightLightOverrideFlow.first()
            if (saved != null) return
            val status = nightLightRepository.getStatus()
            nightLightOverrideRepository.saveNightLightOverride(
                SavedNightLightOverride(
                    wasActivated = status.isActivated,
                    temperature = status.temperature ?: DEFAULT_TEMPERATURE_KELVIN,
                    autoMode = status.autoMode ?: NightLightController.AUTO_MODE_DISABLED,
                ),
            )
        }

        private fun requireWrite(
            succeeded: Boolean,
            setting: String,
        ) {
            if (!succeeded) {
                throw NightLightWriteException("Failed to write night light setting: $setting")
            }
        }

        private fun computeStrength(schedule: AutomationSchedule): Int? {
            val now = LocalTime.now(clock)
            return computeCurrentNightLightStrength(
                schedule = schedule,
                resolvedBoostKelvin = resolvedBoostKelvin(schedule),
                nowMinute = now.hour * 60 + now.minute,
            )
        }
    }
