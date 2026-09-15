package io.github.threefreetree.betternightlight.domain

interface AutomationWorkScheduler {
    fun schedulePeriodicWork()

    fun cancelPeriodicWork()
}
