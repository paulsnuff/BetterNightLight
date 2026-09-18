package io.github.paulsnuff.betternightlight.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts/stops [NightLightAutomationService] in response to the automation
 * schedule being enabled or disabled.
 */
@Singleton
class AutomationServiceController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun onAutomationEnabled() {
            ContextCompat.startForegroundService(
                context,
                Intent(context, NightLightAutomationService::class.java),
            )
        }

        fun onAutomationDisabled() {
            context.stopService(Intent(context, NightLightAutomationService::class.java))
        }
    }
