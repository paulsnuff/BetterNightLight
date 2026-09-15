package io.github.threefreetree.betternightlight.domain

import io.github.threefreetree.betternightlight.domain.model.AutomationSchedule
import io.github.threefreetree.betternightlight.domain.model.AutomationTrigger
import java.time.LocalDate
import java.time.ZoneId

private const val FALLBACK_SUNSET_HOUR = 18
private const val FALLBACK_SUNRISE_HOUR = 6

fun resolvedBoostKelvin(schedule: AutomationSchedule): Int =
    schedule.boostKelvin
        ?: (schedule.nightLightTemperatureKelvin - BOOST_FALLBACK_OFFSET_KELVIN)
            .coerceIn(TEMPERATURE_MIN_KELVIN, TEMPERATURE_MAX_KELVIN)

fun nightWindowMinutes(
    schedule: AutomationSchedule,
    date: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Pair<Int, Int> =
    when (schedule.trigger) {
        AutomationTrigger.MANUAL -> {
            schedule.manualStartTime.minutes to schedule.manualEndTime.minutes
        }

        AutomationTrigger.LOCATION -> {
            if (schedule.latitude != null && schedule.longitude != null) {
                val sunTimes =
                    SunTimesCalculator.compute(
                        schedule.latitude,
                        schedule.longitude,
                        date,
                        zone,
                    )
                val start =
                    (sunTimes.sunset?.hour ?: FALLBACK_SUNSET_HOUR) * 60 +
                        (sunTimes.sunset?.minute ?: 0)
                val end =
                    (sunTimes.sunrise?.hour ?: FALLBACK_SUNRISE_HOUR) * 60 +
                        (sunTimes.sunrise?.minute ?: 0)
                start.coerceIn(0, MINUTES_PER_DAY) to end.coerceIn(0, MINUTES_PER_DAY)
            } else {
                DEFAULT_MANUAL_START.minutes to DEFAULT_MANUAL_END.minutes
            }
        }
    }

fun computeCurrentNightLightStrength(
    schedule: AutomationSchedule,
    resolvedBoostKelvin: Int = resolvedBoostKelvin(schedule),
    nowMinute: Int,
): Int? {
    val (baseStartMinutes, baseEndMinutes) = nightWindowMinutes(schedule)

    return computeEffectiveKelvin(
        EffectiveKelvinInput(
            nowMinute = nowMinute,
            baseStartMinutes = baseStartMinutes,
            baseEndMinutes = baseEndMinutes,
            baseKelvin = schedule.nightLightTemperatureKelvin,
            boostEnabled = schedule.boostEnabled,
            boostStartMinutes = schedule.boostTime.minutes,
            boostKelvin = resolvedBoostKelvin,
            offEnabled = schedule.offEnabled,
            offMinutes = schedule.offTime.minutes,
            phaseTransition = schedule.phaseTransition,
        ),
    )
}
