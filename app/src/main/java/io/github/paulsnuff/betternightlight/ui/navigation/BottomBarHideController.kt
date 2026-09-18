package io.github.paulsnuff.betternightlight.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
class BottomBarHideController(
    private val scope: CoroutineScope,
    density: Density,
) {
    private val maxOffsetPx: Float = with(density) { 100.dp.toPx() }
    private val deadZonePx: Float = with(density) { 8.dp.toPx() }
    private val minFlingVelocityPx: Float = with(density) { 450.dp.toPx() }
    private val horizontalSwipeThresholdPx: Float = with(density) { 6.dp.toPx() }
    private val followRatio: Float = 0.6f

    private val offsetPxState = mutableFloatStateOf(0f)
    var offsetPx: Float
        get() = offsetPxState.floatValue
        private set(value) {
            offsetPxState.floatValue = value
        }

    val hideFraction: Float
        get() = if (maxOffsetPx > 0f) (offsetPx / maxOffsetPx).coerceIn(0f, 1f) else 0f

    private var settleJob: Job? = null
    private var scrollAccumulator: Float = 0f
    private var lastScrollDirection: Int =
        0 // -1: up (finger up / scroll down), 1: down (finger down / scroll up)

    val nestedScrollConnection: NestedScrollConnection =
        object : NestedScrollConnection {
            @Suppress("SameReturnValue")
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Reveal bar on horizontal pager swipe
                if (abs(available.x) > abs(available.y) * 1.3f &&
                    abs(available.x) > horizontalSwipeThresholdPx
                ) {
                    if (offsetPx > 0f) {
                        revealBar()
                    }
                    return Offset.Zero
                }

                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    val currentDirection = if (available.y < 0f) -1 else 1
                    if (currentDirection != lastScrollDirection) {
                        lastScrollDirection = currentDirection
                        scrollAccumulator = 0f
                    }

                    scrollAccumulator += available.y

                    if (abs(scrollAccumulator) > deadZonePx) {
                        settleJob?.cancel()
                        settleJob = null

                        val delta = -available.y * followRatio
                        offsetPx = (offsetPx + delta).coerceIn(0f, maxOffsetPx)
                    }
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val vy = available.y
                val targetOffset =
                    when {
                        vy < -minFlingVelocityPx -> maxOffsetPx
                        vy > minFlingVelocityPx -> 0f
                        hideFraction >= 0.4f -> maxOffsetPx
                        else -> 0f
                    }

                animateToOffset(targetOffset)
                return Velocity.Zero
            }
        }

    fun revealBar() {
        if (offsetPx > 0f) {
            animateToOffset(0f)
        }
    }

    private fun animateToOffset(targetOffset: Float) {
        settleJob?.cancel()
        settleJob =
            scope.launch {
                val animatable = Animatable(offsetPx)
                animatable.animateTo(
                    targetValue = targetOffset,
                    animationSpec =
                        tween(
                            durationMillis = 260,
                            easing = FastOutSlowInEasing,
                        ),
                ) {
                    offsetPx = value
                }
            }
    }
}

@Composable
fun rememberBottomBarHideController(pagerState: PagerState? = null): BottomBarHideController {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val controller =
        remember(coroutineScope, density) {
            BottomBarHideController(coroutineScope, density)
        }

    if (pagerState != null) {
        LaunchedEffect(pagerState.currentPage) {
            controller.revealBar()
        }
    }

    return controller
}

fun Modifier.bottomBarNestedScroll(controller: BottomBarHideController): Modifier = this.nestedScroll(connection = controller.nestedScrollConnection)
