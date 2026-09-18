package io.github.paulsnuff.betternightlight.ui.components

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager

fun Modifier.clearFocusOnTapOutside(excludeAreaBounds: () -> List<Rect>): Modifier =
    composed {
        val focusManager = LocalFocusManager.current
        var containerRootOrigin by remember { mutableStateOf(Offset.Zero) }
        pointerInput(excludeAreaBounds) {
            awaitPointerEventScope {
                while (true) {
                    val down =
                        awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Final,
                        )
                    var movedBeyondSlop = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        var released = false
                        for (change in event.changes) {
                            if (change.id == down.id) {
                                if (!change.pressed) {
                                    released = true
                                } else if (!movedBeyondSlop &&
                                    (change.position - down.position).getDistance() >
                                    viewConfiguration.touchSlop
                                ) {
                                    movedBeyondSlop = true
                                }
                            }
                        }
                        if (released) break
                    }
                    if (movedBeyondSlop) continue

                    val rootOffset = down.position + containerRootOrigin
                    val insideExcluded = excludeAreaBounds().any { it.contains(rootOffset) }
                    if (!insideExcluded) {
                        focusManager.clearFocus()
                    }
                }
            }
        }.onGloballyPositioned { coords ->
            if (coords.isAttached) {
                containerRootOrigin = coords.positionInRoot()
            }
        }
    }

@Composable
fun rememberTrackedBounds(): Pair<Rect?, (LayoutCoordinates?) -> Unit> {
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val setter: (LayoutCoordinates?) -> Unit = { coords ->
        bounds = coords?.takeIf { it.isAttached }?.boundsInRoot()
    }
    return bounds to setter
}
