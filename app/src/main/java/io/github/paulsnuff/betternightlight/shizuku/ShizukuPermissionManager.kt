package io.github.paulsnuff.betternightlight.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.paulsnuff.betternightlight.data.PermissionGate
import io.github.paulsnuff.betternightlight.data.SecureSettingsGrantCommands
import io.github.paulsnuff.betternightlight.domain.DispatcherProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

sealed interface ShizukuGrantResult {
    data object Granted : ShizukuGrantResult

    data object Cancelled : ShizukuGrantResult

    data object NotInstalled : ShizukuGrantResult

    data object NotRunning : ShizukuGrantResult

    data object Unsupported : ShizukuGrantResult

    data class Failed(
        val reason: String,
    ) : ShizukuGrantResult
}

data class CompatHarness(
    val packageName: String,
    val label: String,
)

@Singleton
class ShizukuPermissionManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val permissionGate: PermissionGate,
        private val dispatchers: DispatcherProvider,
        applicationScope: CoroutineScope,
    ) {
        private val packageName = context.packageName
        private val backgroundScope = applicationScope
        private val grantInFlight = AtomicBoolean(false)

        fun tryGrantSilentlyInBackground() {
            if (permissionGate.updatePermissionState()) {
                return
            }
            if (!grantInFlight.compareAndSet(false, true)) {
                Log.d(TAG, "Auto/grant: already in flight, skipping")
                return
            }
            backgroundScope.launch {
                try {
                    Log.d(TAG, "Auto/grant: background attempt starts")
                    val result = tryGrantWriteSecureSettingsSilently()
                    Log.i(TAG, "Auto/grant: background result -> $result")
                    permissionGate.updatePermissionState()
                } catch (e: Throwable) {
                    Log.w(TAG, "Auto/grant: background attempt threw", e)
                } finally {
                    grantInFlight.set(false)
                }
            }
        }

        suspend fun grantWriteSecureSettingsPermission(): ShizukuGrantResult = grantWriteSecureSettingsPermission(requestServicePermission = true)

        suspend fun tryGrantWriteSecureSettingsSilently(): ShizukuGrantResult = grantWriteSecureSettingsPermission(requestServicePermission = false)

        private suspend fun grantWriteSecureSettingsPermission(requestServicePermission: Boolean): ShizukuGrantResult {
            if (permissionGate.updatePermissionState()) {
                return ShizukuGrantResult.Granted
            }
            val installed = detectInstalledHarnesses()
            Log.d(TAG, "Auto/grant: installed=${installed.joinToString { it.label }}")

            if (!awaitBinder()) {
                Log.d(TAG, "Auto/grant: binder not ready")
                return if (installed.isEmpty()) {
                    ShizukuGrantResult.NotInstalled
                } else {
                    ShizukuGrantResult.NotRunning
                }
            }
            Log.d(TAG, "Auto/grant: binder ready")

            if (Shizuku.isPreV11()) {
                Log.d(TAG, "Auto/grant: preV11")
                return ShizukuGrantResult.Unsupported
            }

            val hasServicePermission =
                try {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } catch (e: Throwable) {
                    Log.w(TAG, "Auto/grant: checkSelfPermission threw ${e.message}")
                    false
                }

            val serviceVersion =
                runCatching { Shizuku.getVersion() }.getOrElse { error ->
                    if (error is SecurityException) 11 else 0
                }
            Log.d(TAG, "Auto/grant: hasServicePermission=$hasServicePermission version=$serviceVersion")

            if (serviceVersion in 1 until MIN_SUPPORTED_SHIZUKU_VERSION) {
                return ShizukuGrantResult.Unsupported
            }

            if (requestServicePermission && !hasServicePermission) {
                if (runCatching { Shizuku.shouldShowRequestPermissionRationale() }.getOrDefault(false)) {
                    return ShizukuGrantResult.Cancelled
                }
                if (!awaitPermissionGranted()) {
                    return ShizukuGrantResult.Cancelled
                }
            } else if (!hasServicePermission) {
                Log.d(TAG, "Auto/grant: no runtime service permission, attempting via config")
            }

            val result = runWithWorker()
            permissionGate.updatePermissionState()
            return result
        }

        fun detectInstalledHarnesses(): List<CompatHarness> =
            KNOWN_HARNESSES.mapNotNull { (packageName, label) ->
                runCatching {
                    context.packageManager.getPackageInfo(packageName, 0)
                    CompatHarness(packageName, label)
                }.getOrNull()
            }

        private suspend fun awaitBinder(): Boolean {
            if (runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
                return true
            }
            val received = CompletableDeferred<Unit>()
            val listener =
                Shizuku.OnBinderReceivedListener {
                    if (!received.isCompleted) received.complete(Unit)
                }
            try {
                runCatching { Shizuku.addBinderReceivedListenerSticky(listener) }
                    .onFailure { received.completeExceptionally(it) }

                return withTimeoutOrNull(BINDER_WAIT_TIMEOUT) {
                    received.await()
                    true
                } ?: false
            } finally {
                runCatching { Shizuku.removeBinderReceivedListener(listener) }
            }
        }

        private suspend fun awaitPermissionGranted(): Boolean {
            val deferred = CompletableDeferred<Boolean>()
            val listener =
                Shizuku.OnRequestPermissionResultListener { code, result ->
                    if (code == PERMISSION_REQUEST_CODE && !deferred.isCompleted) {
                        deferred.complete(result == PackageManager.PERMISSION_GRANTED)
                    }
                }
            Shizuku.addRequestPermissionResultListener(listener)
            return try {
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
                withTimeoutOrNull(OPERATION_TIMEOUT) { deferred.await() } ?: false
            } finally {
                Shizuku.removeRequestPermissionResultListener(listener)
                runCatching { Shizuku.checkSelfPermission() }
            }
        }

        private suspend fun runWithWorker(): ShizukuGrantResult =
            withContext(dispatchers.io) {
                val uniqueTag =
                    "worker_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
                val args =
                    Shizuku
                        .UserServiceArgs(
                            ComponentName(packageName, ShizukuPermissionWorker::class.java.name),
                        ).daemon(false)
                        .tag(uniqueTag)
                        .processNameSuffix("permission")
                        .version(SERVICE_VERSION)

                val deferred = CompletableDeferred<ShizukuGrantResult>()

                val connection =
                    object : ServiceConnection {
                        override fun onServiceConnected(
                            name: ComponentName?,
                            binder: IBinder?,
                        ) {
                            if (deferred.isCompleted) return
                            if (binder == null || !binder.pingBinder()) {
                                deferred.complete(ShizukuGrantResult.Failed("worker is not available"))
                                return
                            }
                            val worker = IShizukuWorker.Stub.asInterface(binder)
                            backgroundScope.launch {
                                val result =
                                    runCatching {
                                        withContext(dispatchers.io) { doGrantWithWorker(worker) }
                                    }.getOrElse {
                                        ShizukuGrantResult.Failed(
                                            it.message ?: it.javaClass.simpleName,
                                        )
                                    }
                                if (!deferred.isCompleted) {
                                    deferred.complete(result)
                                }
                            }
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            if (!deferred.isCompleted) {
                                deferred.complete(ShizukuGrantResult.Failed("worker disconnected"))
                            }
                        }
                    }

                try {
                    Shizuku.bindUserService(args, connection)
                    withTimeoutOrNull(OPERATION_TIMEOUT) { deferred.await() }
                        ?: ShizukuGrantResult.Failed("operation timed out")
                } catch (e: Throwable) {
                    Log.w(TAG, "Auto/grant: bindUserService failed", e)
                    ShizukuGrantResult.Failed(e.message ?: e.javaClass.simpleName)
                } finally {
                    runCatching {
                        Shizuku.unbindUserService(args, connection, false)
                        Shizuku.unbindUserService(args, null, true)
                    }
                }
            }

        private fun doGrantWithWorker(worker: IShizukuWorker): ShizukuGrantResult {
            val commands = SecureSettingsGrantCommands.workerCommands(packageName)

            var lastExitCode = -1
            for (cmd in commands) {
                lastExitCode = worker.exec(cmd)
                Log.d(TAG, "Auto/grant: ${cmd.joinToString(" ")} exit=$lastExitCode")
                if (lastExitCode == 0) {
                    return ShizukuGrantResult.Granted
                }
            }

            return ShizukuGrantResult.Failed("pm grant failed with exit code $lastExitCode")
        }

        companion object {
            private const val PERMISSION_REQUEST_CODE = 7911
            private const val SERVICE_VERSION = 1

            private const val MIN_SUPPORTED_SHIZUKU_VERSION = 10
            private val BINDER_WAIT_TIMEOUT = 8.seconds
            private val OPERATION_TIMEOUT = 60.seconds
            private const val TAG = "BnlShizuku"

            private val KNOWN_HARNESSES =
                listOf(
                    "moe.shizuku.privileged.api" to "Shizuku",
                    "kerneldroid.nightzuku" to "Nightzuku",
                    "roro.stellar.manager" to "Stellar",
                    "af.shizuku.plus.api" to "Shizuku+",
                )
        }
    }
