package io.github.threefreetree.betternightlight.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private val MIDNIGHT_MARGIN: Duration = 1.minutes
private val MIN_RETRY_DELAY: Duration = 1.minutes

fun currentDateFlow(zone: ZoneId = ZoneId.systemDefault()): Flow<LocalDate> =
    flow {
        var date = LocalDate.now(zone)
        while (true) {
            emit(date)
            val now = java.time.LocalDateTime.now(zone)
            val nextMidnight = date.plusDays(1).atStartOfDay(zone).toLocalDateTime()
            val delayDuration =
                java.time.Duration
                    .between(now, nextMidnight)
                    .toMillis()
                    .milliseconds + MIDNIGHT_MARGIN
            delay(delayDuration.coerceAtLeast(MIN_RETRY_DELAY))
            date = LocalDate.now(zone)
        }
    }
