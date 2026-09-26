package io.github.paulsnuff.betternightlight.domain

import io.github.paulsnuff.betternightlight.data.NightLightOverrideRepository
import io.github.paulsnuff.betternightlight.data.NightLightRepository
import io.github.paulsnuff.betternightlight.data.SavedNightLightOverride
import io.github.paulsnuff.betternightlight.data.UserPreferencesRepository
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
        private val userPreferencesRepository: UserPreferencesRepository,
        private val clock: Clock,
    ) {
        private val mutex = Mutex()

        suspend fun applyStrength(schedule: AutomationSchedule) =
            mutex.withLock {
                if (!schedule.enabled) {
                    clearUserDisabledIfSet()
                    return@withLock
                }
                // The caller may hold a stale snapshot read before the schedule
                // was disabled; re-check the persisted state under the mutex so
                // a disable is never overwritten by an in-flight tick.
                if (!userPreferencesRepository.automationScheduleFlow.first().enabled) {
                    return@withLock
                }
                saveOverrideIfNeeded()
                val strength = computeStrength(schedule)
                if (strength == null) {
                    clearUserDisabledIfSet()
                    clearLastAutomationActivatedIfSet()
                    requireWrite(nightLightController.setAutoMode(NightLightController.AUTO_MODE_DISABLED), "auto_mode")
                    requireWrite(nightLightController.setActivated(false), "activated")
                    return@withLock
                }
                val status = nightLightRepository.getStatus()
                val userDisabled = nightLightOverrideRepository.userDisabledNightLightFlow.first()
                val lastAutomationActivated = nightLightOverrideRepository.lastAutomationActivatedFlow.first()
                if (userDisabled) {
                    if (status.isActivated) {
                        nightLightOverrideRepository.clearUserDisabledNightLight()
                    } else {
                        return@withLock
                    }
                } else if (lastAutomationActivated && !status.isActivated) {
                    nightLightOverrideRepository.setUserDisabledNightLight(true)
                    nightLightOverrideRepository.setSavedOverrideActivated(false)
                    return@withLock
                }
                requireWrite(nightLightController.setAutoMode(NightLightController.AUTO_MODE_DISABLED), "auto_mode")
                requireWrite(nightLightController.setActivated(true), "activated")
                requireWrite(nightLightController.setColorTemperature(strength), "color temperature")
                if (!lastAutomationActivated) {
                    nightLightOverrideRepository.setLastAutomationActivated(true)
                }
            }

        suspend fun restoreAndClearOverride() =
            mutex.withLock {
                nightLightOverrideRepository.clearUserDisabledNightLight()
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
                nightLightOverrideRepository.setLastAutomationActivated(saved?.wasActivated ?: false)
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

        private suspend fun clearUserDisabledIfSet() {
            if (nightLightOverrideRepository.userDisabledNightLightFlow.first()) {
                nightLightOverrideRepository.clearUserDisabledNightLight()
            }
        }

        private suspend fun clearLastAutomationActivatedIfSet() {
            if (nightLightOverrideRepository.lastAutomationActivatedFlow.first()) {
                nightLightOverrideRepository.setLastAutomationActivated(false)
            }
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
