package io.github.paulsnuff.betternightlight.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Semantic status colors for the status cards. Derived from the current
 * [ColorScheme] luminance so they track light/dark theme without extra
 * CompositionLocals, while remaining static per scheme.
 */
private val SuccessLight = Color(0xFF2E7D32)
private val SuccessDark = Color(0xFF81C784)
private val DangerLight = Color(0xFFD32F2F)
private val DangerDark = Color(0xFFFF7575)

private val SuccessContainerLight = Color(0x202E7D32)
private val SuccessContainerDark = Color(0x3081C784)
private val DangerContainerLight = Color(0x20D32F2F)
private val DangerContainerDark = Color(0x30EF5350)

private val ColorScheme.isLight: Boolean
    get() = background.luminance() > 0.5f

/** Green accent for positive states (e.g. "supported", "granted"). */
val ColorScheme.success: Color
    get() = if (isLight) SuccessLight else SuccessDark

/** Red accent for negative states (e.g. "not supported", "denied"). */
val ColorScheme.danger: Color
    get() = if (isLight) DangerLight else DangerDark

/** Translucent background for the status icon bubble in the positive state. */
val ColorScheme.successContainer: Color
    get() = if (isLight) SuccessContainerLight else SuccessContainerDark

/** Translucent background for the status icon bubble in the negative state. */
val ColorScheme.dangerContainer: Color
    get() = if (isLight) DangerContainerLight else DangerContainerDark
