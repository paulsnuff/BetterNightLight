package io.github.paulsnuff.betternightlight.data

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.paulsnuff.betternightlight.domain.DispatcherProvider
import io.github.paulsnuff.betternightlight.domain.NightLightController
import io.github.paulsnuff.betternightlight.domain.TEMPERATURE_MAX_KELVIN
import io.github.paulsnuff.betternightlight.domain.TEMPERATURE_MIN_KELVIN
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsSecureNightLightController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val permissionGate: PermissionGate,
        private val dispatchers: DispatcherProvider,
    ) : NightLightController {
        private val appContext = context.applicationContext

        override suspend fun setActivated(activated: Boolean): Boolean = writeSetting("night_display_activated", if (activated) 1 else 0)

        override suspend fun setColorTemperature(kelvin: Int): Boolean =
            writeSetting(
                "night_display_color_temperature",
                kelvin.coerceIn(TEMPERATURE_MIN_KELVIN, TEMPERATURE_MAX_KELVIN),
            )

        override suspend fun setAutoMode(mode: Int): Boolean = writeSetting("night_display_auto_mode", mode)

        private suspend fun writeSetting(
            name: String,
            value: Int,
        ): Boolean {
            if (!permissionGate.ensurePermission()) {
                return false
            }

            return withContext(dispatchers.io) {
                runCatching { Settings.Secure.putInt(appContext.contentResolver, name, value) }
                    .getOrDefault(false)
            }
        }
    }
