package io.github.threefreetree.betternightlight.data

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import io.github.threefreetree.betternightlight.domain.NightLightController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlin.time.Duration.Companion.seconds

data class NightLightStatus(
    val isAvailable: Boolean,
    val isActivated: Boolean,
    val temperature: Int?,
    val autoMode: Int?,
) {
    companion object {
        val UNKNOWN =
            NightLightStatus(
                isAvailable = false,
                isActivated = false,
                temperature = null,
                autoMode = null,
            )
    }
}

class NightLightReader(
    private val context: Context,
) {
    companion object {
        private const val TAG = "BnlReader"
    }

    @SuppressLint("DiscouragedApi")
    fun getStatus(): NightLightStatus {
        val resolver = context.contentResolver

        val isAvailable =
            runCatching {
                val resId =
                    context.resources.getIdentifier(
                        "config_nightDisplayAvailable",
                        "bool",
                        "android",
                    )
                if (resId != 0) {
                    context.resources.getBoolean(resId)
                } else {
                    Settings.Secure.getString(resolver, "night_display_activated") != null
                }
            }.getOrDefault(false)

        val isActivated =
            runCatching {
                Settings.Secure.getInt(resolver, "night_display_activated") == 1
            }.getOrDefault(false)

        val temperature =
            runCatching {
                val colorTemp = Settings.Secure.getInt(resolver, "night_display_color_temperature")
                if (colorTemp > 0) colorTemp else null
            }.getOrNull()

        val autoMode =
            runCatching {
                Settings.Secure
                    .getInt(resolver, "night_display_auto_mode")
                    .takeIf { it == NightLightController.AUTO_MODE_CUSTOM || it == NightLightController.AUTO_MODE_TWILIGHT }
            }.getOrNull()

        return NightLightStatus(
            isAvailable = isAvailable,
            isActivated = isActivated,
            temperature = temperature,
            autoMode = autoMode,
        )
    }

    @OptIn(FlowPreview::class)
    fun observeStatus(): Flow<NightLightStatus> =
        callbackFlow {
            val resolver = context.contentResolver
            val observer =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(
                        selfChange: Boolean,
                        uri: Uri?,
                    ) {
                        trySend(Unit)
                    }
                }

            val urisToObserve =
                listOf(
                    Settings.Secure.getUriFor("night_display_activated"),
                    Settings.Secure.getUriFor("night_display_color_temperature"),
                    Settings.Secure.getUriFor("night_display_auto_mode"),
                )

            urisToObserve.forEach { uri ->
                try {
                    resolver.registerContentObserver(uri, false, observer)
                } catch (e: Exception) {
                    Log.w(TAG, "registerContentObserver failed for $uri", e)
                }
            }

            trySend(Unit)

            awaitClose {
                resolver.unregisterContentObserver(observer)
            }
        }.map { getStatus() }
            .buffer(Channel.BUFFERED)
            .distinctUntilChanged()
            .sample(1.seconds)
            .flowOn(Dispatchers.IO)
}
