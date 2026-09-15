package io.github.threefreetree.betternightlight.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun SelectableCard(
    selected: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
            ),
        border =
            BorderStroke(
                width = if (selected) 1.5.dp else 1.dp,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    },
            ),
        shape = shape,
        content = { content() },
    )
}

@Composable
fun Modifier.selectableRowColors(selected: Boolean): Modifier =
    clip(RoundedCornerShape(12.dp))
        .background(
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ).border(
            width = 1.dp,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
            shape = RoundedCornerShape(12.dp),
        )
