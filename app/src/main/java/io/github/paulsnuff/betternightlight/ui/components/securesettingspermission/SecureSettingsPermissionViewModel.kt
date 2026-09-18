package io.github.paulsnuff.betternightlight.ui.components.securesettingspermission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.paulsnuff.betternightlight.data.PermissionGate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SecureSettingsPermissionUiState(
    val hasSecureSettingsPermission: Boolean = false,
)

@HiltViewModel
class SecureSettingsPermissionViewModel
    @Inject
    constructor(
        private val permissionGate: PermissionGate,
    ) : ViewModel() {
        val uiState: StateFlow<SecureSettingsPermissionUiState> =
            permissionGate.hasPermissionFlow
                .map { hasPermission ->
                    SecureSettingsPermissionUiState(hasSecureSettingsPermission = hasPermission)
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue =
                        SecureSettingsPermissionUiState(
                            hasSecureSettingsPermission = permissionGate.hasPermissionFlow.value,
                        ),
                )

        fun refreshPermission() {
            permissionGate.updatePermissionState()
        }
    }
