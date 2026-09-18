package io.github.paulsnuff.betternightlight.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.paulsnuff.betternightlight.MainActivity
import io.github.paulsnuff.betternightlight.R
import io.github.paulsnuff.betternightlight.data.UserPreferencesRepository
import io.github.paulsnuff.betternightlight.domain.AutomationWorkScheduler
import io.github.paulsnuff.betternightlight.domain.NightLightAutomationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Foreground service that periodically (roughly once a minute) recomputes and
 * applies the Night Light strength according to the user's schedule.
 *
 * A foreground service keeps running while the app is in the background and is
 * not throttled by battery optimizations, unlike periodic WorkManager jobs
 * (which have a hard 15-minute minimum and are deferred in Doze).
 */
@AndroidEntryPoint
class NightLightAutomationService : Service() {
    @Inject
    lateinit var automationManager: NightLightAutomationManager

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var workScheduler: AutomationWorkScheduler

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startInForeground()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // WorkManager periodic work is kept as a safety net in case the service
        // is ever killed by the system; the worker applies the same strength.
        workScheduler.schedulePeriodicWork()
        loopJob?.cancel()
        loopJob =
            serviceScope.launch {
                runAutomationLoop()
            }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun runAutomationLoop() {
        while (true) {
            try {
                val schedule = userPreferencesRepository.automationScheduleFlow.first()
                if (!schedule.enabled) {
                    workScheduler.cancelPeriodicWork()
                    stopSelf()
                    return
                }
                automationManager.applyStrength(schedule)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Never let the loop die: log and retry on the next tick.
                Log.w(TAG, "automation tick failed", e)
            }
            delay(REPEAT_INTERVAL)
        }
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "BnlService"
        private const val NOTIFICATION_CHANNEL_ID = "night_light_automation"
        private const val NOTIFICATION_ID = 1

        // As frequent as practical; the tick does not have to be exact.
        private val REPEAT_INTERVAL = 60.seconds
    }
}
