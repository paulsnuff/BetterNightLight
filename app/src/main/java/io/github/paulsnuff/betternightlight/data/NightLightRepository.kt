package io.github.paulsnuff.betternightlight.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

interface NightLightRepository {
    val statusFlow: StateFlow<NightLightStatus>

    fun getStatus(): NightLightStatus
}

@Singleton
class NightLightRepositoryImpl
    @Inject
    constructor(
        private val reader: NightLightReader,
        applicationScope: CoroutineScope,
    ) : NightLightRepository {
        override val statusFlow: StateFlow<NightLightStatus> =
            reader
                .observeStatus()
                .stateIn(
                    scope = applicationScope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                    initialValue = NightLightStatus.UNKNOWN,
                )

        override fun getStatus(): NightLightStatus = reader.getStatus()
    }
