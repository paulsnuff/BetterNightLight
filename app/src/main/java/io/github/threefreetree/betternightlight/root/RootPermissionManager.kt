package io.github.threefreetree.betternightlight.root

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.threefreetree.betternightlight.data.PermissionGate
import io.github.threefreetree.betternightlight.data.SecureSettingsGrantCommands
import io.github.threefreetree.betternightlight.domain.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RootGrantResult {
    data object Granted : RootGrantResult

    data object Denied : RootGrantResult

    data object NotAvailable : RootGrantResult

    data class Failed(
        val reason: String,
    ) : RootGrantResult
}

@Singleton
class RootPermissionManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val permissionGate: PermissionGate,
        private val dispatchers: DispatcherProvider,
        applicationScope: CoroutineScope,
    ) {
        private val packageName = context.packageName
        private val backgroundScope = applicationScope

        private val _isRootAvailable = MutableStateFlow<Boolean?>(null)
        val isRootAvailableFlow: StateFlow<Boolean?> = _isRootAvailable.asStateFlow()

        private val grantCommands = SecureSettingsGrantCommands.shellCommands(packageName)

        fun refreshRootAvailability() {
            backgroundScope.launch {
                _isRootAvailable.value =
                    withContext(dispatchers.io) { detectRootAccess() }
            }
        }

        suspend fun grantWriteSecureSettingsPermission(): RootGrantResult =
            withContext(dispatchers.io) {
                grantInternal()
            }

        private fun grantInternal(): RootGrantResult {
            if (permissionGate.hasWriteSecureSettingsPermission()) {
                return RootGrantResult.Granted
            }

            dropCachedNonRootShell()

            return runCatching {
                var exitCode = -1
                for (cmd in grantCommands) {
                    val execResult = Shell.cmd(cmd).exec()
                    exitCode = execResult.code
                    Log.d(TAG, "grant: [$cmd] exit=$exitCode")
                    if (exitCode == 0) break
                }

                when {
                    exitCode == 0 || permissionGate.hasWriteSecureSettingsPermission() -> {
                        RootGrantResult.Granted
                    }

                    !detectRootAccess() -> {
                        RootGrantResult.NotAvailable
                    }

                    Shell.isAppGrantedRoot() != true -> {
                        RootGrantResult.Denied
                    }

                    else -> {
                        RootGrantResult.Failed("pm grant failed with exit code $exitCode")
                    }
                }
            }.getOrElse { error ->
                Log.w(TAG, "grant: threw ${error.message}", error)
                if (detectRootAccess()) {
                    RootGrantResult.Denied
                } else {
                    RootGrantResult.NotAvailable
                }
            }.also {
                refreshRootAvailability()
            }
        }

        private fun detectRootAccess(): Boolean =
            runCatching {
                Shell.Builder
                    .create()
                    .setFlags(Shell.FLAG_NON_ROOT_SHELL)
                    .setContext(context)
                    .build()
                    .use { shell ->
                        shell
                            .newJob()
                            .add("command -v su")
                            .exec()
                            .isSuccess
                    }
            }.getOrDefault(false)

        private fun dropCachedNonRootShell() {
            val cached = Shell.getCachedShell()
            if (cached != null && !cached.isRoot) {
                Log.d(TAG, "grant: closing cached non-root shell to retry root")
                runCatching { cached.close() }
            }
        }

        companion object {
            private const val TAG = "BnlRoot"
        }
    }
