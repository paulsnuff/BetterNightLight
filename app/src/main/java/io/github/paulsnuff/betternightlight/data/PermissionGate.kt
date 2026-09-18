package io.github.paulsnuff.betternightlight.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionGate
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val _permissionMissing = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val permissionMissing: SharedFlow<Unit> = _permissionMissing.asSharedFlow()
        private val _hasPermission = MutableStateFlow(hasWriteSecureSettingsPermission())
        val hasPermissionFlow: StateFlow<Boolean> = _hasPermission.asStateFlow()

        fun hasWriteSecureSettingsPermission(): Boolean = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

        fun updatePermissionState(): Boolean {
            val granted = hasWriteSecureSettingsPermission()
            _hasPermission.value = granted
            return granted
        }

        fun ensurePermission(): Boolean {
            val granted = updatePermissionState()
            if (!granted) {
                _permissionMissing.tryEmit(Unit)
            }
            return granted
        }
    }
