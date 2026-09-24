package io.github.paulsnuff.betternightlight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.paulsnuff.betternightlight.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val MESSAGE_DURATION = 4000.milliseconds
private const val MESSAGE_ANIMATION_MS = 220

internal data class InAppMessageData(
    val id: Int,
    val text: String,
)

class InAppMessageState {
    internal var message: InAppMessageData? by mutableStateOf(null)
        private set

    private var nextId = 0

    fun show(text: String) {
        nextId += 1
        message = InAppMessageData(id = nextId, text = text)
    }

    fun dismiss() {
        message = null
    }
}

@Composable
fun rememberInAppMessageState(): InAppMessageState = remember { InAppMessageState() }

@Composable
fun InAppMessageHost(
    state: InAppMessageState,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
) {
    var lastMessage by remember { mutableStateOf<InAppMessageData?>(null) }
    var isHeld by remember { mutableStateOf(false) }
    var interactionEpoch by remember { mutableIntStateOf(0) }
    val currentMessage = state.message
    if (currentMessage != null) {
        lastMessage = currentMessage
    }

    LaunchedEffect(currentMessage?.id, interactionEpoch, isHeld) {
        if (currentMessage != null && !isHeld) {
            delay(MESSAGE_DURATION)
            state.dismiss()
        }
    }

    AnimatedVisibility(
        visible = currentMessage != null,
        enter =
            fadeIn(animationSpec = tween(MESSAGE_ANIMATION_MS, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    animationSpec = tween(MESSAGE_ANIMATION_MS, easing = FastOutSlowInEasing),
                ) { -it },
        exit = slideOutVertically(animationSpec = tween(MESSAGE_ANIMATION_MS)) { -it },
        modifier = modifier,
    ) {
        key(lastMessage?.id ?: 0) {
            DragToDismiss(
                onDismiss = state::dismiss,
                onDragStarted = { isHeld = true },
                onDragEnded = { dismissed ->
                    isHeld = false
                    if (!dismissed) {
                        interactionEpoch++
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                InAppMessageCard(
                    message = lastMessage?.text.orEmpty(),
                    icon = icon,
                    onClose = state::dismiss,
                )
            }
        }
    }
}

@Composable
private fun InAppMessageCard(
    message: String,
    icon: ImageVector,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}