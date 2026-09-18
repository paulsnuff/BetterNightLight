package io.github.paulsnuff.betternightlight.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.paulsnuff.betternightlight.domain.DEFAULT_BOOST_TIME
import io.github.paulsnuff.betternightlight.domain.DEFAULT_MANUAL_END
import io.github.paulsnuff.betternightlight.domain.DEFAULT_MANUAL_START
import io.github.paulsnuff.betternightlight.domain.DEFAULT_OFF_TIME
import io.github.paulsnuff.betternightlight.domain.DEFAULT_SCHEDULE_TEMPERATURE_KELVIN
import io.github.paulsnuff.betternightlight.domain.MINUTES_PER_DAY
import io.github.paulsnuff.betternightlight.domain.model.AutomationLocationSource
import io.github.paulsnuff.betternightlight.domain.model.AutomationSchedule
import io.github.paulsnuff.betternightlight.domain.model.AutomationTrigger
import io.github.paulsnuff.betternightlight.domain.model.PhaseTransition
import io.github.paulsnuff.betternightlight.domain.model.TimeOfDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private inline fun <reified T : Enum<T>> Preferences.readEnum(
    key: Preferences.Key<String>,
    default: T,
): T = this[key]?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

data class UserPreferences(
    val themeMode: AppThemeMode,
    val dynamicColor: Boolean,
    val language: AppLanguage,
)

interface UserPreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>
    val automationScheduleFlow: Flow<AutomationSchedule>

    suspend fun updateThemeMode(themeMode: AppThemeMode)

    suspend fun updateDynamicColor(dynamicColor: Boolean)

    suspend fun updateLanguage(language: AppLanguage)

    suspend fun updateAutomationSchedule(schedule: AutomationSchedule)

    suspend fun isWizardCompleted(): Boolean

    suspend fun setWizardCompleted()
}

private object PreferencesKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val LANGUAGE = stringPreferencesKey("language")
    val WIZARD_COMPLETED = booleanPreferencesKey("wizard_completed")

    val AUTOMATION_ENABLED = booleanPreferencesKey("automation_enabled")
    val AUTOMATION_TRIGGER = stringPreferencesKey("automation_trigger")
    val AUTOMATION_LOCATION_SOURCE = stringPreferencesKey("automation_location_source")
    val AUTOMATION_START_MINUTES = intPreferencesKey("automation_start_minutes")
    val AUTOMATION_END_MINUTES = intPreferencesKey("automation_end_minutes")

    val AUTOMATION_LATITUDE = doublePreferencesKey("automation_latitude")
    val AUTOMATION_LONGITUDE = doublePreferencesKey("automation_longitude")
    val AUTOMATION_TEMPERATURE_KELVIN = intPreferencesKey("automation_temperature_kelvin")
    val AUTOMATION_BOOST_ENABLED = booleanPreferencesKey("automation_boost_enabled")
    val AUTOMATION_BOOST_MINUTES = intPreferencesKey("automation_boost_minutes")
    val AUTOMATION_BOOST_KELVIN = intPreferencesKey("automation_boost_kelvin")
    val AUTOMATION_PHASE_TRANSITION = stringPreferencesKey("automation_phase_transition")
    val AUTOMATION_OFF_ENABLED = booleanPreferencesKey("automation_off_enabled")
    val AUTOMATION_OFF_MINUTES = intPreferencesKey("automation_off_minutes")
}

