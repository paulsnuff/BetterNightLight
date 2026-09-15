package io.github.threefreetree.betternightlight.ui.screens.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.threefreetree.betternightlight.ui.components.nightlightsupport.NightLightSupportCard
import io.github.threefreetree.betternightlight.ui.theme.BetterNightLightTheme

@Composable
fun NightLightSupportStep(
    isSupported: Boolean,
    contentBottomPadding: Dp,
    onNextStep: () -> Unit,
) {
    var secretTapCount by remember { mutableIntStateOf(0) }
    var lastSecretTap by remember { mutableLongStateOf(0L) }
    var secretUnlocked by remember { mutableStateOf(false) }

    val onSecretTap: () -> Unit = {
        if (!secretUnlocked) {
            val now = System.currentTimeMillis()
            secretTapCount =
                if (now - lastSecretTap <= 2_000) secretTapCount + 1 else 1
            lastSecretTap = now
            if (secretTapCount >= 10) {
                secretUnlocked = true
            }
        }
    }

    CenteredCardWithNextButton(
        showButton = isSupported || secretUnlocked,
        contentBottomPadding = contentBottomPadding,
        onNextStep = onNextStep,
    ) {
        NightLightSupportCard(
            isNightLightSupported = isSupported,
            onSecretUnlock = onSecretTap,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun NightLightSupportStepSupportedPreview() {
    BetterNightLightTheme {
        NightLightSupportStep(
            isSupported = true,
            contentBottomPadding = 0.dp,
            onNextStep = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun NightLightSupportStepNotSupportedPreview() {
    BetterNightLightTheme {
        NightLightSupportStep(
            isSupported = false,
            contentBottomPadding = 0.dp,
            onNextStep = {},
        )
    }
}
