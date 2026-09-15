package io.github.threefreetree.betternightlight.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.threefreetree.betternightlight.ui.theme.BetterNightLightTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun SlidingBottomNavigationBar(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    destinations: List<TopLevelDestination>,
    onDestinationClick: (Int) -> Unit,
    hideFraction: () -> Float = { 0f },
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val fraction = hideFraction()
                    translationY = fraction * (size.height + 32.dp.toPx())
                }.windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 9.dp, vertical = 3.dp)
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border =
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
        tonalElevation = 2.dp,
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(6.dp),
        ) {
            val itemWidth = maxWidth / destinations.size
            val itemWidthPx = with(density) { itemWidth.toPx() }

            val draggableState =
                rememberDraggableState { delta ->
                    val pagerPageSize =
                        pagerState.layoutInfo.pageSize
                            .toFloat()
                            .takeIf { it > 0f }
                            ?: with(density) { maxWidth.toPx() }
                    if (itemWidthPx > 0f) {
                        val scaleFactor = pagerPageSize / itemWidthPx
                        pagerState.dispatchRawDelta(delta * scaleFactor)
                    }
                }

            Box(
                modifier =
                    Modifier
                        .offset {
                            val currentExactPosition =
                                (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                    .coerceIn(0f, (destinations.size - 1).toFloat())
                            val pillOffsetPx = itemWidthPx * currentExactPosition
                            IntOffset(x = pillOffsetPx.roundToInt(), y = 0)
                        }.width(itemWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            onDragStopped = { velocity ->
                                coroutineScope.launch {
                                    val currentExactPosition =
                                        (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                            .coerceIn(0f, (destinations.size - 1).toFloat())

                                    val velocityThreshold = with(density) { 300.dp.toPx() }
                                    val targetPage =
                                        when {
                                            velocity < -velocityThreshold -> ceil(currentExactPosition).toInt()
                                            velocity > velocityThreshold -> floor(currentExactPosition).toInt()
                                            else -> currentExactPosition.roundToInt()
                                        }.coerceIn(0, destinations.size - 1)

                                    pagerState.animateScrollToPage(
                                        page = targetPage,
                                        animationSpec =
                                            tween(
                                                durationMillis = 300,
                                                easing = FastOutSlowInEasing,
                                            ),
                                    )
                                }
                            },
                        ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEachIndexed { index, destination ->
                    BottomBarTabItem(
                        destination = destination,
                        index = index,
                        pagerState = pagerState,
                        onClick = { onDestinationClick(index) },
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarTabItem(
    destination: TopLevelDestination,
    index: Int,
    pagerState: PagerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedColor = MaterialTheme.colorScheme.onPrimaryContainer
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    fun selectionFraction(): Float =
        (
            1f -
                abs(
                    (pagerState.currentPage + pagerState.currentPageOffsetFraction) - index,
                )
        ).coerceIn(0f, 1f)

    // Discrete state that only changes when the selection actually crosses the
    // 0.5 threshold. Reusing derivedStateOf avoids recomposing every frame.
    val isSelected by remember {
        derivedStateOf { selectionFraction() >= 0.5f }
    }

    val currentColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else unselectedColor,
        label = "bottomBarTabColor",
    )

    Column(
        modifier =
            modifier
                .semantics {
                    this.role = Role.Tab
                    this.selected = isSelected
                }.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    onClick()
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .graphicsLayer {
                        val scale = 1.0f + 0.18f * selectionFraction()
                        scaleX = scale
                        scaleY = scale
                        if (destination == TopLevelDestination.SETTINGS) {
                            rotationZ = selectionFraction() * 90f
                        }
                    }.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = stringResource(destination.labelRes),
                tint = currentColor,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = stringResource(destination.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = currentColor,
            maxLines = 1,
            modifier =
                Modifier.graphicsLayer {
                    val scale = 1.0f + 0.08f * selectionFraction()
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SlidingBottomNavigationBarPreview() {
    BetterNightLightTheme {
        val pagerState =
            rememberPagerState(
                initialPage = 1,
                pageCount = { 3 },
            )
        SlidingBottomNavigationBar(
            pagerState = pagerState,
            destinations = TopLevelDestination.entries,
            onDestinationClick = {},
        )
    }
}
