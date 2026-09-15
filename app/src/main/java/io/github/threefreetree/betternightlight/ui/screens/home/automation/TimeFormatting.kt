package io.github.threefreetree.betternightlight.ui.screens.home.automation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.threefreetree.betternightlight.domain.SunTimesCalculator
import io.github.threefreetree.betternightlight.domain.SunTimesInfo
import io.github.threefreetree.betternightlight.domain.currentDateFlow
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

@Composable
internal fun rememberSunTimes(
    latitude: Double?,
    longitude: Double?,
): SunTimesInfo? {
    val today by currentDateFlow().collectAsStateWithLifecycle(LocalDate.now())
    val zone = remember { ZoneId.systemDefault() }
    return remember(latitude, longitude, today, zone) {
        if (latitude != null && longitude != null) {
            SunTimesCalculator.compute(latitude, longitude, today, zone)
        } else {
            null
        }
    }
}

internal fun formatTime(
    hour: Int,
    minute: Int,
): String = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

internal fun formatSunTime(
    dateTime: ZonedDateTime?,
    unavailable: String,
): String =
    dateTime?.let {
        String.format(Locale.getDefault(), "%02d:%02d", it.hour, it.minute)
    } ?: unavailable

internal fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.2f", value)
