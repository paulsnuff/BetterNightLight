package io.github.paulsnuff.betternightlight.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import io.github.paulsnuff.betternightlight.ui.theme.BetterNightLightTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val trackWidth = 58.dp
    val trackHeight = 34.dp
    val checkedThumbSize = 26.dp
    val uncheckedThumbSize = 24.dp
    val trackPadding = 4.dp
    val travelDistance: Dp = trackWidth - checkedThumbSize - (trackPadding * 2)

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val currentChecked by rememberUpdatedState(checked)
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    var isDragging by remember { mutableStateOf(false) }
    val animProgress = remember { Animatable(if (checked) 1f else 0f) }

    LaunchedEffect(checked) {
        if (!isDragging) {
            animProgress.animateTo(
                targetValue = if (checked) 1f else 0f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
            )
        }
    }

    val progress = animProgress.value.coerceIn(0f, 1f)
    val currentThumbSize = lerp(uncheckedThumbSize, checkedThumbSize, progress)
    val minThumbCenter = trackPadding + checkedThumbSize / 2
    val maxThumbCenter = trackWidth - trackPadding - checkedThumbSize / 2
    val currentThumbCenter = lerp(minThumbCenter, maxThumbCenter, progress)
    val thumbStartOffset = currentThumbCenter - currentThumbSize / 2

    val trackColor =
        lerp(
            start = MaterialTheme.colorScheme.surfaceContainerHighest,
            stop = MaterialTheme.colorScheme.primary,
            fraction = progress,
        )
    val borderColor =
        lerp(
            start = MaterialTheme.colorScheme.outline,
            stop = Color.Transparent,
            fraction = progress,
        )
    val thumbColor =
        lerp(
            start = MaterialTheme.colorScheme.outline,
            stop = MaterialTheme.colorScheme.onPrimary,
            fraction = progress,
        )
    val iconAlpha = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)
    val closeIconAlpha = ((0.6f - progress) / 0.6f).coerceIn(0f, 1f)

    Box(
        modifier =
            modifier
                .alpha(if (enabled) 1f else 0.38f)
                .size(width = trackWidth, height = trackHeight)
                .semantics {
                    this.role = Role.Switch
                    this.toggleableState = ToggleableState(checked)
                    if (enabled && onCheckedChange != null) {
                        this.onClick {
                            currentOnCheckedChange?.invoke(!currentChecked)
                            true
                        }
                    }
                }.clip(RoundedCornerShape(trackHeight / 2))
                .background(trackColor)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(trackHeight / 2),
                ).pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    val travelPx = with(density) { travelDistance.toPx() }
                    val touchSlop = viewConfiguration.touchSlop

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragging = false
                        val initialProgress = animProgress.value.coerceIn(0f, 1f)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || change.isConsumed) {
                                if (dragging) {
                                    isDragging = false
                                    coroutineScope.launch {
                                        animProgress.animateTo(
                                            if (currentChecked) 1f else 0f,
                                            animationSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium,
                                                ),
                                        )
                                    }
                                }
                                break
                            }

                            if (!change.pressed) {
                                if (dragging) {
                                    isDragging = false
                                    val target = animProgress.value > 0.5f
                                    currentOnCheckedChange?.invoke(target)
                                    coroutineScope.launch {
                                        animProgress.animateTo(
                                            if (target) 1f else 0f,
                                            animationSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium,
                                                ),
                                        )
                                    }
                                } else {
                                    currentOnCheckedChange?.invoke(!currentChecked)
                                }
                                break
                            }

                            val totalDrag = change.position.x - down.position.x
                            if (!dragging && abs(totalDrag) > touchSlop) {
                                dragging = true
                                isDragging = true
                            }

                            if (dragging) {
                                change.consume()
                                if (travelPx > 0) {
                                    val slopCorrection = if (totalDrag > 0) touchSlop else -touchSlop
                                    val progressDelta = (totalDrag - slopCorrection) / travelPx
                                    val newProgress = (initialProgress + progressDelta).coerceIn(0f, 1f)
                                    coroutineScope.launch {
                                        animProgress.snapTo(newProgress)
                                    }
                                }
                            }
                        }
                    }
                },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .offset(x = thumbStartOffset)
                    .size(currentThumbSize)
                    .clip(CircleShape)
                    .background(thumbColor),
            contentAlignment = Alignment.Center,
        ) {
            if (iconAlpha > 0f) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .size(16.dp)
                            .graphicsLayer { alpha = iconAlpha },
                )
            }
            if (closeIconAlpha > 0f) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier =
                        Modifier
                            .size(14.dp)
                            .graphicsLayer { alpha = closeIconAlpha },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppSwitchCheckedPreview() {
    BetterNightLightTheme {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            AppSwitch(checked = true, onCheckedChange = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppSwitchUncheckedPreview() {
    BetterNightLightTheme {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            AppSwitch(checked = false, onCheckedChange = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppSwitchDisabledPreview() {
    BetterNightLightTheme {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            AppSwitch(checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}
