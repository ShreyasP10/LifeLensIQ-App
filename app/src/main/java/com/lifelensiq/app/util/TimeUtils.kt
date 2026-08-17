package com.lifelensiq.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object TimeUtils {

    /** A "day" runs from 02:00 to 02:00 local time. */
    const val DAY_START_HOUR = 2

    fun now(): Long = System.currentTimeMillis()

    /** Start of the current day (02:00 local) in epoch ms. */
    fun todayEpochStart(): Long = dayStartFor(now())

    /** Day start (02:00 local) that [ts] belongs to, in epoch ms. */
    fun dayStartFor(ts: Long): Long {
        val zone = ZoneId.systemDefault()
        val instant = Instant.ofEpochMilli(ts).atZone(zone)
        val date = if (instant.hour >= DAY_START_HOUR) {
            instant.toLocalDate()
        } else {
            instant.toLocalDate().minusDays(1)
        }
        return date.atTime(DAY_START_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
    }
}