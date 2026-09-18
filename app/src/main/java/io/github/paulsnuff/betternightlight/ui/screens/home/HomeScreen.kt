package io.github.paulsnuff.betternightlight.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.paulsnuff.betternightlight.R
import io.github.paulsnuff.betternightlight.ui.theme.BetterNightLightTheme

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentContext by rememberUpdatedState(context)

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermission()
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast
                .makeText(
                    currentContext,
                    currentContext.getString(message.textRes),
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    val actions =
        remember(viewModel) {
            AutomationActions(
                onAutomationEnabledChange = viewModel::setAutomationEnabled,
                onTriggerChange = viewModel::setAutomationTrigger,
                onManualStartTimeChange = viewModel::setManualStartTime,
                onManualEndTimeChange = viewModel::setManualEndTime,
                onLocationSourceChange = viewModel::setLocationSource,
                onCoordinatesChange = viewModel::setCoordinates,
                onNightLightTemperatureChange = viewModel::setNightLightTemperature,
                onRequestDeviceLocation = viewModel::fetchDeviceLocation,
                onLocationPermissionDenied = viewModel::onLocationPermissionDenied,
                onBoostEnabledChange = viewModel::setBoostEnabled,
                onBoostTimeChange = viewModel::setBoostTime,
                onBoostKelvinChange = viewModel::setBoostKelvin,
                onPhaseTransitionChange = viewModel::setPhaseTransition,
                onOffEnabledChange = viewModel::setOffEnabled,
                onOffTimeChange = viewModel::setOffTime,
            )
        }

    HomeScreen(
        uiState = uiState,
        onNextStep = viewModel::goToNextStep,
        onRequestShizukuPermission = viewModel::requestShizukuPermission,
        onRequestRootPermission = viewModel::requestRootPermission,
        actions = actions,
        modifier = modifier,
        contentBottomPadding = contentBottomPadding,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    onNextStep: () -> Unit = {},
    onRequestShizukuPermission: () -> Unit = {},
    onRequestRootPermission: () -> Unit = {},
    actions: AutomationActions = AutomationActions(),
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.97f,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                        )
                ).togetherWith(
                    fadeOut(animationSpec = tween(150)) +
                        scaleOut(
                            targetScale = 0.97f,
                            animationSpec = tween(150),
                        ),
                )
            },
            label = "homeWizardStep",
        ) { step ->
            when (step) {
                null -> {}

                HomeWizardStep.SUPPORT -> {
                    NightLightSupportStep(
                        isSupported = uiState.isNightLightSupported,
                        contentBottomPadding = contentBottomPadding,
                        onNextStep = onNextStep,
                    )
                }

                HomeWizardStep.PERMISSION -> {
                    PermissionStep(
                        hasSecureSettingsPermission = uiState.hasSecureSettingsPermission,
                        isGrantingPermission = uiState.isGrantingPermission,
                        isRootAvailable = uiState.isRootAvailable,
                        compatibleHarnessLabels = uiState.compatibleHarnessLabels,
                        contentBottomPadding = contentBottomPadding,
                        onNextStep = onNextStep,
                        onRequestShizukuPermission = onRequestShizukuPermission,
                        onRequestRootPermission = onRequestRootPermission,
                    )
                }

                HomeWizardStep.AUTOMATION -> {
                    AutomationStep(
                        schedule = uiState.automationSchedule,
                        currentKelvin = uiState.currentKelvin,
                        contentBottomPadding = contentBottomPadding,
                        actions = actions,
                        isFetchingLocation = uiState.isFetchingDeviceLocation,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenSupportStepSupportedPreview() {
    BetterNightLightTheme {
        HomeScreen(
            uiState =
                HomeUiState(
                    isNightLightSupported = true,
                    currentStep = HomeWizardStep.SUPPORT,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenSupportStepNotSupportedPreview() {
    BetterNightLightTheme {
        HomeScreen(
            uiState =
                HomeUiState(
                    isNightLightSupported = false,
                    currentStep = HomeWizardStep.SUPPORT,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPermissionGrantedPreview() {
    BetterNightLightTheme {
        HomeScreen(
            uiState =
                HomeUiState(
                    isNightLightSupported = true,
                    hasSecureSettingsPermission = true,
                    currentStep = HomeWizardStep.PERMISSION,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPermissionDeniedPreview() {
    BetterNightLightTheme {
        HomeScreen(
            uiState =
                HomeUiState(
                    isNightLightSupported = true,
                    hasSecureSettingsPermission = false,
                    currentStep = HomeWizardStep.PERMISSION,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenAutomationStepPreview() {
    BetterNightLightTheme {
        HomeScreen(
            uiState =
                HomeUiState(
                    isNightLightSupported = true,
                    hasSecureSettingsPermission = true,
                    currentStep = HomeWizardStep.AUTOMATION,
                ),
        )
    }
}
