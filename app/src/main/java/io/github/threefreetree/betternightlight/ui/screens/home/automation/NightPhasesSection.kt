package io.github.threefreetree.betternightlight.ui.screens.home.automation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.threefreetree.betternightlight.R
import io.github.threefreetree.betternightlight.domain.MINUTES_PER_DAY
import io.github.threefreetree.betternightlight.domain.TimelineSegment
import io.github.threefreetree.betternightlight.domain.TimelineSegmentType
import io.github.threefreetree.betternightlight.domain.buildTimelineSegments
import io.github.threefreetree.betternightlight.domain.model.AutomationSchedule
import io.github.threefreetree.betternightlight.domain.model.AutomationTrigger
import io.github.threefreetree.betternightlight.domain.model.PhaseTransition
import io.github.threefreetree.betternightlight.domain.model.TimeOfDay
import io.github.threefreetree.betternightlight.domain.nightWindowMinutes
import io.github.threefreetree.betternightlight.domain.shiftSegmentsToNoonView
import io.github.threefreetree.betternightlight.ui.components.AppSwitch
import io.github.threefreetree.betternightlight.ui.components.IconBubble
import io.github.threefreetree.betternightlight.ui.components.SelectableCard
import io.github.threefreetree.betternightlight.ui.components.TimePickerRow
import io.github.threefreetree.betternightlight.ui.components.selectableRowColors
import io.github.threefreetree.betternightlight.ui.screens.home.AutomationActions
import io.github.threefreetree.betternightlight.ui.theme.BetterNightLightTheme
import io.github.threefreetree.betternightlight.domain.resolvedBoostKelvin as resolveBoostKelvin

