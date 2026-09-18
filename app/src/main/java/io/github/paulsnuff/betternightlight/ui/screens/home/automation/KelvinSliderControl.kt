package io.github.paulsnuff.betternightlight.ui.screens.home.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.paulsnuff.betternightlight.R
import io.github.paulsnuff.betternightlight.domain.TEMPERATURE_MAX_KELVIN
import io.github.paulsnuff.betternightlight.domain.TEMPERATURE_MIN_KELVIN
import io.github.paulsnuff.betternightlight.ui.theme.BetterNightLightTheme
import kotlin.math.roundToInt

@Composable
internal fun KelvinSliderControl(
    value: Int,
    onValueChange: (Int) -> Unit,
    inputLabel: String,
    onFieldCoordinatesChange: ((LayoutCoordinates?) -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current

    var sliderValue by rememberSaveable(value) {
        mutableFloatStateOf(value.toFloat())
    }
    var textValue by rememberSaveable {
        mutableStateOf(value.toString())
    }
    var isEditingText by remember { mutableStateOf(false) }
    LaunchedEffect(value, isEditingText) {
        if (!isEditingText && textValue.toIntOrNull() != value) {
            textValue = value.toString()
        }
    }

    fun commitText() {
        val parsed =
            textValue
                .replace(',', '.')
                .toIntOrNull()
                ?.coerceIn(TEMPERATURE_MIN_KELVIN, TEMPERATURE_MAX_KELVIN)
        if (parsed != null) {
            sliderValue = parsed.toFloat()
            textValue = parsed.toString()
            onValueChange(parsed)
        } else {
            textValue = value.toString()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leadingContent != null) {
                Box(modifier = Modifier.weight(1f)) {
                    leadingContent()
                }
            }

            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    isEditingText = true
                    textValue = it
                },
                label = { Text(inputLabel) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        },
                    ),
                modifier =
                    Modifier
                        .width(110.dp)
                        .then(
                            if (onFieldCoordinatesChange != null) {
                                Modifier.onGloballyPositioned { onFieldCoordinatesChange(it) }
                            } else {
                                Modifier
                            },
                        ).onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                isEditingText = false
                                commitText()
                            }
                        },
            )
        }

        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                textValue = it.roundToInt().toString()
            },
            onValueChangeFinished = {
                onValueChange(sliderValue.roundToInt())
            },
            valueRange = TEMPERATURE_MIN_KELVIN.toFloat()..TEMPERATURE_MAX_KELVIN.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text =
                    stringResource(
                        R.string.home_automation_temperature_value,
                        TEMPERATURE_MIN_KELVIN,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    stringResource(
                        R.string.home_automation_temperature_value,
                        sliderValue.roundToInt(),
                    ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text =
                    stringResource(
                        R.string.home_automation_temperature_value,
                        TEMPERATURE_MAX_KELVIN,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun KelvinSliderControlDefaultPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            KelvinSliderControl(
                value = 3500,
                onValueChange = {},
                inputLabel = "Kelvin",
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun KelvinSliderControlCoolPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            KelvinSliderControl(
                value = 2000,
                onValueChange = {},
                inputLabel = "Kelvin",
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun KelvinSliderControlWarmPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            KelvinSliderControl(
                value = 5500,
                onValueChange = {},
                inputLabel = "Kelvin",
            )
        }
    }
}
