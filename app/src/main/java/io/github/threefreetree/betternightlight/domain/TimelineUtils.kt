package io.github.threefreetree.betternightlight.domain

import io.github.threefreetree.betternightlight.domain.model.PhaseTransition

internal enum class TimelineSegmentType { NONE, BASE, BOOST, OFF }

internal data class Arc(
    val start: Int,
    val end: Int,
)

internal data class TimelineSegment(
    val start: Int,
    val end: Int,
    val type: TimelineSegmentType,
)

internal fun dayView(arc: Arc): List<Arc> =
    buildList {
        if (arc.start < MINUTES_PER_DAY && arc.end > 0) {
            val start = arc.start.coerceAtLeast(0)
            val end = arc.end.coerceAtMost(MINUTES_PER_DAY)
            if (start < end) add(Arc(start, end))
        }
        if (arc.end > MINUTES_PER_DAY) {
            val start = (arc.start.coerceAtLeast(MINUTES_PER_DAY) - MINUTES_PER_DAY)
            val end = arc.end - MINUTES_PER_DAY
            if (end in (start + 1)..MINUTES_PER_DAY) add(Arc(start, end))
        }
    }

internal fun buildTimelineSegments(
    baseStartMinutes: Int,
    baseEndMinutes: Int,
    boostStartMinutes: Int,
    boostEnabled: Boolean,
    offMinutes: Int,
    offEnabled: Boolean,
): List<TimelineSegment> {
    val baseStart = baseStartMinutes.coerceIn(0, MINUTES_PER_DAY)
    val baseEnd = baseEndMinutes.coerceIn(0, MINUTES_PER_DAY)

    val baseArcs: List<Arc> =
        when {
            baseStart == baseEnd -> {
                listOf(Arc(0, MINUTES_PER_DAY))
            }

            baseStart < baseEnd -> {
                listOf(Arc(baseStart, baseEnd))
            }

            else -> {
                listOf(
                    Arc(baseStart, MINUTES_PER_DAY),
                    Arc(MINUTES_PER_DAY, MINUTES_PER_DAY + baseEnd),
                )
            }
        }
    val nightEnd = baseArcs.maxOf { it.end }

    val boostArcs: List<Arc> =
        if (boostEnabled) {
            when {
                baseArcs.any { boostStartMinutes in it.start until it.end } -> {
                    listOf(Arc(boostStartMinutes, nightEnd))
                }

                baseArcs.any { boostStartMinutes + MINUTES_PER_DAY in it.start until it.end } -> {
                    listOf(Arc(boostStartMinutes + MINUTES_PER_DAY, nightEnd))
                }

                else -> {
                    emptyList()
                }
            }.filter { it.end > it.start }
        } else {
            emptyList()
        }

    val offArcs: List<Arc> =
        if (offEnabled) {
            when {
                baseArcs.any { offMinutes in it.start until it.end } -> {
                    listOf(Arc(offMinutes, nightEnd))
                }

                baseArcs.any { offMinutes + MINUTES_PER_DAY in it.start until it.end } -> {
                    listOf(Arc(offMinutes + MINUTES_PER_DAY, nightEnd))
                }

                else -> {
                    emptyList()
                }
            }.filter { it.end > it.start }
        } else {
            emptyList()
        }

    val baseView = baseArcs.flatMap { dayView(it) }
    val boostView = boostArcs.flatMap { dayView(it) }
    val offView = offArcs.flatMap { dayView(it) }

    fun inView(
        view: List<Arc>,
        minute: Int,
    ): Boolean = view.any { minute >= it.start && minute < it.end }

    fun typeAt(minute: Int): TimelineSegmentType =
        when {
            inView(offView, minute) -> TimelineSegmentType.OFF
            inView(boostView, minute) -> TimelineSegmentType.BOOST
            inView(baseView, minute) -> TimelineSegmentType.BASE
            else -> TimelineSegmentType.NONE
        }

    val points =
        setOf(0, MINUTES_PER_DAY) +
            (baseView + boostView + offView)
                .flatMap { listOf(it.start, it.end) }
                .filter { it in 1..<MINUTES_PER_DAY }
    val sorted = points.sorted()

    val result = mutableListOf<TimelineSegment>()
    var runStart = sorted.first()
    var runType = typeAt(runStart)
    for (point in sorted.drop(1)) {
        val nextType = typeAt(point)
        if (nextType != runType) {
            if (runStart < point) result += TimelineSegment(runStart, point, runType)
            runStart = point
            runType = nextType
        }
    }
    if (runStart < MINUTES_PER_DAY) result += TimelineSegment(runStart, MINUTES_PER_DAY, runType)
    return result
}

internal fun shiftSegmentsToNoonView(segments: List<TimelineSegment>): List<TimelineSegment> {
    val noonOffset = MINUTES_PER_DAY / 2
    return segments.flatMap { segment ->
        val adjStart = (segment.start - noonOffset + MINUTES_PER_DAY) % MINUTES_PER_DAY
        val adjEnd = (segment.end - noonOffset + MINUTES_PER_DAY) % MINUTES_PER_DAY
        if (adjStart < adjEnd) {
            listOf(TimelineSegment(adjStart, adjEnd, segment.type))
        } else if (adjStart > adjEnd) {
            buildList {
                add(TimelineSegment(adjStart, MINUTES_PER_DAY, segment.type))
                if (adjEnd > 0) add(TimelineSegment(0, adjEnd, segment.type))
            }
        } else {
            emptyList()
        }
    }
}

internal data class EffectiveKelvinInput(
    val nowMinute: Int,
    val baseStartMinutes: Int,
    val baseEndMinutes: Int,
    val baseKelvin: Int,
    val boostEnabled: Boolean,
    val boostStartMinutes: Int,
    val boostKelvin: Int,
    val offEnabled: Boolean,
    val offMinutes: Int,
    val phaseTransition: PhaseTransition,
)

internal fun computeEffectiveKelvin(input: EffectiveKelvinInput): Int? {
    val segments =
        buildTimelineSegments(
            baseStartMinutes = input.baseStartMinutes,
            baseEndMinutes = input.baseEndMinutes,
            boostStartMinutes = input.boostStartMinutes,
            boostEnabled = input.boostEnabled,
            offMinutes = input.offMinutes,
            offEnabled = input.offEnabled,
        )

    val currentSegment =
        segments.find { input.nowMinute in it.start until it.end }
            ?: return null

    return when (currentSegment.type) {
        TimelineSegmentType.NONE, TimelineSegmentType.OFF -> {
            null
        }

        TimelineSegmentType.BOOST -> {
            input.boostKelvin
        }

        TimelineSegmentType.BASE -> {
            if (input.boostEnabled && input.phaseTransition == PhaseTransition.GRADUAL) {
                val nightStart = currentSegment.start
                val boostStart =
                    if (input.boostStartMinutes > nightStart) {
                        input.boostStartMinutes
                    } else {
                        input.boostStartMinutes + MINUTES_PER_DAY
                    }
                if (boostStart > nightStart && input.nowMinute < boostStart) {
                    val progress = (input.nowMinute - nightStart).toFloat() / (boostStart - nightStart)
                    val interpolated = (input.baseKelvin + (input.boostKelvin - input.baseKelvin) * progress).toInt()
                    return interpolated.coerceIn(
                        minOf(input.baseKelvin, input.boostKelvin),
                        maxOf(input.baseKelvin, input.boostKelvin),
                    )
                }
            }
            input.baseKelvin
        }
    }
}
