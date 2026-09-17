package io.github.threefreetree.betternightlight.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.threefreetree.betternightlight.R
import io.github.threefreetree.betternightlight.data.AppLanguage
import io.github.threefreetree.betternightlight.data.AppThemeMode
import io.github.threefreetree.betternightlight.ui.components.AppSwitch
import io.github.threefreetree.betternightlight.ui.components.IconBubble
import io.github.threefreetree.betternightlight.ui.components.SingleChoiceDialog
import io.github.threefreetree.betternightlight.ui.theme.BetterNightLightTheme

private val AppThemeMode.titleRes: Int
    get() =
        when (this) {
            AppThemeMode.SYSTEM -> R.string.settings_theme_system
            AppThemeMode.LIGHT -> R.string.settings_theme_light
            AppThemeMode.DARK -> R.string.settings_theme_dark
        }

private val AppLanguage.titleRes: Int
    get() =
        when (this) {
            AppLanguage.AUTO -> R.string.settings_language_auto
            AppLanguage.ENGLISH -> R.string.settings_language_en
            AppLanguage.POLISH -> R.string.settings_language_pl
        }

@Composable
fun SettingsRoute(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onThemeModeChange = viewModel::updateThemeMode,
        onDynamicColorChange = viewModel::updateDynamicColor,
        onLanguageChange = viewModel::updateLanguage,
        modifier = modifier,
        contentBottomPadding = contentBottomPadding,
    )
}

@Composable
fun SettingsScreen(
    uiState: AppUiState,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 20.dp)
                .padding(bottom = contentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Subsection: Customize
        Text(
            text = stringResource(R.string.settings_section_customize),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        // 1. Language setting tile
        SettingsTile(
            icon = Icons.Rounded.Language,
            title = stringResource(R.string.settings_language_title),
            subtitle = stringResource(uiState.language.titleRes),
            onClick = { showLanguageDialog = true },
        )

        // 2. Theme setting tile
        SettingsTile(
            icon = Icons.Rounded.Palette,
            title = stringResource(R.string.settings_theme_title),
            subtitle = stringResource(uiState.themeMode.titleRes),
            onClick = { showThemeDialog = true },
        )

        // 3. Material You toggle card tile (only on Android 12+, where dynamic color is supported)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onDynamicColorChange(!uiState.dynamicColor) },
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                border =
                    BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBubble(
                        icon = Icons.Rounded.AutoAwesome,
                        size = 44.dp,
                        iconSize = 24.dp,
                    )
                    Text(
                        text = stringResource(R.string.settings_material_you_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    AppSwitch(
                        checked = uiState.dynamicColor,
                        onCheckedChange = onDynamicColorChange,
                    )
                }
            }
        }
    }

    // Language Selection Dialog Popup
    if (showLanguageDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_language_title),
            icon = Icons.Rounded.Language,
            options = AppLanguage.entries,
            selected = uiState.language,
            optionLabel = { stringResource(it.titleRes) },
            onSelect = {
                onLanguageChange(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    // Theme Selection Dialog Popup
    if (showThemeDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_theme_title),
            icon = Icons.Rounded.Palette,
            options = AppThemeMode.entries,
            selected = uiState.themeMode,
            optionLabel = { stringResource(it.titleRes) },
            onSelect = {
                onThemeModeChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }
}

@Composable
private fun SettingsTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBubble(
                icon = icon,
                size = 44.dp,
                iconSize = 24.dp,
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
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    BetterNightLightTheme {
        SettingsScreen(
            uiState =
                AppUiState(
                    themeMode = AppThemeMode.SYSTEM,
                    dynamicColor = true,
                    language = AppLanguage.AUTO,
                ),
            onThemeModeChange = {},
            onDynamicColorChange = {},
            onLanguageChange = {},
        )
    }
}