internal enum class PhaseTimeTarget { BOOST, OFF }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NightPhasesSection(
    schedule: AutomationSchedule,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actions: AutomationActions,
    onBoostTemperatureFieldCoordinatesChange: ((LayoutCoordinates?) -> Unit)? = null,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "phasesExpandRotation",
    )

    var timeTarget by remember { mutableStateOf<PhaseTimeTarget?>(null) }
    val boostEnabled = schedule.boostEnabled
    val boostHour = schedule.boostTime.hour
    val boostMinute = schedule.boostTime.minute
    val offEnabled = schedule.offEnabled
    val offHour = schedule.offTime.hour
    val offMinute = schedule.offTime.minute
    val resolvedBoostKelvin =
        remember(schedule.boostKelvin, schedule.nightLightTemperatureKelvin) {
            resolveBoostKelvin(schedule)
        }

    val baseMinutes = remember(schedule) { nightWindowMinutes(schedule) }
    val baseStartMinutes = baseMinutes.first
    val baseEndMinutes = baseMinutes.second

    // Hoisted out of the trigger-dependent branch below: remember must not
    // live inside a `when`. Re-computed shortly after every midnight so
    // sunset/sunrise stay current when the app stays open across a day change.
    val sunTimes = rememberSunTimes(schedule.latitude, schedule.longitude)

    val baseTimeLabel =
        when (schedule.trigger) {
            AutomationTrigger.MANUAL -> {
                formatTime(schedule.manualStartTime.hour, schedule.manualStartTime.minute) + " – " +
                    formatTime(schedule.manualEndTime.hour, schedule.manualEndTime.minute)
            }

            AutomationTrigger.LOCATION -> {
                val unavailable = stringResource(R.string.home_automation_time_unavailable)
                val sunset = sunTimes?.sunset?.let { formatSunTime(it, unavailable) } ?: unavailable
                val sunrise = sunTimes?.sunrise?.let { formatSunTime(it, unavailable) } ?: unavailable
                stringResource(
                    R.string.home_automation_phase_base_times_location,
                    sunset,
                    sunrise,
                )
            }
        }

    val boostStartMinutes = boostHour * 60 + boostMinute
    val offMinutes = offHour * 60 + offMinute

    val segments =
        remember(
            baseStartMinutes,
            baseEndMinutes,
            boostStartMinutes,
            boostEnabled,
            offMinutes,
            offEnabled,
        ) {
            buildTimelineSegments(
                baseStartMinutes = baseStartMinutes,
                baseEndMinutes = baseEndMinutes,
                boostStartMinutes = boostStartMinutes,
                boostEnabled = boostEnabled,
                offMinutes = offMinutes,
                offEnabled = offEnabled,
            )
        }

    val summary =
        buildString {
            if (boostEnabled) {
                append(stringResource(R.string.home_automation_phase_boost_summary_label))
                append(' ')
                append(formatTime(boostHour, boostMinute))
            }
            if (offEnabled) {
                if (boostEnabled) append("  •  ")
                append(stringResource(R.string.home_automation_phase_off_summary_label))
                append(' ')
                append(formatTime(offHour, offMinute))
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
                        .heightIn(min = 72.dp)
                        .clickable { onExpandedChange(!expanded) },
                colors =
                    ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                leadingContent = {
                    IconBubble(
                        icon = Icons.Rounded.NightsStay,
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.home_automation_phases_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                supportingContent = {
                    if (!expanded && summary.isNotEmpty()) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription =
                            stringResource(
                                if (expanded) {
                                    R.string.home_automation_phases_collapse
                                } else {
                                    R.string.home_automation_phases_expand
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

                    val legendEntries =
                        buildList {
                            add(
                                MaterialTheme.colorScheme.primaryContainer to
                                    stringResource(R.string.home_automation_phase_legend_base),
                            )
                            if (boostEnabled) {
                                add(
                                    MaterialTheme.colorScheme.primary to
                                        stringResource(R.string.home_automation_phase_legend_boost),
                                )
                            }
                            if (offEnabled) {
                                add(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f) to
                                        stringResource(R.string.home_automation_phase_legend_off),
                                )
                            }
                        }

                    NightPhasesTimeline(
                        segments = segments,
                        legendEntries = legendEntries,
                    )

                    BasePhaseRow(
                        timeLabel = baseTimeLabel,
                    )

                    BoostPhaseCard(
                        enabled = boostEnabled,
                        onEnabledChange = actions.onBoostEnabledChange,
                        startHour = boostHour,
                        startMinute = boostMinute,
                        kelvin = resolvedBoostKelvin,
                        onKelvinChange = actions.onBoostKelvinChange,
                        transition = schedule.phaseTransition,
                        onTransitionChange = actions.onPhaseTransitionChange,
                        onStartTimeClick = { timeTarget = PhaseTimeTarget.BOOST },
                        onTemperatureFieldCoordinatesChange = onBoostTemperatureFieldCoordinatesChange,
                    )

                    OffPhaseCard(
                        enabled = offEnabled,
                        onEnabledChange = actions.onOffEnabledChange,
                        hour = offHour,
                        minute = offMinute,
                        onTimeClick = { timeTarget = PhaseTimeTarget.OFF },
                    )
                }
            }
        }
    }

    timeTarget?.let { target ->
        PhaseTimePickerDialog(
            target = target,
            boostTime = schedule.boostTime,
            offTime = schedule.offTime,
            onDismiss = { timeTarget = null },
            onConfirm = { hour, minute ->
                when (target) {
                    PhaseTimeTarget.BOOST -> actions.onBoostTimeChange(hour, minute)
                    PhaseTimeTarget.OFF -> actions.onOffTimeChange(hour, minute)
                }
                timeTarget = null
            },
        )
    }
}

@Composable
private fun PhaseTimePickerDialog(
    target: PhaseTimeTarget,
    boostTime: TimeOfDay,
    offTime: TimeOfDay,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    TimePickerDialogHost(
        title =
            when (target) {
                PhaseTimeTarget.BOOST -> stringResource(R.string.home_automation_phase_boost_start)
                PhaseTimeTarget.OFF -> stringResource(R.string.home_automation_phase_off_time)
            },
        initialHour =
            when (target) {
                PhaseTimeTarget.BOOST -> boostTime.hour
                PhaseTimeTarget.OFF -> offTime.hour
            },
        initialMinute =
            when (target) {
                PhaseTimeTarget.BOOST -> boostTime.minute
                PhaseTimeTarget.OFF -> offTime.minute
            },
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun NightPhasesTimeline(
    segments: List<TimelineSegment>,
    legendEntries: List<Pair<Color, String>>,
) {
    val shifted = remember(segments) { shiftSegmentsToNoonView(segments) }

    @Composable
    fun colorFor(type: TimelineSegmentType): Color =
        when (type) {
            TimelineSegmentType.NONE -> Color.Transparent
            TimelineSegmentType.BASE -> MaterialTheme.colorScheme.primaryContainer
            TimelineSegmentType.BOOST -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            TimelineSegmentType.OFF -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val density = LocalDensity.current
            val totalWidthPx = with(density) { maxWidth.toPx() }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                shifted.forEach { segment ->
                    val startFraction = segment.start.toFloat() / MINUTES_PER_DAY
                    val endFraction = segment.end.toFloat() / MINUTES_PER_DAY
                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(endFraction - startFraction)
                                .absoluteOffset(
                                    x = with(density) { (totalWidthPx * startFraction).toDp() },
                                ).background(colorFor(segment.type)),
                    )
                }

                Text(
                    text = "12",
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp),
                )
                Text(
                    text = "24",
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Center),
                )
                Text(
                    text = "12",
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp),
                )
            }
        }

        if (legendEntries.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                legendEntries.forEach { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(entry.first),
                        )
                        Text(
                            text = entry.second,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BasePhaseRow(
    timeLabel: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.NightsStay,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.home_automation_phase_base_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.home_automation_phase_base_badge),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun BoostPhaseCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    startHour: Int,
    startMinute: Int,
    kelvin: Int,
    onKelvinChange: (Int) -> Unit,
    transition: PhaseTransition,
    onTransitionChange: (PhaseTransition) -> Unit,
    onStartTimeClick: () -> Unit,
    onTemperatureFieldCoordinatesChange: ((LayoutCoordinates?) -> Unit)? = null,
) {
    SelectableCard(
        selected = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBubble(
                    icon = Icons.Rounded.BrightnessHigh,
                    size = 40.dp,
                    iconSize = 22.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_automation_phase_boost_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.home_automation_phase_boost_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AppSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                )
            }

            if (enabled) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                KelvinSliderControl(
                    value = kelvin,
                    onValueChange = onKelvinChange,
                    inputLabel = stringResource(R.string.home_automation_temperature_input),
                    onFieldCoordinatesChange = onTemperatureFieldCoordinatesChange,
                    leadingContent = {
                        TimePickerRow(
                            label = stringResource(R.string.home_automation_phase_boost_start),
                            time = formatTime(startHour, startMinute),
                            onClick = onStartTimeClick,
                        )
                    },
                )

                Text(
                    text = stringResource(R.string.home_automation_phase_transition),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                PhaseOptionRow(
                    selected = transition == PhaseTransition.INSTANT,
                    icon = Icons.Rounded.Bolt,
                    title = stringResource(R.string.home_automation_phase_transition_instant),
                    subtitle = stringResource(R.string.home_automation_phase_transition_instant_desc),
                    onClick = { onTransitionChange(PhaseTransition.INSTANT) },
                )

                PhaseOptionRow(
                    selected = transition == PhaseTransition.GRADUAL,
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    title = stringResource(R.string.home_automation_phase_transition_gradual),
                    subtitle = stringResource(R.string.home_automation_phase_transition_gradual_desc),
                    onClick = { onTransitionChange(PhaseTransition.GRADUAL) },
                )
            }
        }
    }
}

