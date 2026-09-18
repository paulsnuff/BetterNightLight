package io.github.paulsnuff.betternightlight.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class SavedNightLightOverride(
    val wasActivated: Boolean,
    val temperature: Int,
    val autoMode: Int,
)

interface NightLightOverrideRepository {
    val savedNightLightOverrideFlow: Flow<SavedNightLightOverride?>

    suspend fun saveNightLightOverride(override: SavedNightLightOverride)

    suspend fun clearNightLightOverride()
}

@Singleton
class NightLightOverrideRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : NightLightOverrideRepository {
        private object Keys {
            val SAVED_ACTIVATED = booleanPreferencesKey("night_light_override_saved_activated")
            val SAVED_TEMPERATURE = intPreferencesKey("night_light_override_saved_temperature")
            val SAVED_AUTO_MODE = intPreferencesKey("night_light_override_saved_auto_mode")
        }

        private val preferencesFlow: Flow<Preferences> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }

        override val savedNightLightOverrideFlow: Flow<SavedNightLightOverride?> =
            preferencesFlow
                .map { preferences ->
                    val wasActivated = preferences[Keys.SAVED_ACTIVATED] ?: return@map null
                    val temperature = preferences[Keys.SAVED_TEMPERATURE] ?: return@map null
                    val autoMode = preferences[Keys.SAVED_AUTO_MODE] ?: return@map null
                    SavedNightLightOverride(
                        wasActivated = wasActivated,
                        temperature = temperature,
                        autoMode = autoMode,
                    )
                }

        override suspend fun saveNightLightOverride(override: SavedNightLightOverride) {
            dataStore.edit { preferences ->
                preferences[Keys.SAVED_ACTIVATED] = override.wasActivated
                preferences[Keys.SAVED_TEMPERATURE] = override.temperature
                preferences[Keys.SAVED_AUTO_MODE] = override.autoMode
            }
        }

        override suspend fun clearNightLightOverride() {
            dataStore.edit { preferences ->
                preferences.remove(Keys.SAVED_ACTIVATED)
                preferences.remove(Keys.SAVED_TEMPERATURE)
                preferences.remove(Keys.SAVED_AUTO_MODE)
            }
        }
    }
