package io.github.paulsnuff.betternightlight.ui.components.status

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.paulsnuff.betternightlight.ui.components.IconBubble
import io.github.paulsnuff.betternightlight.ui.theme.danger
import io.github.paulsnuff.betternightlight.ui.theme.dangerContainer
import io.github.paulsnuff.betternightlight.ui.theme.success
import io.github.paulsnuff.betternightlight.ui.theme.successContainer

@Composable
fun StatusOverviewCard(
    title: String,
    statusText: String,
    description: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
    onIconClick: (() -> Unit)? = null,
) {
    val statusColor =
        if (isPositive) {
            MaterialTheme.colorScheme.success
        } else {
            MaterialTheme.colorScheme.danger
        }
    val iconBgColor =
        if (isPositive) {
            MaterialTheme.colorScheme.successContainer
        } else {
            MaterialTheme.colorScheme.dangerContainer
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconBubble(
                    icon = if (isPositive) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    containerColor = iconBgColor,
                    tint = statusColor,
                    onClick = onIconClick,
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
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
