package io.github.threefreetree.betternightlight.domain

interface NightLightController {
    suspend fun setActivated(activated: Boolean): Boolean

    suspend fun setColorTemperature(kelvin: Int): Boolean

    suspend fun setAutoMode(mode: Int): Boolean

    companion object {
        const val AUTO_MODE_DISABLED = 0
        const val AUTO_MODE_CUSTOM = 1
        const val AUTO_MODE_TWILIGHT = 2
    }
}

class NightLightWriteException(
    message: String,
) : Exception(message)
