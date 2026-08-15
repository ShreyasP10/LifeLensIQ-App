package com.lifelensiq.app.util

import java.time.LocalDate
import java.time.ZoneId

object TimeUtils {

    fun now(): Long = System.currentTimeMillis()

    /** Start of the current day (local timezone) in epoch ms. */
    fun todayEpochStart(): Long {
        val startOfDay = LocalDate.now().atStartOfDay()
        return startOfDay.toEpochSecond(ZoneId.systemDefault().rules.getOffset(startOfDay)) * 1000
    }
}