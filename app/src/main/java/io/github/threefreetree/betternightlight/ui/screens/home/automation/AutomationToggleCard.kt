package io.github.threefreetree.betternightlight.ui.screens.home.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.threefreetree.betternightlight.R
import io.github.threefreetree.betternightlight.ui.components.AppSwitch
import io.github.threefreetree.betternightlight.ui.components.IconBubble
import io.github.threefreetree.betternightlight.ui.theme.BetterNightLightTheme

@Composable
internal fun AutomationToggleCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
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
                    .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBubble(
                icon = Icons.Rounded.BrightnessAuto,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_automation_enable_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            AppSwitch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AutomationToggleCardEnabledPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AutomationToggleCard(enabled = true, onEnabledChange = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AutomationToggleCardDisabledPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AutomationToggleCard(enabled = false, onEnabledChange = {})
        }
    }
}
