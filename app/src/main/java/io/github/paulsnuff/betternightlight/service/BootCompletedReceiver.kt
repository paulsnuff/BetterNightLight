package io.github.paulsnuff.betternightlight.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.paulsnuff.betternightlight.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restarts the Night Light automation foreground service after the device
 * boots (or the app is updated), so the automation survives a system restart
 * even when the user never opens the app again.
 *
 * Only [Intent.ACTION_BOOT_COMPLETED] is handled (not LOCKED_BOOT_COMPLETED):
 * it fires after the user unlock, when credential-encrypted storage (DataStore)
 * is readable.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = userPreferencesRepository.automationScheduleFlow.first().enabled
                if (enabled) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, NightLightAutomationService::class.java),
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("BnlBoot", "Failed to restart automation on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val SUPPORTED_ACTIONS =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
            )
    }
}
