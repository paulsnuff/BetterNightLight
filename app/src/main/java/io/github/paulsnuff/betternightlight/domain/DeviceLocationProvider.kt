package io.github.paulsnuff.betternightlight.domain

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

interface DeviceLocationProvider {
    suspend fun getCurrentLocation(): GeoPoint?
}
