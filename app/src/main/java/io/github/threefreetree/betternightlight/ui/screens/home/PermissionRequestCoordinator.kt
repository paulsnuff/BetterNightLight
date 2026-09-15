package io.github.threefreetree.betternightlight.ui.screens.home

import io.github.threefreetree.betternightlight.data.PermissionGate
import io.github.threefreetree.betternightlight.root.RootGrantResult
import io.github.threefreetree.betternightlight.root.RootPermissionManager
import io.github.threefreetree.betternightlight.shizuku.ShizukuGrantResult
import io.github.threefreetree.betternightlight.shizuku.ShizukuPermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class PermissionRequestCoordinator
    @Inject
    constructor(
        private val shizukuPermissionManager: ShizukuPermissionManager,
        private val rootPermissionManager: RootPermissionManager,
        private val permissionGate: PermissionGate,
    ) {
        private val _isGrantingPermission = MutableStateFlow(false)
        val isGrantingPermission: StateFlow<Boolean> = _isGrantingPermission.asStateFlow()
        val isRootAvailable: StateFlow<Boolean?> = rootPermissionManager.isRootAvailableFlow

        fun installedHarnessLabels(): List<String> = shizukuPermissionManager.detectInstalledHarnesses().map { it.label }

        fun refreshRootAvailability() {
            rootPermissionManager.refreshRootAvailability()
        }

        suspend fun requestShizukuPermission(onMessage: suspend (HomeMessage) -> Unit) {
            if (!beginGrant()) return
            try {
                val result =
                    try {
                        shizukuPermissionManager.grantWriteSecureSettingsPermission()
                    } catch (e: Throwable) {
                        ShizukuGrantResult.Failed(e.message ?: e.javaClass.simpleName)
                    }
                onMessage(
                    when (result) {
                        is ShizukuGrantResult.Granted -> HomeMessage.PermissionShizukuSuccess
                        is ShizukuGrantResult.Cancelled -> HomeMessage.PermissionShizukuCancelled
                        is ShizukuGrantResult.NotInstalled -> HomeMessage.PermissionShizukuNotInstalled
                        is ShizukuGrantResult.NotRunning -> HomeMessage.PermissionShizukuNotRunning
                        is ShizukuGrantResult.Unsupported -> HomeMessage.PermissionShizukuUnsupported
                        is ShizukuGrantResult.Failed -> HomeMessage.PermissionShizukuFailed
                    },
                )
            } finally {
                endGrant()
            }
        }

        suspend fun requestRootPermission(onMessage: suspend (HomeMessage) -> Unit) {
            if (!beginGrant()) return
            try {
                onMessage(
                    when (rootPermissionManager.grantWriteSecureSettingsPermission()) {
                        is RootGrantResult.Granted -> HomeMessage.PermissionRootSuccess
                        is RootGrantResult.Denied -> HomeMessage.PermissionRootDenied
                        is RootGrantResult.NotAvailable -> HomeMessage.PermissionRootNotAvailable
                        is RootGrantResult.Failed -> HomeMessage.PermissionRootFailed
                    },
                )
            } finally {
                endGrant()
            }
        }

        private fun beginGrant(): Boolean = _isGrantingPermission.compareAndSet(expect = false, update = true)

        private fun endGrant() {
            _isGrantingPermission.value = false
            permissionGate.updatePermissionState()
        }
    }
