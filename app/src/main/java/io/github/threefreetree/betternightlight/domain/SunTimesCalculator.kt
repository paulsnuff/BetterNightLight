package io.github.threefreetree.betternightlight.domain

import org.shredzone.commons.suncalc.SunTimes
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class SunTimesInfo(
    val sunrise: ZonedDateTime?,
    val sunset: ZonedDateTime?,
)

object SunTimesCalculator {
    fun compute(
        latitude: Double,
        longitude: Double,
        date: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): SunTimesInfo {
        val times =
            SunTimes
                .compute()
                .on(date)
                .at(latitude, longitude)
                .timezone(zoneId)
                .execute()
        return SunTimesInfo(
            sunrise = times.rise,
            sunset = times.set,
        )
    }
}
