package io.github.paulsnuff.betternightlight.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.paulsnuff.betternightlight.data.NightLightRepository
import io.github.paulsnuff.betternightlight.data.PermissionGate
import io.github.paulsnuff.betternightlight.data.UserPreferencesRepository
import io.github.paulsnuff.betternightlight.domain.AutomationScheduleApplier
import io.github.paulsnuff.betternightlight.domain.LATITUDE_MAX
import io.github.paulsnuff.betternightlight.domain.LATITUDE_MIN
import io.github.paulsnuff.betternightlight.domain.LONGITUDE_MAX
import io.github.paulsnuff.betternightlight.domain.LONGITUDE_MIN
import io.github.paulsnuff.betternightlight.domain.NightLightAutomationManager
import io.github.paulsnuff.betternightlight.domain.NightLightWriteException
import io.github.paulsnuff.betternightlight.domain.computeCurrentNightLightStrength
import io.github.paulsnuff.betternightlight.domain.model.AutomationLocationSource
import io.github.paulsnuff.betternightlight.domain.model.AutomationSchedule
import io.github.paulsnuff.betternightlight.domain.model.AutomationTrigger
import io.github.paulsnuff.betternightlight.domain.model.PhaseTransition
import io.github.paulsnuff.betternightlight.domain.model.TimeOfDay
import io.github.paulsnuff.betternightlight.domain.resolvedBoostKelvin
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

