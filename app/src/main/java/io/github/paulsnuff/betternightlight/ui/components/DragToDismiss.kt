package io.github.paulsnuff.betternightlight.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class DragAxis { Horizontal, Vertical }

private const val DISMISS_ANIMATION_MS = 200
private const val DISMISS_DRAG_FRACTION = 0.4f
private const val DISMISS_DRAG_OVERSHOOT = 1.5f
private const val AXIS_LOCK_SLOP_PX = 8f

@Composable
fun DragToDismiss(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onDragStarted: () -> Unit = {},
    onDragEnded: (dismissed: Boolean) -> Unit = {},
    content: @Composable () -> Unit,
) {
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    fun stopAnimations() {
        coroutineScope.launch {
            offsetX.stop()
            offsetY.stop()
        }
    }

    fun springBack() {
        coroutineScope.launch {
            launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
            launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
        }
    }

    Box(
        modifier =
            modifier
                .onSizeChanged { contentSize = it }
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .pointerInput(contentSize) {
                    var lockedAxis: DragAxis? = null
                    var pendingX = 0f
                    var pendingY = 0f

                    fun dismissAlong(axis: DragAxis) {
                        val maxX = contentSize.width * DISMISS_DRAG_OVERSHOOT
                        val maxY = contentSize.height * DISMISS_DRAG_OVERSHOOT
                        coroutineScope.launch {
                            when (axis) {
                                DragAxis.Horizontal ->
                                    offsetX.animateTo(
                                        if (offsetX.value > 0f) maxX else -maxX,
                                        tween(DISMISS_ANIMATION_MS),
                                    )

                                DragAxis.Vertical ->
                                    offsetY.animateTo(-maxY, tween(DISMISS_ANIMATION_MS))
                            }
                            onDismiss()
                        }
                    }

                    detectDragGestures(
                        onDragStart = {
                            lockedAxis = null
                            pendingX = 0f
                            pendingY = 0f
                            stopAnimations()
                            onDragStarted()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (lockedAxis == null) {
                                pendingX += dragAmount.x
                                pendingY += dragAmount.y
                                if (abs(pendingX) > AXIS_LOCK_SLOP_PX || abs(pendingY) > AXIS_LOCK_SLOP_PX) {
                                    lockedAxis =
                                        if (abs(pendingX) > abs(pendingY)) {
                                            DragAxis.Horizontal
                                        } else {
                                            DragAxis.Vertical
                                        }
                                } else {
                                    return@detectDragGestures
                                }
                            }

                            val maxX = contentSize.width * DISMISS_DRAG_OVERSHOOT
                            val maxY = contentSize.height * DISMISS_DRAG_OVERSHOOT
                            val axis = lockedAxis ?: return@detectDragGestures
                            coroutineScope.launch {
                                when (axis) {
                                    DragAxis.Horizontal ->
                                        offsetX.snapTo((offsetX.value + dragAmount.x).coerceIn(-maxX, maxX))

                                    DragAxis.Vertical ->
                                        offsetY.snapTo((offsetY.value + dragAmount.y).coerceIn(-maxY, 0f))
                                }
                            }
                        },
                        onDragEnd = {
                            val axis = lockedAxis
                            if (axis == null) {
                                springBack()
                            } else {
                                val dismissed =
                                    when (axis) {
                                        DragAxis.Horizontal ->
                                            abs(offsetX.value) >
                                                contentSize.width * DISMISS_DRAG_FRACTION

                                        DragAxis.Vertical ->
                                            offsetY.value <
                                                -contentSize.height * DISMISS_DRAG_FRACTION
                                    }

                                if (dismissed) {
                                    dismissAlong(axis)
                                    onDragEnded(true)
                                } else {
                                    springBack()
                                    onDragEnded(false)
                                }
                            }
                        },
                        onDragCancel = {
                            springBack()
                            onDragEnded(false)
                        },
                    )
                },
    ) {
        content()
    }
}