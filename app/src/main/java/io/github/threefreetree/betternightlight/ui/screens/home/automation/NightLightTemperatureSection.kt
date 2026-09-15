package io.github.threefreetree.betternightlight.ui.screens.home.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.threefreetree.betternightlight.R
import io.github.threefreetree.betternightlight.ui.components.IconBubble
import io.github.threefreetree.betternightlight.ui.theme.BetterNightLightTheme

@Composable
internal fun NightLightTemperatureSection(
    kelvin: Int,
    onKelvinChange: (Int) -> Unit,
    onFieldCoordinatesChange: (LayoutCoordinates?) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KelvinSliderControl(
                value = kelvin,
                onValueChange = onKelvinChange,
                inputLabel = stringResource(R.string.home_automation_temperature_input),
                onFieldCoordinatesChange = onFieldCoordinatesChange,
                leadingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBubble(
                            icon = Icons.Rounded.WbSunny,
                            size = 40.dp,
                            iconSize = 22.dp,
                        )

                        Text(
                            text = stringResource(R.string.home_automation_temperature_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun NightLightTemperatureSectionPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NightLightTemperatureSection(
                kelvin = 3500,
                onKelvinChange = {},
                onFieldCoordinatesChange = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun NightLightTemperatureSectionCoolPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NightLightTemperatureSection(
                kelvin = 2000,
                onKelvinChange = {},
                onFieldCoordinatesChange = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun NightLightTemperatureSectionWarmPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NightLightTemperatureSection(
                kelvin = 5500,
                onKelvinChange = {},
                onFieldCoordinatesChange = {},
            )
        }
    }
}
