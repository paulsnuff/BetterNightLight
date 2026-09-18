package io.github.paulsnuff.betternightlight.ui.screens.home

import androidx.annotation.StringRes
import io.github.paulsnuff.betternightlight.R

sealed interface HomeMessage {
    @get:StringRes
    val textRes: Int

    data object PermissionShizukuSuccess : HomeMessage {
        override val textRes get() = R.string.home_permission_shizuku_success
    }

    data object PermissionShizukuCancelled : HomeMessage {
        override val textRes get() = R.string.home_permission_shizuku_cancelled
    }

    data object PermissionShizukuNotInstalled : HomeMessage {
        override val textRes get() = R.string.home_permission_shizuku_not_installed
    }

    data object PermissionShizukuNotRunning : HomeMessage {
        override val textRes get() = R.string.home_permission_shizuku_not_running
    }

    data object PermissionShizukuUnsupported : HomeMessage {
        override val textRes get() = R.string.home_permission_shizuku_unsupported
    }

    data object PermissionShizukuFailed : HomeMessage {
        override val textRes get() = R.string.home_permission_shizuku_failed
    }

    data object PermissionRootSuccess : HomeMessage {
        override val textRes get() = R.string.home_permission_root_success
    }

    data object PermissionRootDenied : HomeMessage {
        override val textRes get() = R.string.home_permission_root_denied
    }

    data object PermissionRootNotAvailable : HomeMessage {
        override val textRes get() = R.string.home_permission_root_not_available
    }

    data object PermissionRootFailed : HomeMessage {
        override val textRes get() = R.string.home_permission_root_failed
    }

    data object LocationUnavailable : HomeMessage {
        override val textRes get() = R.string.home_automation_location_unavailable
    }

    data object LocationPermissionDenied : HomeMessage {
        override val textRes get() = R.string.home_automation_location_permission_denied
    }
}