@Composable
private fun OffPhaseCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    hour: Int,
    minute: Int,
    onTimeClick: () -> Unit,
) {
    SelectableCard(
        selected = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBubble(
                    icon = Icons.Rounded.PowerOff,
                    size = 40.dp,
                    iconSize = 22.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_automation_phase_off_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.home_automation_phase_off_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AppSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                )
            }

            if (enabled) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                TimePickerRow(
                    label = stringResource(R.string.home_automation_phase_off_time),
                    time = formatTime(hour, minute),
                    onClick = onTimeClick,
                )

                Text(
                    text = stringResource(R.string.home_automation_phase_off_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PhaseOptionRow(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectableRowColors(selected)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            modifier = Modifier.size(22.dp),
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
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun NightPhasesSectionCollapsedPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NightPhasesSection(
                schedule =
                    AutomationSchedule(
                        enabled = true,
                        trigger = AutomationTrigger.MANUAL,
                        manualStartTime = TimeOfDay.of(21, 0),
                        manualEndTime = TimeOfDay.of(6, 0),
                        nightLightTemperatureKelvin = 3500,
                    ),
                expanded = false,
                onExpandedChange = {},
                actions = AutomationActions(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun NightPhasesSectionExpandedPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NightPhasesSection(
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
            )
        }
    }
}
