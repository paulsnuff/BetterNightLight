package io.github.threefreetree.betternightlight.ui.screens.home.automation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditLocation
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.threefreetree.betternightlight.R
import io.github.threefreetree.betternightlight.domain.LATITUDE_MAX
import io.github.threefreetree.betternightlight.domain.LATITUDE_MIN
import io.github.threefreetree.betternightlight.domain.LONGITUDE_MAX
import io.github.threefreetree.betternightlight.domain.LONGITUDE_MIN
import io.github.threefreetree.betternightlight.domain.SunTimesInfo
import io.github.threefreetree.betternightlight.domain.model.AutomationLocationSource
import io.github.threefreetree.betternightlight.domain.model.AutomationSchedule
import io.github.threefreetree.betternightlight.domain.model.AutomationTrigger
import io.github.threefreetree.betternightlight.domain.model.TimeOfDay
import io.github.threefreetree.betternightlight.ui.components.IconBubble
import io.github.threefreetree.betternightlight.ui.components.SelectableCard
import io.github.threefreetree.betternightlight.ui.components.TimePickerRow
import io.github.threefreetree.betternightlight.ui.components.selectableRowColors
import io.github.threefreetree.betternightlight.ui.screens.home.AutomationActions
import io.github.threefreetree.betternightlight.ui.theme.BetterNightLightTheme

