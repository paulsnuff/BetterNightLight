package io.github.paulsnuff.betternightlight.domain

import io.github.paulsnuff.betternightlight.domain.model.TimeOfDay

const val TEMPERATURE_MIN_KELVIN = 1000
const val TEMPERATURE_MAX_KELVIN = 6500

const val DEFAULT_TEMPERATURE_KELVIN = 3250

const val DEFAULT_SCHEDULE_TEMPERATURE_KELVIN = 3500

const val BOOST_FALLBACK_OFFSET_KELVIN = 1000

const val LATITUDE_MIN = -90.0
const val LATITUDE_MAX = 90.0
const val LONGITUDE_MIN = -180.0
const val LONGITUDE_MAX = 180.0

const val MINUTES_PER_DAY = 1440

val DEFAULT_MANUAL_START = TimeOfDay.of(21, 0)
val DEFAULT_MANUAL_END = TimeOfDay.of(6, 0)
val DEFAULT_BOOST_TIME = TimeOfDay.of(23, 0)
val DEFAULT_OFF_TIME = TimeOfDay.of(5, 0)
