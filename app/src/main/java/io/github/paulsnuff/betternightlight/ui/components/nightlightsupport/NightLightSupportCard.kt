package io.github.paulsnuff.betternightlight.ui.components.nightlightsupport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.github.paulsnuff.betternightlight.R
import io.github.paulsnuff.betternightlight.ui.components.status.StatusOverviewCard
import io.github.paulsnuff.betternightlight.ui.theme.BetterNightLightTheme

@Composable
fun NightLightSupportCardRoute(modifier: Modifier = Modifier) {
    val isInspectionMode = LocalInspectionMode.current
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    if (isInspectionMode || viewModelStoreOwner == null) {
        NightLightSupportCard(isNightLightSupported = true, modifier = modifier)
        return
    }

    val viewModel: NightLightSupportViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NightLightSupportCard(
        isNightLightSupported = uiState.isNightLightSupported,
        modifier = modifier,
    )
}

@Composable
fun NightLightSupportCard(
    isNightLightSupported: Boolean,
    modifier: Modifier = Modifier,
    onSecretUnlock: (() -> Unit)? = null,
) {
    StatusOverviewCard(
        title = stringResource(R.string.home_night_light_support_title),
        statusText =
            stringResource(
                if (isNightLightSupported) {
                    R.string.home_night_light_supported
                } else {
                    R.string.home_night_light_not_supported
                },
            ),
        description =
            stringResource(
                if (isNightLightSupported) {
                    R.string.home_night_light_supported_desc
                } else {
                    R.string.home_night_light_not_supported_desc
                },
            ),
        isPositive = isNightLightSupported,
        // The hidden 10-tap easter egg only reacts on the "not supported" icon.
        onIconClick = if (!isNightLightSupported) onSecretUnlock else null,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun NightLightSupportCardSupportedPreview() {
    BetterNightLightTheme {
        NightLightSupportCard(isNightLightSupported = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun NightLightSupportCardNotSupportedPreview() {
    BetterNightLightTheme {
        NightLightSupportCard(isNightLightSupported = false)
    }
}
