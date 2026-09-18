package io.github.paulsnuff.betternightlight.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.paulsnuff.betternightlight.data.UserPreferencesRepository
import io.github.paulsnuff.betternightlight.domain.NightLightAutomationManager
import io.github.paulsnuff.betternightlight.domain.NightLightWriteException
import kotlinx.coroutines.flow.first

@HiltWorker
class NightLightAutomationWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val automationManager: NightLightAutomationManager,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            val schedule = userPreferencesRepository.automationScheduleFlow.first()
            if (!schedule.enabled) {
                WorkManager.getInstance(applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
                return Result.success()
            }
            return try {
                automationManager.applyStrength(schedule)
                Result.success()
            } catch (e: NightLightWriteException) {
                Log.w(TAG, "applyStrength failed, scheduling retry", e)
                Result.retry()
            }
        }

        companion object {
            private const val TAG = "BnlWorker"
            const val UNIQUE_WORK_NAME = "night_light_automation"
        }
    }
