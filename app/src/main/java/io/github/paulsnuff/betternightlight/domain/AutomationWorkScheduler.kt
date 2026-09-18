package io.github.paulsnuff.betternightlight.domain

interface AutomationWorkScheduler {
    fun schedulePeriodicWork()

    fun cancelPeriodicWork()
}
