package io.github.paulsnuff.betternightlight.ui.screens.home

import io.github.paulsnuff.betternightlight.domain.DeviceLocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class DeviceLocationFetcher
    @Inject
    constructor(
        private val deviceLocationProvider: DeviceLocationProvider,
    ) {
        private val _isFetching = MutableStateFlow(false)
        val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

        suspend fun fetch(
            onResult: (latitude: Double, longitude: Double) -> Unit,
            onUnavailable: suspend () -> Unit,
        ) {
            if (_isFetching.value) {
                return
            }

            _isFetching.value = true

            try {
                val location = deviceLocationProvider.getCurrentLocation()
                if (location != null) {
                    onResult(location.latitude, location.longitude)
                } else {
                    onUnavailable()
                }
            } finally {
                _isFetching.value = false
            }
        }
    }