@Singleton
class UserPreferencesRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : UserPreferencesRepository {
        private val preferencesData: Flow<Preferences> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }

        override val userPreferencesFlow: Flow<UserPreferences> =
            preferencesData
                .map { preferences ->
                    UserPreferences(
                        themeMode = preferences.readEnum(PreferencesKeys.THEME_MODE, AppThemeMode.SYSTEM),
                        dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: false,
                        language = preferences.readEnum(PreferencesKeys.LANGUAGE, AppLanguage.AUTO),
                    )
                }

        override suspend fun updateThemeMode(themeMode: AppThemeMode) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME_MODE] = themeMode.name
            }
        }

        override suspend fun updateDynamicColor(dynamicColor: Boolean) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.DYNAMIC_COLOR] = dynamicColor
            }
        }

        override suspend fun updateLanguage(language: AppLanguage) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.LANGUAGE] = language.name
            }
        }

        override val automationScheduleFlow: Flow<AutomationSchedule> =
            preferencesData.map { preferences -> preferences.toAutomationSchedule() }

        override suspend fun updateAutomationSchedule(schedule: AutomationSchedule) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTOMATION_ENABLED] = schedule.enabled
                preferences[PreferencesKeys.AUTOMATION_TRIGGER] = schedule.trigger.name
                preferences[PreferencesKeys.AUTOMATION_LOCATION_SOURCE] = schedule.locationSource.name
                preferences[PreferencesKeys.AUTOMATION_START_MINUTES] = schedule.manualStartTime.minutes
                preferences[PreferencesKeys.AUTOMATION_END_MINUTES] = schedule.manualEndTime.minutes

                if (schedule.latitude != null) {
                    preferences[PreferencesKeys.AUTOMATION_LATITUDE] = schedule.latitude
                } else {
                    preferences.remove(PreferencesKeys.AUTOMATION_LATITUDE)
                }
                if (schedule.longitude != null) {
                    preferences[PreferencesKeys.AUTOMATION_LONGITUDE] = schedule.longitude
                } else {
                    preferences.remove(PreferencesKeys.AUTOMATION_LONGITUDE)
                }

                preferences[PreferencesKeys.AUTOMATION_TEMPERATURE_KELVIN] = schedule.nightLightTemperatureKelvin
                preferences[PreferencesKeys.AUTOMATION_BOOST_ENABLED] = schedule.boostEnabled
                preferences[PreferencesKeys.AUTOMATION_BOOST_MINUTES] = schedule.boostTime.minutes

                if (schedule.boostKelvin != null) {
                    preferences[PreferencesKeys.AUTOMATION_BOOST_KELVIN] = schedule.boostKelvin
                } else {
                    preferences.remove(PreferencesKeys.AUTOMATION_BOOST_KELVIN)
                }

                preferences[PreferencesKeys.AUTOMATION_PHASE_TRANSITION] = schedule.phaseTransition.name
                preferences[PreferencesKeys.AUTOMATION_OFF_ENABLED] = schedule.offEnabled
                preferences[PreferencesKeys.AUTOMATION_OFF_MINUTES] = schedule.offTime.minutes
            }
        }

        override suspend fun isWizardCompleted(): Boolean =
            try {
                dataStore.data.first()[PreferencesKeys.WIZARD_COMPLETED] ?: false
            } catch (_: IOException) {
                false
            }

        override suspend fun setWizardCompleted() {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.WIZARD_COMPLETED] = true
            }
        }
    }

private fun readTimeOfDay(
    preferences: Preferences,
    minutesKey: Preferences.Key<Int>,
    default: TimeOfDay,
): TimeOfDay {
    val minutes = preferences[minutesKey]
    return if (minutes != null && minutes in 0 until MINUTES_PER_DAY) {
        TimeOfDay(minutes)
    } else {
        default
    }
}

private fun Preferences.toAutomationSchedule(): AutomationSchedule {
    val trigger = readEnum(PreferencesKeys.AUTOMATION_TRIGGER, AutomationTrigger.MANUAL)
    val locationSource = readEnum(PreferencesKeys.AUTOMATION_LOCATION_SOURCE, AutomationLocationSource.MANUAL)
    val transition = readEnum(PreferencesKeys.AUTOMATION_PHASE_TRANSITION, PhaseTransition.GRADUAL)

    return AutomationSchedule(
        enabled = this[PreferencesKeys.AUTOMATION_ENABLED] ?: false,
        trigger = trigger,
        locationSource = locationSource,
        manualStartTime =
            readTimeOfDay(
                this,
                PreferencesKeys.AUTOMATION_START_MINUTES,
                default = DEFAULT_MANUAL_START,
            ),
        manualEndTime =
            readTimeOfDay(
                this,
                PreferencesKeys.AUTOMATION_END_MINUTES,
                default = DEFAULT_MANUAL_END,
            ),
        latitude = this[PreferencesKeys.AUTOMATION_LATITUDE],
        longitude = this[PreferencesKeys.AUTOMATION_LONGITUDE],
        nightLightTemperatureKelvin = this[PreferencesKeys.AUTOMATION_TEMPERATURE_KELVIN] ?: DEFAULT_SCHEDULE_TEMPERATURE_KELVIN,
        boostEnabled = this[PreferencesKeys.AUTOMATION_BOOST_ENABLED] ?: false,
        boostTime =
            readTimeOfDay(
                this,
                PreferencesKeys.AUTOMATION_BOOST_MINUTES,
                default = DEFAULT_BOOST_TIME,
            ),
        boostKelvin = this[PreferencesKeys.AUTOMATION_BOOST_KELVIN],
        phaseTransition = transition,
        offEnabled = this[PreferencesKeys.AUTOMATION_OFF_ENABLED] ?: false,
        offTime =
            readTimeOfDay(
                this,
                PreferencesKeys.AUTOMATION_OFF_MINUTES,
                default = DEFAULT_OFF_TIME,
            ),
    )
}
