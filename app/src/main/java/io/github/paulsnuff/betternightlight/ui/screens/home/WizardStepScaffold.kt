package io.github.paulsnuff.betternightlight.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.paulsnuff.betternightlight.R
import io.github.paulsnuff.betternightlight.ui.components.IconBubble
import io.github.paulsnuff.betternightlight.ui.theme.BetterNightLightTheme

@Composable
fun CenteredCardWithNextButton(
    showButton: Boolean,
    contentBottomPadding: Dp,
    onNextStep: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(bottom = contentBottomPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()

        Spacer(modifier = Modifier.height(24.dp))

        // Fixed-size slot keeps the card centered and the layout stable;
        // the button fades/scales in without pushing anything.
        NextButtonSlot(showButton = showButton, onNextStep = onNextStep)
    }
}

@Composable
private fun NextButtonSlot(
    showButton: Boolean,
    onNextStep: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth(0.5f)
                .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = showButton,
            enter =
                fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.85f,
                        animationSpec = tween(250, easing = FastOutSlowInEasing),
                    ),
            exit =
                fadeOut(animationSpec = tween(150)) +
                    scaleOut(targetScale = 0.85f, animationSpec = tween(150)),
            modifier = Modifier.fillMaxSize(),
        ) {
            NextButton(onClick = onNextStep, modifier = Modifier.fillMaxSize())
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun CenteredCardWithNextButtonVisiblePreview() {
    BetterNightLightTheme {
        CenteredCardWithNextButton(
            showButton = true,
            contentBottomPadding = 0.dp,
            onNextStep = {},
        ) {
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
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    IconBubble(
                        icon = Icons.Rounded.CheckCircle,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Night Light Support",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Supported",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun CenteredCardWithNextButtonHiddenPreview() {
    BetterNightLightTheme {
        CenteredCardWithNextButton(
            showButton = false,
            contentBottomPadding = 0.dp,
            onNextStep = {},
        ) {
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
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    IconBubble(
                        icon = Icons.Rounded.CheckCircle,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Night Light Support",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Not Supported",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun NextButtonPreview() {
    BetterNightLightTheme {
        NextButton(onClick = {}, modifier = Modifier.fillMaxWidth(0.5f))
    }
}

@Composable
fun NextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        border =
            BorderStroke(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
            ),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.home_wizard_next),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
        )
    }
}