@Composable
internal fun AutomationScheduleSection(
    schedule: AutomationSchedule,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actions: AutomationActions,
    onCoordinateFieldsCoordinatesChange: (LayoutCoordinates?) -> Unit,
    onRequestDeviceLocation: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    isFetchingLocation: Boolean = false,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "scheduleExpandRotation",
    )

    val summary =
        when (schedule.trigger) {
            AutomationTrigger.MANUAL -> {
                stringResource(
                    R.string.home_automation_schedule_summary_manual,
                    formatTime(schedule.manualStartTime.hour, schedule.manualStartTime.minute),
                    formatTime(schedule.manualEndTime.hour, schedule.manualEndTime.minute),
                )
            }

            AutomationTrigger.LOCATION -> {
                stringResource(R.string.home_automation_trigger_location_title)
            }
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column {
            ListItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onExpandedChange(!expanded) },
                colors =
                    ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                leadingContent = {
                    IconBubble(
                        icon = Icons.Rounded.Schedule,
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.home_automation_trigger_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                supportingContent = {
                    AnimatedVisibility(
                        visible = !expanded,
                        enter = fadeIn(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(150)),
                    ) {
                        if (schedule.trigger == AutomationTrigger.MANUAL) {
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            val sunTimes = rememberSunTimes(schedule.latitude, schedule.longitude)
                            if (sunTimes != null) {
                                SunTimesBadge(sunTimes)
                            } else {
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription =
                            stringResource(
                                if (expanded) {
                                    R.string.home_automation_schedule_collapse
                                } else {
                                    R.string.home_automation_schedule_expand
                                },
                            ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .size(24.dp)
                                .graphicsLayer { rotationZ = rotation },
                    )
                },
            )

            AnimatedVisibility(
                visible = expanded,
                enter =
                    fadeIn(animationSpec = tween(200)) +
                        expandVertically(animationSpec = tween(200)),
                exit =
                    fadeOut(animationSpec = tween(150)) +
                        shrinkVertically(animationSpec = tween(150)),
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    TriggerOptionCard(
                        selected = schedule.trigger == AutomationTrigger.MANUAL,
                        icon = Icons.Rounded.Schedule,
                        title = stringResource(R.string.home_automation_trigger_manual_title),
                        onClick = { actions.onTriggerChange(AutomationTrigger.MANUAL) },
                    ) {
                        if (schedule.trigger == AutomationTrigger.MANUAL) {
                            ManualTimeSection(
                                startTime =
                                    formatTime(
                                        schedule.manualStartTime.hour,
                                        schedule.manualStartTime.minute,
                                    ),
                                endTime =
                                    formatTime(
                                        schedule.manualEndTime.hour,
                                        schedule.manualEndTime.minute,
                                    ),
                                onStartClick = onStartTimeClick,
                                onEndClick = onEndTimeClick,
                            )
                        }
                    }

                    TriggerOptionCard(
                        selected = schedule.trigger == AutomationTrigger.LOCATION,
                        icon = Icons.Rounded.Public,
                        title = stringResource(R.string.home_automation_trigger_location_title),
                        onClick = { actions.onTriggerChange(AutomationTrigger.LOCATION) },
                    ) {
                        if (schedule.trigger == AutomationTrigger.LOCATION) {
                            LocationDetailsSection(
                                source = schedule.locationSource,
                                latitude = schedule.latitude,
                                longitude = schedule.longitude,
                                onLocationSourceChange = actions.onLocationSourceChange,
                                onRequestDeviceLocation = onRequestDeviceLocation,
                                onCoordinatesChange = actions.onCoordinatesChange,
                                onCoordinateFieldsCoordinatesChange = onCoordinateFieldsCoordinatesChange,
                                isFetchingLocation = isFetchingLocation,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TriggerOptionCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    details: (@Composable () -> Unit)? = null,
) {
    SelectableCard(
        selected = selected,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(selected = selected, onClick = onClick)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.size(24.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            details?.invoke()
        }
    }
}

@Composable
private fun ManualTimeSection(
    startTime: String,
    endTime: String,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
) {
    TimePickerRow(
        label = stringResource(R.string.home_automation_manual_start),
        time = startTime,
        onClick = onStartClick,
    )
    TimePickerRow(
        label = stringResource(R.string.home_automation_manual_end),
        time = endTime,
        onClick = onEndClick,
    )
}

@Composable
private fun LocationDetailsSection(
    source: AutomationLocationSource,
    latitude: Double?,
    longitude: Double?,
    onLocationSourceChange: (AutomationLocationSource) -> Unit,
    onRequestDeviceLocation: () -> Unit,
    onCoordinatesChange: (latitude: Double?, longitude: Double?) -> Unit,
    onCoordinateFieldsCoordinatesChange: (LayoutCoordinates?) -> Unit,
    isFetchingLocation: Boolean = false,
) {
    LocationSourceRow(
        selected = source == AutomationLocationSource.DEVICE,
        icon = Icons.Rounded.MyLocation,
        title = stringResource(R.string.home_automation_use_device_location),
        isFetching = isFetchingLocation,
        onClick = {
            onLocationSourceChange(AutomationLocationSource.DEVICE)
            onRequestDeviceLocation()
        },
    )

    if (source == AutomationLocationSource.DEVICE) {
        if (latitude != null && longitude != null) {
            Text(
                text =
                    stringResource(
                        R.string.home_automation_current_coordinates,
                        formatCoordinate(latitude),
                        formatCoordinate(longitude),
                    ),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }

    LocationSourceRow(
        selected = source == AutomationLocationSource.MANUAL,
        icon = Icons.Rounded.EditLocation,
        title = stringResource(R.string.home_automation_enter_manually),
        onClick = { onLocationSourceChange(AutomationLocationSource.MANUAL) },
    )

    if (source == AutomationLocationSource.MANUAL) {
        ManualCoordinateSection(
            latitude = latitude,
            longitude = longitude,
            onCoordinatesChange = onCoordinatesChange,
            onBoundsChange = onCoordinateFieldsCoordinatesChange,
        )
    }

    if (latitude != null && longitude != null) {
        SunTimesSummary(
            latitude = latitude,
            longitude = longitude,
        )
    }
}

@Composable
private fun SunTimesBadge(sunTimes: SunTimesInfo) {
    val unavailable = stringResource(R.string.home_automation_time_unavailable)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.NightsStay,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = formatSunTime(sunTimes.sunset, unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.WbSunny,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = formatSunTime(sunTimes.sunrise, unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SunTimesSummary(
    latitude: Double,
    longitude: Double,
) {
    val sunTimes = rememberSunTimes(latitude, longitude) ?: return

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SunTimeItem(
            icon = Icons.Rounded.NightsStay,
            label = stringResource(R.string.home_automation_sunset),
            time =
                formatSunTime(
                    sunTimes.sunset,
                    unavailable = stringResource(R.string.home_automation_time_unavailable),
                ),
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .size(width = 1.dp, height = 36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        )
        SunTimeItem(
            icon = Icons.Rounded.WbSunny,
            label = stringResource(R.string.home_automation_sunrise),
            time =
                formatSunTime(
                    sunTimes.sunrise,
                    unavailable = stringResource(R.string.home_automation_time_unavailable),
                ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SunTimeItem(
    icon: ImageVector,
    label: String,
    time: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LocationSourceRow(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    isFetching: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "locationSearch")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 700),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "locationSearchPulse",
    )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectableRowColors(selected)
                .clickable(enabled = !isFetching, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, enabled = !isFetching, onClick = onClick)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier =
                Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        if (isFetching) {
                            alpha = pulse
                            scaleX = 1f + 0.25f * pulse
                            scaleY = 1f + 0.25f * pulse
                        }
                    },
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ManualCoordinateSection(
    latitude: Double?,
    longitude: Double?,
    onCoordinatesChange: (latitude: Double?, longitude: Double?) -> Unit,
    onBoundsChange: (LayoutCoordinates?) -> Unit,
) {
    var latText by rememberSaveable { mutableStateOf(latitude?.toString() ?: "") }
    var lonText by rememberSaveable { mutableStateOf(longitude?.toString() ?: "") }
    var isEditingText by remember { mutableStateOf(false) }
    LaunchedEffect(latitude, longitude, isEditingText) {
        if (!isEditingText) {
            latText = latitude?.let { formatCoordinate(it) } ?: ""
            lonText = longitude?.let { formatCoordinate(it) } ?: ""
        }
    }

    fun commit() {
        val parsedLat =
            latText.replace(',', '.').toDoubleOrNull()?.coerceIn(LATITUDE_MIN, LATITUDE_MAX)
        val parsedLon =
            lonText.replace(',', '.').toDoubleOrNull()?.coerceIn(LONGITUDE_MIN, LONGITUDE_MAX)
        if (parsedLat != null) latText = formatCoordinate(parsedLat)
        if (parsedLon != null) lonText = formatCoordinate(parsedLon)
        onCoordinatesChange(parsedLat, parsedLon)
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { onBoundsChange(it) },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CoordinateField(
            label = stringResource(R.string.home_automation_latitude),
            value = latText,
            onValueChange = {
                isEditingText = true
                latText = it
            },
            onCommit = ::commit,
            onEditingFinished = { isEditingText = false },
            modifier = Modifier.weight(1f),
        )
        CoordinateField(
            label = stringResource(R.string.home_automation_longitude),
            value = lonText,
            onValueChange = {
                isEditingText = true
                lonText = it
            },
            onCommit = ::commit,
            onEditingFinished = { isEditingText = false },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CoordinateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    onEditingFinished: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    onCommit()
                    focusManager.clearFocus()
                },
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        onCommit()
                        onEditingFinished()
                    }
                },
    )
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun ScheduleSectionManualPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AutomationScheduleSection(
                schedule =
                    AutomationSchedule(
                        enabled = true,
                        trigger = AutomationTrigger.MANUAL,
                        manualStartTime = TimeOfDay.of(21, 0),
                        manualEndTime = TimeOfDay.of(6, 0),
                        nightLightTemperatureKelvin = 3500,
                    ),
                expanded = true,
                onExpandedChange = {},
                actions = AutomationActions(),
                onCoordinateFieldsCoordinatesChange = {},
                onRequestDeviceLocation = {},
                onStartTimeClick = {},
                onEndTimeClick = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun ScheduleSectionLocationPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AutomationScheduleSection(
                schedule =
                    AutomationSchedule(
                        enabled = true,
                        trigger = AutomationTrigger.LOCATION,
                        locationSource = AutomationLocationSource.DEVICE,
                        latitude = 52.2297,
                        longitude = 21.0122,
                        nightLightTemperatureKelvin = 3500,
                    ),
                expanded = true,
                onExpandedChange = {},
                actions = AutomationActions(),
                onCoordinateFieldsCoordinatesChange = {},
                onRequestDeviceLocation = {},
                onStartTimeClick = {},
                onEndTimeClick = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun ScheduleSectionManualCoordinatesPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AutomationScheduleSection(
                schedule =
                    AutomationSchedule(
                        enabled = true,
                        trigger = AutomationTrigger.LOCATION,
                        locationSource = AutomationLocationSource.MANUAL,
                        latitude = 48.8566,
                        longitude = 2.3522,
                        nightLightTemperatureKelvin = 3500,
                    ),
                expanded = true,
                onExpandedChange = {},
                actions = AutomationActions(),
                onCoordinateFieldsCoordinatesChange = {},
                onRequestDeviceLocation = {},
                onStartTimeClick = {},
                onEndTimeClick = {},
            )
        }
    }
}
