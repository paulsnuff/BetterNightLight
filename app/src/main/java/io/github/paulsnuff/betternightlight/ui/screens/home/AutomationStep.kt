package io.github.paulsnuff.betternightlight.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.paulsnuff.betternightlight.R
import io.github.paulsnuff.betternightlight.domain.model.AutomationSchedule
import io.github.paulsnuff.betternightlight.domain.model.TimeOfDay
import io.github.paulsnuff.betternightlight.ui.components.IconBubble
import io.github.paulsnuff.betternightlight.ui.components.clearFocusOnTapOutside
import io.github.paulsnuff.betternightlight.ui.components.rememberTrackedBounds
import io.github.paulsnuff.betternightlight.ui.screens.home.automation.AutomationScheduleSection
import io.github.paulsnuff.betternightlight.ui.screens.home.automation.AutomationToggleCard
import io.github.paulsnuff.betternightlight.ui.screens.home.automation.NightLightTemperatureSection
import io.github.paulsnuff.betternightlight.ui.screens.home.automation.NightPhasesSection
import io.github.paulsnuff.betternightlight.ui.screens.home.automation.TimePickerDialogHost
import io.github.paulsnuff.betternightlight.ui.theme.BetterNightLightTheme

internal enum class TimeTarget { START, END }

@Composable
fun AutomationStep(
    schedule: AutomationSchedule,
    currentKelvin: Int?,
    contentBottomPadding: Dp,
    actions: AutomationActions,
    isFetchingLocation: Boolean = false,
    initialScheduleExpanded: Boolean = true,
    initialNightPhasesExpanded: Boolean = true,
) {
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }
    val context = LocalContext.current
    val coordinateFieldBounds = rememberTrackedBounds()
    val temperatureFieldBounds = rememberTrackedBounds()
    val boostTemperatureFieldBounds = rememberTrackedBounds()

    var scheduleExpanded by rememberSaveable { mutableStateOf(initialScheduleExpanded) }
    var nightPhasesExpanded by rememberSaveable { mutableStateOf(initialNightPhasesExpanded) }

    val scrollState = rememberScrollState()

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                actions.onRequestDeviceLocation()
            } else {
                actions.onLocationPermissionDenied()
            }
        }

    val requestLocationPermission: () -> Unit = {
        val permission = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            actions.onRequestDeviceLocation()
        } else {
            locationPermissionLauncher.launch(permission)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .clearFocusOnTapOutside {
                    listOfNotNull(
                        coordinateFieldBounds.first,
                        temperatureFieldBounds.first,
                        boostTemperatureFieldBounds.first,
                    )
                }.padding(bottom = contentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AutomationToggleCard(
            enabled = schedule.enabled,
            onEnabledChange = actions.onAutomationEnabledChange,
        )

        NightLightTemperatureSection(
            kelvin = schedule.nightLightTemperatureKelvin,
            onKelvinChange = actions.onNightLightTemperatureChange,
            onFieldCoordinatesChange = temperatureFieldBounds.second,
        )

        AutomationScheduleSection(
            schedule = schedule,
            expanded = scheduleExpanded,
            onExpandedChange = { scheduleExpanded = it },
            actions = actions,
            onCoordinateFieldsCoordinatesChange = coordinateFieldBounds.second,
            onRequestDeviceLocation = requestLocationPermission,
            onStartTimeClick = { timeTarget = TimeTarget.START },
            onEndTimeClick = { timeTarget = TimeTarget.END },
            isFetchingLocation = isFetchingLocation,
        )

        NightPhasesSection(
            schedule = schedule,
            expanded = nightPhasesExpanded,
            onExpandedChange = { nightPhasesExpanded = it },
            actions = actions,
            onBoostTemperatureFieldCoordinatesChange = boostTemperatureFieldBounds.second,
        )

        CurrentStrengthCard(currentKelvin = currentKelvin)
    }

    timeTarget?.let { target ->
        TimePickerDialogHost(
            title =
                when (target) {
                    TimeTarget.START -> stringResource(R.string.home_automation_manual_start)
                    TimeTarget.END -> stringResource(R.string.home_automation_manual_end)
                },
            initialHour =
                when (target) {
                    TimeTarget.START -> schedule.manualStartTime.hour
                    TimeTarget.END -> schedule.manualEndTime.hour
                },
            initialMinute =
                when (target) {
                    TimeTarget.START -> schedule.manualStartTime.minute
                    TimeTarget.END -> schedule.manualEndTime.minute
                },
            onDismiss = { timeTarget = null },
            onConfirm = { hour, minute ->
                when (target) {
                    TimeTarget.START -> actions.onManualStartTimeChange(hour, minute)
                    TimeTarget.END -> actions.onManualEndTimeChange(hour, minute)
                }
                timeTarget = null
            },
        )
    }
}

@Composable
private fun CurrentStrengthCard(currentKelvin: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBubble(
                icon = Icons.Rounded.Nightlight,
                size = 40.dp,
                iconSize = 22.dp,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_automation_temperature_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        currentKelvin?.let {
                            stringResource(R.string.home_automation_current_strength, it)
                        } ?: stringResource(R.string.home_automation_current_strength_off),
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (currentKelvin != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AutomationStepCollapsedPreview() {
    BetterNightLightTheme {
        AutomationStep(
            schedule =
                AutomationSchedule(
                    enabled = true,
                    nightLightTemperatureKelvin = 3500,
                ),
            currentKelvin = 3500,
            contentBottomPadding = 0.dp,
            actions = AutomationActions(),
            initialScheduleExpanded = false,
            initialNightPhasesExpanded = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AutomationStepPreview() {
    BetterNightLightTheme {
        AutomationStep(
            schedule =
                AutomationSchedule(
                    enabled = true,
                    manualStartTime = TimeOfDay.of(21, 0),
                    manualEndTime = TimeOfDay.of(6, 0),
                    nightLightTemperatureKelvin = 3500,
                ),
            currentKelvin = 3500,
            contentBottomPadding = 0.dp,
            actions = AutomationActions(),
        )
    }
}
