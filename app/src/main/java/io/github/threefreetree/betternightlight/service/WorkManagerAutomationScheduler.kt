package io.github.threefreetree.betternightlight.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.threefreetree.betternightlight.domain.AutomationWorkScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerAutomationScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AutomationWorkScheduler {
        override fun schedulePeriodicWork() {
            val request =
                PeriodicWorkRequestBuilder<NightLightAutomationWorker>(
                    REPEAT_INTERVAL_MINUTES,
                    TimeUnit.MINUTES,
                ).build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    NightLightAutomationWorker.UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
        }

        override fun cancelPeriodicWork() {
            WorkManager
                .getInstance(context)
                .cancelUniqueWork(NightLightAutomationWorker.UNIQUE_WORK_NAME)
        }

        companion object {
            // WorkManager's minimum periodic interval; shorter periods are clamped
            // to this value by the library anyway.
            private const val REPEAT_INTERVAL_MINUTES = 15L
        }
    }