data class HomeUiState(
    val isNightLightSupported: Boolean = false,
    val hasSecureSettingsPermission: Boolean = false,
    val isGrantingPermission: Boolean = false,
    val isRootAvailable: Boolean? = null,
    val compatibleHarnessLabels: List<String> = emptyList(),
    val isFetchingDeviceLocation: Boolean = false,
    val currentStep: HomeWizardStep? = null,
    val automationSchedule: AutomationSchedule = AutomationSchedule(),
    val currentKelvin: Int? = null,
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val nightLightRepository: NightLightRepository,
        private val nightLightAutomationManager: NightLightAutomationManager,
        private val scheduleApplier: AutomationScheduleApplier,
        private val permissionCoordinator: PermissionRequestCoordinator,
        private val locationFetcher: DeviceLocationFetcher,
        private val permissionGate: PermissionGate,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val wizard = HomeWizardStateMachine()

        private val _compatibleHarnessLabels = MutableStateFlow<List<String>>(emptyList())
        private val _automationSchedule = MutableStateFlow(AutomationSchedule())
        private val _currentMinute = MutableStateFlow(currentMinuteOfDay())
        private val _messages = Channel<HomeMessage>(Channel.BUFFERED)
        private var locationFetchJob: Job? = null

        init {
            viewModelScope.launch {
                val wizardCompleted = userPreferencesRepository.isWizardCompleted()
                wizard.initialize(
                    wizardCompleted = wizardCompleted,
                    isSupported = nightLightRepository.getStatus().isAvailable,
                    hasPermission = permissionGate.hasWriteSecureSettingsPermission(),
                )
            }

            viewModelScope.launch {
                permissionGate.permissionMissing.collect {
                    wizard.onPermissionMissing(
                        isSupported = nightLightRepository.getStatus().isAvailable,
                    )
                }
            }

            viewModelScope.launch {
                userPreferencesRepository.automationScheduleFlow
                    .distinctUntilChanged()
                    .collect { schedule ->
                        _automationSchedule.value = schedule
                        applyStrengthQuietly { scheduleApplier.onScheduleChanged(schedule) }
                    }
            }

            viewModelScope.launch {
                while (true) {
                    delay(60.seconds)
                    _currentMinute.value = currentMinuteOfDay()
                    applyStrengthQuietly { nightLightAutomationManager.applyStrength(_automationSchedule.value) }
                }
            }
        }

        private suspend fun applyStrengthQuietly(block: suspend () -> Unit) {
            try {
                block()
            } catch (e: NightLightWriteException) {
                Log.w(TAG, "automation write failed", e)
            }
        }

        val uiState: StateFlow<HomeUiState> =
            combine(
                combine(
                    nightLightRepository.statusFlow,
                    permissionGate.hasPermissionFlow,
                    permissionCoordinator.isRootAvailable,
                ) { status, hasPermission, rootAvailable ->
                    Triple(status, hasPermission, rootAvailable)
                },
                combine(
                    permissionCoordinator.isGrantingPermission,
                    locationFetcher.isFetching,
                    _compatibleHarnessLabels,
                ) { isGranting, isFetchingLocation, harnessLabels ->
                    Triple(isGranting, isFetchingLocation, harnessLabels)
                },
                wizard.currentStep,
                _automationSchedule,
                _currentMinute,
            ) {
                (status, hasPermission, rootAvailable),
                (isGranting, isFetchingLocation, harnessLabels),
                step,
                schedule,
                currentMinute,
                ->
                HomeUiState(
                    isNightLightSupported = status.isAvailable,
                    hasSecureSettingsPermission = hasPermission,
                    isRootAvailable = rootAvailable,
                    isGrantingPermission = isGranting,
                    isFetchingDeviceLocation = isFetchingLocation,
                    compatibleHarnessLabels = harnessLabels,
                    currentStep = step,
                    automationSchedule = schedule,
                    currentKelvin =
                        computeCurrentNightLightStrength(
                            schedule = schedule,
                            resolvedBoostKelvin = resolvedBoostKelvin(schedule),
                            nowMinute = currentMinute,
                        ),
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue =
                    HomeUiState(
                        isNightLightSupported = nightLightRepository.getStatus().isAvailable,
                        hasSecureSettingsPermission = permissionGate.hasPermissionFlow.value,
                        currentStep = wizard.currentStep.value,
                        automationSchedule = _automationSchedule.value,
                    ),
            )

        private fun updateAutomationSchedule(
            transform: (AutomationSchedule) -> AutomationSchedule,
        ) {
            // Single source of truth: only mutate the persisted schedule. The
            // automationScheduleFlow collector applies the strength and manages
            // work scheduling, so the write is applied exactly once.
            val updated = transform(_automationSchedule.value)
            _automationSchedule.value = updated
            viewModelScope.launch {
                userPreferencesRepository.updateAutomationSchedule(updated)
            }
        }

        fun setAutomationEnabled(enabled: Boolean) {
            if (!enabled) {
                viewModelScope.launch {
                    try {
                        nightLightAutomationManager.restoreAndClearOverride()
                    } catch (e: NightLightWriteException) {
                        Log.w(TAG, "restoreAndClearOverride failed", e)
                    }
                }
            }
            updateAutomationSchedule { it.copy(enabled = enabled) }
        }

        fun setAutomationTrigger(trigger: AutomationTrigger) {
            updateAutomationSchedule { it.copy(trigger = trigger) }
        }

        fun setManualStartTime(
            hour: Int,
            minute: Int,
        ) {
            updateAutomationSchedule { it.copy(manualStartTime = TimeOfDay.of(hour, minute)) }
        }

        fun setManualEndTime(
            hour: Int,
            minute: Int,
        ) {
            updateAutomationSchedule { it.copy(manualEndTime = TimeOfDay.of(hour, minute)) }
        }

        fun setCoordinates(
            latitude: Double?,
            longitude: Double?,
        ) {
            updateAutomationSchedule {
                it.copy(
                    latitude = clampCoordinate(latitude, LATITUDE_MIN, LATITUDE_MAX),
                    longitude = clampCoordinate(longitude, LONGITUDE_MIN, LONGITUDE_MAX),
                )
            }
        }

        fun setLocationSource(source: AutomationLocationSource) {
            if (source != AutomationLocationSource.DEVICE) {
                locationFetchJob?.cancel()
                locationFetchJob = null
            }
            updateAutomationSchedule {
                it.copy(locationSource = source)
            }
        }

        fun setNightLightTemperature(kelvin: Int) {
            updateAutomationSchedule {
                it.copy(nightLightTemperatureKelvin = kelvin)
            }
        }

        fun setBoostEnabled(enabled: Boolean) {
            updateAutomationSchedule { it.copy(boostEnabled = enabled) }
        }

        fun setBoostTime(
            hour: Int,
            minute: Int,
        ) {
            updateAutomationSchedule { it.copy(boostTime = TimeOfDay.of(hour, minute)) }
        }

        fun setBoostKelvin(kelvin: Int) {
            updateAutomationSchedule { it.copy(boostKelvin = kelvin) }
        }

        fun setPhaseTransition(transition: PhaseTransition) {
            updateAutomationSchedule { it.copy(phaseTransition = transition) }
        }

        fun setOffEnabled(enabled: Boolean) {
            updateAutomationSchedule { it.copy(offEnabled = enabled) }
        }

        fun setOffTime(
            hour: Int,
            minute: Int,
        ) {
            updateAutomationSchedule { it.copy(offTime = TimeOfDay.of(hour, minute)) }
        }

        fun fetchDeviceLocation() {
            locationFetchJob?.cancel()
            locationFetchJob =
                viewModelScope.launch {
                    locationFetcher.fetch(
                        onResult = { latitude, longitude -> setCoordinates(latitude, longitude) },
                        onUnavailable = { _messages.send(HomeMessage.LocationUnavailable) },
                    )
                }
        }

        fun onLocationPermissionDenied() {
            viewModelScope.launch {
                _messages.send(HomeMessage.LocationPermissionDenied)
            }
        }

        fun goToNextStep() {
            if (wizard.next() == HomeWizardStep.AUTOMATION) {
                viewModelScope.launch {
                    userPreferencesRepository.setWizardCompleted()
                }
            }
        }

        val messages: Flow<HomeMessage> = _messages.receiveAsFlow()

        fun refreshPermission() {
            permissionGate.updatePermissionState()
            permissionCoordinator.refreshRootAvailability()
            _compatibleHarnessLabels.value = permissionCoordinator.installedHarnessLabels()
        }

        fun requestShizukuPermission() {
            viewModelScope.launch {
                permissionCoordinator.requestShizukuPermission { _messages.send(it) }
            }
        }

        fun requestRootPermission() {
            viewModelScope.launch {
                permissionCoordinator.requestRootPermission { _messages.send(it) }
            }
        }

        private fun clampCoordinate(
            value: Double?,
            min: Double,
            max: Double,
        ): Double? {
            if (value == null) return null
            val clamped = value.coerceIn(min, max)
            return (clamped * 100.0).roundToInt() / 100.0
        }

        private fun currentMinuteOfDay(): Int {
            val now = LocalTime.now(clock)
            return now.hour * 60 + now.minute
        }

        companion object {
            private const val TAG = "BnlHomeVm"
        }
    }
