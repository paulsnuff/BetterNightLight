package io.github.threefreetree.betternightlight.domain.model

import io.github.threefreetree.betternightlight.domain.MINUTES_PER_DAY

@JvmInline
value class TimeOfDay(
    val minutes: Int,
) {
    init {
        require(minutes in 0 until MINUTES_PER_DAY) {
            "TimeOfDay minutes must be in 0..${MINUTES_PER_DAY - 1}, got $minutes"
        }
    }

    val hour: Int get() = minutes / 60
    val minute: Int get() = minutes % 60

    companion object {
        fun of(
            hour: Int,
            minute: Int,
        ): TimeOfDay = TimeOfDay(hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59))
    }
}
