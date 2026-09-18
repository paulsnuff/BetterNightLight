package io.github.paulsnuff.betternightlight.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.paulsnuff.betternightlight.domain.DeviceLocationProvider
import io.github.paulsnuff.betternightlight.domain.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class DeviceLocationProviderImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DeviceLocationProvider {
        override suspend fun getCurrentLocation(): GeoPoint? {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                return null
            }

            val fresh =
                withTimeoutOrNull(LOCATION_FIX_TIMEOUT) {
                    awaitLocationFix(locationManager, hasFine)
                }

            return (fresh ?: lastKnownLocation(locationManager, hasFine))?.let { location ->
                GeoPoint(location.latitude, location.longitude)
            }
        }

        @SuppressLint("MissingPermission")
        private suspend fun awaitLocationFix(
            locationManager: LocationManager,
            hasFine: Boolean,
        ): Location? {
            val providers =
                candidateProviders(hasFine, includePassive = false)
                    .filter { provider ->
                        runCatching { locationManager.isProviderEnabled(provider) }
                            .getOrDefault(false)
                    }

            if (providers.isEmpty()) {
                return null
            }

            return suspendCancellableCoroutine { continuation ->
                var listener: LocationListener? = null
                listener =
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (continuation.isActive) {
                                listener?.let { runCatching { locationManager.removeUpdates(it) } }
                                continuation.resumeWith(Result.success(location))
                            }
                        }
                    }

                var requestsAccepted = 0
                providers.forEach { provider ->
                    val accepted =
                        runCatching {
                            locationManager.requestLocationUpdates(
                                provider,
                                0L,
                                0f,
                                listener,
                                Looper.getMainLooper(),
                            )
                            true
                        }.getOrDefault(false)
                    if (accepted) requestsAccepted++
                }

                if (requestsAccepted == 0) {
                    continuation.resumeWith(Result.success(null))
                } else {
                    continuation.invokeOnCancellation {
                        runCatching { locationManager.removeUpdates(listener) }
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        private fun lastKnownLocation(
            locationManager: LocationManager,
            hasFine: Boolean,
        ): Location? =
            candidateProviders(hasFine, includePassive = true).firstNotNullOfOrNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }

        private fun candidateProviders(
            hasFine: Boolean,
            includePassive: Boolean,
        ): List<String> =
            buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(LocationManager.FUSED_PROVIDER)
                } else if (hasFine) {
                    add(LocationManager.GPS_PROVIDER)
                }
                add(LocationManager.NETWORK_PROVIDER)
                if (includePassive) {
                    add(LocationManager.PASSIVE_PROVIDER)
                }
            }

        companion object {
            private val LOCATION_FIX_TIMEOUT = 15.seconds
        }
    }
