package io.github.paulsnuff.betternightlight.ui.screens.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.paulsnuff.betternightlight.R
import io.github.paulsnuff.betternightlight.data.NightLightStatus
import io.github.paulsnuff.betternightlight.domain.NightLightController
import io.github.paulsnuff.betternightlight.ui.components.nightlightsupport.NightLightSupportCardRoute
import io.github.paulsnuff.betternightlight.ui.components.securesettingspermission.SecureSettingsPermissionCardRoute
import io.github.paulsnuff.betternightlight.ui.theme.BetterNightLightTheme

@Composable
fun AboutRoute(
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AboutScreen(
        uiState = uiState,
        modifier = modifier,
        contentBottomPadding = contentBottomPadding,
    )
}

@Composable
fun AboutScreen(
    uiState: AboutUiState,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
) {
    AboutScreenContent(
        status = uiState.status,
        modifier = modifier,
        contentBottomPadding = contentBottomPadding,
    )
}

@Composable
fun AboutScreenContent(
    status: NightLightStatus,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 20.dp)
                .padding(bottom = contentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        AboutHeader(modifier = Modifier.fillMaxWidth())

        NightLightSupportCardRoute(modifier = Modifier.fillMaxWidth())

        SecureSettingsPermissionCardRoute(modifier = Modifier.fillMaxWidth())

        Card(
            modifier = Modifier.fillMaxWidth(),
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NightLightInfoRow(
                    label = stringResource(R.string.home_night_light_available),
                    value =
                        if (status.isAvailable) {
                            stringResource(R.string.status_yes)
                        } else {
                            stringResource(R.string.status_no)
                        },
                    isHighlight = status.isAvailable,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                NightLightInfoRow(
                    label = stringResource(R.string.home_night_light_status),
                    value =
                        if (status.isActivated) {
                            stringResource(R.string.status_enabled)
                        } else {
                            stringResource(R.string.status_disabled)
                        },
                    isHighlight = status.isActivated,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                val tempText =
                    when {
                        status.temperature != null -> {
                            stringResource(R.string.home_automation_temperature_value, status.temperature)
                        }

                        status.isActivated -> {
                            stringResource(R.string.status_unknown)
                        }

                        else -> {
                            stringResource(R.string.status_not_active)
                        }
                    }

                NightLightInfoRow(
                    label = stringResource(R.string.home_night_light_value),
                    value = tempText,
                    isHighlight = status.isActivated && status.temperature != null,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                val scheduleText =
                    when (status.autoMode) {
                        NightLightController.AUTO_MODE_CUSTOM -> {
                            stringResource(R.string.schedule_custom)
                        }

                        NightLightController.AUTO_MODE_TWILIGHT -> {
                            stringResource(R.string.schedule_twilight)
                        }

                        else -> {
                            stringResource(R.string.schedule_manual)
                        }
                    }

                NightLightInfoRow(
                    label = stringResource(R.string.home_night_light_schedule),
                    value = scheduleText,
                    isHighlight = false,
                )
            }
        }
    }
}

@Composable
private fun AboutHeader(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val githubUrl = stringResource(R.string.about_github_url)
    var showLibraries by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .clip(CircleShape),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_name),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .scale(1.25f),
                contentScale = ContentScale.Crop,
            )
        }

        Button(
            onClick = { uriHandler.openUri(githubUrl) },
            modifier = Modifier.widthIn(min = 220.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.about_source_code))
        }

        OutlinedButton(
            onClick = { showLibraries = true },
            modifier = Modifier.widthIn(min = 220.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.about_libraries))
        }
    }

    if (showLibraries) {
        LibrariesDialog(onDismiss = { showLibraries = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibrariesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val libraries =
        stringResource(R.string.about_libraries_list)
            .split("\n")
            .filter { it.isNotBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.9f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(text = stringResource(R.string.about_libraries_title)) },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                libraries.forEachIndexed { index, library ->
                    Text(
                        text = library,
                        style = MaterialTheme.typography.bodySmall,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        val clipboard =
                                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText("library", library),
                                        )
                                    },
                                ).padding(horizontal = 4.dp, vertical = 10.dp),
                    )
                    if (index != libraries.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.about_close))
            }
        },
    )
}

@Composable
private fun NightLightInfoRow(
    label: String,
    value: String,
    isHighlight: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    BetterNightLightTheme {
        AboutScreen(
            uiState =
                AboutUiState(
                    status =
                        NightLightStatus(
                            isAvailable = true,
                            isActivated = true,
                            temperature = 3200,
                            autoMode = 1,
                        ),
                ),
        )
    }
}
