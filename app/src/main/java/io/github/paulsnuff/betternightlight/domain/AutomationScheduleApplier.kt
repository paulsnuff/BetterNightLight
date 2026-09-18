package io.github.paulsnuff.betternightlight.domain

import io.github.paulsnuff.betternightlight.domain.model.AutomationSchedule
import io.github.paulsnuff.betternightlight.service.AutomationServiceController
import javax.inject.Inject

class AutomationScheduleApplier
    @Inject
    constructor(
        private val automationManager: NightLightAutomationManager,
        private val serviceController: AutomationServiceController,
    ) {
        private var lastSeenEnabled = false

        suspend fun onScheduleChanged(schedule: AutomationSchedule) {
            when {
                schedule.enabled && !lastSeenEnabled -> serviceController.onAutomationEnabled()
                !schedule.enabled && lastSeenEnabled -> serviceController.onAutomationDisabled()
            }
            lastSeenEnabled = schedule.enabled
            automationManager.applyStrength(schedule)
        }
    }
