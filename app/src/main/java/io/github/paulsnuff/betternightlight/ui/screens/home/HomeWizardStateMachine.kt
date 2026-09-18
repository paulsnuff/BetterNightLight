package io.github.paulsnuff.betternightlight.ui.screens.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HomeWizardStep { SUPPORT, PERMISSION, AUTOMATION }

class HomeWizardStateMachine {
    private val _currentStep = MutableStateFlow<HomeWizardStep?>(null)
    val currentStep: StateFlow<HomeWizardStep?> = _currentStep.asStateFlow()

    fun initialize(
        wizardCompleted: Boolean,
        isSupported: Boolean,
        hasPermission: Boolean,
    ) {
        _currentStep.value =
            computeInitialStep(
                wizardCompleted = wizardCompleted,
                isSupported = isSupported,
                hasPermission = hasPermission,
            )
    }

    fun onPermissionMissing(isSupported: Boolean) {
        _currentStep.value =
            if (isSupported) {
                HomeWizardStep.PERMISSION
            } else {
                HomeWizardStep.SUPPORT
            }
    }

    fun next(): HomeWizardStep? {
        val current = _currentStep.value ?: return null
        if (current == HomeWizardStep.AUTOMATION) return null
        val nextStep = HomeWizardStep.entries[current.ordinal + 1]
        _currentStep.value = nextStep
        return nextStep
    }

    companion object {
        fun computeInitialStep(
            wizardCompleted: Boolean,
            isSupported: Boolean,
            hasPermission: Boolean,
        ): HomeWizardStep =
            when {
                !wizardCompleted -> HomeWizardStep.SUPPORT
                !isSupported -> HomeWizardStep.SUPPORT
                !hasPermission -> HomeWizardStep.PERMISSION
                else -> HomeWizardStep.AUTOMATION
            }
    }
}
