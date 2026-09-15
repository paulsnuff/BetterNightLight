package io.github.threefreetree.betternightlight.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.threefreetree.betternightlight.R
import io.github.threefreetree.betternightlight.ui.components.IconBubble
import io.github.threefreetree.betternightlight.ui.components.securesettingspermission.SecureSettingsPermissionCard
import io.github.threefreetree.betternightlight.ui.theme.BetterNightLightTheme

@Composable
fun PermissionStep(
    hasSecureSettingsPermission: Boolean,
    isGrantingPermission: Boolean,
    isRootAvailable: Boolean?,
    compatibleHarnessLabels: List<String>,
    contentBottomPadding: Dp,
    onNextStep: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onRequestRootPermission: () -> Unit,
) {
    if (hasSecureSettingsPermission) {
        CenteredCardWithNextButton(
            showButton = true,
            contentBottomPadding = contentBottomPadding,
            onNextStep = onNextStep,
        ) {
            SecureSettingsPermissionCard(
                hasSecureSettingsPermission = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = contentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SecureSettingsPermissionCard(
                hasSecureSettingsPermission = false,
                modifier = Modifier.fillMaxWidth(),
            )
            PermissionGrantPanel(
                isGrantingPermission = isGrantingPermission,
                isRootAvailable = isRootAvailable,
                compatibleHarnessLabels = compatibleHarnessLabels,
                onRequestShizukuPermission = onRequestShizukuPermission,
                onRequestRootPermission = onRequestRootPermission,
            )
        }
    }
}

@Composable
private fun PermissionGrantPanel(
    modifier: Modifier = Modifier,
    isGrantingPermission: Boolean,
    isRootAvailable: Boolean?,
    compatibleHarnessLabels: List<String>,
    onRequestShizukuPermission: () -> Unit,
    onRequestRootPermission: () -> Unit,
) {
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconBubble(
                    icon = Icons.Rounded.LockOpen,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_permission_grant_panel_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.home_permission_grant_panel_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            AdbMethodCard()
            MethodActionCard(
                icon = Icons.Rounded.Security,
                title = stringResource(R.string.home_permission_method_shizuku_title),
                description = stringResource(R.string.home_permission_method_shizuku_desc),
                caption =
                    if (compatibleHarnessLabels.isEmpty()) {
                        null
                    } else {
                        stringResource(
                            R.string.home_permission_detected_harnesses,
                            compatibleHarnessLabels.joinToString(),
                        )
                    },
                buttonLabel = stringResource(R.string.home_permission_request_button),
                enabled = !isGrantingPermission,
                onClick = onRequestShizukuPermission,
            )
            MethodActionCard(
                icon = Icons.Rounded.AdminPanelSettings,
                title = stringResource(R.string.home_permission_method_root_title),
                description = stringResource(R.string.home_permission_method_root_desc),
                caption =
                    when (isRootAvailable) {
                        true -> stringResource(R.string.home_permission_root_detected)
                        false -> stringResource(R.string.home_permission_root_not_detected)
                        null -> null
                    },
                buttonLabel = stringResource(R.string.home_permission_request_button),
                enabled = !isGrantingPermission && isRootAvailable != false,
                onClick = onRequestRootPermission,
            )
        }
    }
}

@Composable
private fun AdbMethodCard(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val command = stringResource(R.string.home_permission_adb_command, context.packageName)
    val copyLabel = stringResource(R.string.home_permission_copy)
    val copiedMessage = stringResource(R.string.home_permission_copied)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.home_permission_method_adb_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = stringResource(R.string.home_permission_method_adb_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = command,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(copyLabel, command))
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = copyLabel,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MethodActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    caption: String? = null,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = buttonLabel)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun PermissionStepGrantedPreview() {
    BetterNightLightTheme {
        PermissionStep(
            hasSecureSettingsPermission = true,
            isGrantingPermission = false,
            isRootAvailable = null,
            compatibleHarnessLabels = emptyList(),
            contentBottomPadding = 0.dp,
            onNextStep = {},
            onRequestShizukuPermission = {},
            onRequestRootPermission = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun PermissionStepDeniedPreview() {
    BetterNightLightTheme {
        PermissionStep(
            hasSecureSettingsPermission = false,
            isGrantingPermission = false,
            isRootAvailable = true,
            compatibleHarnessLabels = listOf("Shizuku"),
            contentBottomPadding = 0.dp,
            onNextStep = {},
            onRequestShizukuPermission = {},
            onRequestRootPermission = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AdbMethodCardPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AdbMethodCard()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MethodActionCardPreview() {
    BetterNightLightTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MethodActionCard(
                icon = Icons.Rounded.Security,
                title = "Shizuku",
                description = "Grant permission via Shizuku service",
                buttonLabel = "Request",
            )
        }
    }
}
