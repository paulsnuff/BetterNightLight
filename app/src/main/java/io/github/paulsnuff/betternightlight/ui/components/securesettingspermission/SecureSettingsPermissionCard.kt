package io.github.paulsnuff.betternightlight.ui.components.securesettingspermission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.github.paulsnuff.betternightlight.R
import io.github.paulsnuff.betternightlight.ui.components.status.StatusOverviewCard
import io.github.paulsnuff.betternightlight.ui.theme.BetterNightLightTheme

@Composable
fun SecureSettingsPermissionCardRoute(modifier: Modifier = Modifier) {
    val isInspectionMode = LocalInspectionMode.current
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    if (isInspectionMode || viewModelStoreOwner == null) {
        SecureSettingsPermissionCard(hasSecureSettingsPermission = true, modifier = modifier)
        return
    }

    val viewModel: SecureSettingsPermissionViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermission()
        onPauseOrDispose { }
    }

    SecureSettingsPermissionCard(
        hasSecureSettingsPermission = uiState.hasSecureSettingsPermission,
        modifier = modifier,
    )
}

@Composable
fun SecureSettingsPermissionCard(
    hasSecureSettingsPermission: Boolean,
    modifier: Modifier = Modifier,
) {
    StatusOverviewCard(
        title = stringResource(R.string.home_permission_title),
        statusText =
            stringResource(
                if (hasSecureSettingsPermission) {
                    R.string.home_permission_granted
                } else {
                    R.string.home_permission_denied
                },
            ),
        description =
            stringResource(
                if (hasSecureSettingsPermission) {
                    R.string.home_permission_granted_desc
                } else {
                    R.string.home_permission_denied_desc
                },
            ),
        isPositive = hasSecureSettingsPermission,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun SecureSettingsPermissionCardGrantedPreview() {
    BetterNightLightTheme {
        SecureSettingsPermissionCard(hasSecureSettingsPermission = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun SecureSettingsPermissionCardDeniedPreview() {
    BetterNightLightTheme {
        SecureSettingsPermissionCard(hasSecureSettingsPermission = false)
    }
}
