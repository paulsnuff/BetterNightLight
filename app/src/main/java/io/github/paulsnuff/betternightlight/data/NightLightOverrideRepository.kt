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

    /** True while the user's manual Night Light off is respected in the current automation cycle. */
    val userDisabledNightLightFlow: Flow<Boolean>

    /** Last activated value written by the automation itself; used to detect manual user changes. */
    val lastAutomationActivatedFlow: Flow<Boolean>

    suspend fun saveNightLightOverride(override: SavedNightLightOverride)

    suspend fun setSavedOverrideActivated(activated: Boolean)

    suspend fun clearNightLightOverride()

    suspend fun setUserDisabledNightLight(disabled: Boolean)

    suspend fun clearUserDisabledNightLight()

    suspend fun setLastAutomationActivated(activated: Boolean)
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
            val USER_DISABLED = booleanPreferencesKey("automation_user_disabled_night_light")
            val LAST_AUTOMATION_ACTIVATED = booleanPreferencesKey("automation_last_activated")
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

        override val userDisabledNightLightFlow: Flow<Boolean> =
            preferencesFlow.map { preferences -> preferences[Keys.USER_DISABLED] ?: false }

        override val lastAutomationActivatedFlow: Flow<Boolean> =
            preferencesFlow.map { preferences -> preferences[Keys.LAST_AUTOMATION_ACTIVATED] ?: false }

        override suspend fun saveNightLightOverride(override: SavedNightLightOverride) {
            dataStore.edit { preferences ->
                preferences[Keys.SAVED_ACTIVATED] = override.wasActivated
                preferences[Keys.SAVED_TEMPERATURE] = override.temperature
                preferences[Keys.SAVED_AUTO_MODE] = override.autoMode
            }
        }

        override suspend fun setSavedOverrideActivated(activated: Boolean) {
            dataStore.edit { preferences ->
                preferences[Keys.SAVED_ACTIVATED] = activated
            }
        }

        override suspend fun clearNightLightOverride() {
            dataStore.edit { preferences ->
                preferences.remove(Keys.SAVED_ACTIVATED)
                preferences.remove(Keys.SAVED_TEMPERATURE)
                preferences.remove(Keys.SAVED_AUTO_MODE)
            }
        }

        override suspend fun setUserDisabledNightLight(disabled: Boolean) {
            dataStore.edit { preferences ->
                preferences[Keys.USER_DISABLED] = disabled
            }
        }

        override suspend fun clearUserDisabledNightLight() {
            dataStore.edit { preferences ->
                preferences.remove(Keys.USER_DISABLED)
            }
        }

        override suspend fun setLastAutomationActivated(activated: Boolean) {
            dataStore.edit { preferences ->
                preferences[Keys.LAST_AUTOMATION_ACTIVATED] = activated
            }
        }
    }
