package com.lifelensiq.app.notifications

import android.content.Context
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.util.JsonUtil
import com.lifelensiq.app.util.SettingsStore
import com.lifelensiq.app.util.TimeUtils
import com.lifelensiq.app.data.local.EventEntity

/**
 * Posts a "morning report" notification on the first screen-on of the day
 * (after 05:00): yesterday's stats, a 0-100 score and today's goals.
 * Fires at most once per day.
 */
object MorningReporter {

    private const val NOTIF_MORNING = 2005

    suspend fun maybePost(context: Context) {
        if (!SettingsStore.morningReportEnabled) return
        val today = java.time.LocalDate.now().toString()
        if (SettingsStore.lastMorningReportDate == today) return
        if (java.time.LocalTime.now().hour < 5) return

        val repo = ServiceLocator.eventRepository()
        val todayStart = TimeUtils.todayEpochStart()
        val yesterdayStart = todayStart - 86_400_000L
        val deviceId = com.lifelensiq.app.util.DeviceIdProvider.get(ServiceLocator.context())
        val overrides = SettingsStore.categoryOverrides()
        val yesterday = repo.eventsBetween(yesterdayStart, todayStart)
            .filter { it.deviceId == deviceId }

        fun productiveOf(e: EventEntity): Long = when (e.eventType) {
            EventType.STUDY_SESSION.id -> durationMs(e)
            EventType.APP_SESSION.id -> {
                val cat = com.lifelensiq.app.util.WebCategoryMapper
                    .categoryForPackage(payloadString(e, "packageName"), overrides)
                if (com.lifelensiq.app.util.WebCategoryMapper.isProductive(cat)) durationMs(e) else 0L
            }
            else -> 0L
        }

        val productive = yesterday.sumOf { productiveOf(it) } / 60_000
        val screen = yesterday.filter { it.eventType == EventType.APP_SESSION.id }
            .sumOf { durationMs(it) } / 60_000
        val steps = yesterday.filter { it.eventType == EventType.STEPS.id }
            .sumOf { payloadLong(it, "stepDelta") }
        val shorts = yesterday.filter { it.eventType == EventType.SHORT_VIDEO.id }
            .sumOf { payloadLong(it, "views") }
        val pickups = yesterday.count { it.eventType == EventType.SCREEN_ON.id }

        val score = scoreOf(
            study = productive,
            screen = screen,
            shorts = shorts,
            goal = SettingsStore.studyGoalMin,
            limit = SettingsStore.screenLimitMin,
            shortsThreshold = SettingsStore.shortsAlertViews,
            pickups = pickups
        )

        NotificationHelper.post(
            context,
            NotificationHelper.CHANNEL_SUMMARY,
            NOTIF_MORNING,
            "Good morning — here's yesterday",
            "Score $score/100 · Productive $productive/${SettingsStore.studyGoalMin} min · Screen $screen min · " +
                "Steps $steps · Reels $shorts · $pickups pickups. Today's goal: ${
                    SettingsStore.studyGoalMin
                } min productive."
        )
        SettingsStore.lastMorningReportDate = today
    }

    /** Simple 0-100 daily score: study, screen budget, shorts budget, pickups. */
    fun scoreOf(
        study: Long,
        screen: Long,
        shorts: Long,
        goal: Int,
        limit: Int,
        shortsThreshold: Int,
        pickups: Int
    ): Int {
        val studyPts = (study.coerceAtMost(goal * 2L) * 30 / goal.coerceAtLeast(1)).toInt().coerceAtMost(30)
        val screenPts = if (screen <= limit) 30 else (30 * limit.coerceAtLeast(1) / screen.coerceAtLeast(1)).toInt()
        val shortsPts =
            if (shorts <= shortsThreshold) 20 else (20 * shortsThreshold.coerceAtLeast(1) / shorts.coerceAtLeast(1)).toInt()
        val pickupPts = if (pickups <= 25) 20 else (20 * 25 / pickups.coerceAtLeast(1)).toInt()
        return (studyPts + screenPts + shortsPts + pickupPts).coerceIn(0, 100)
    }

    private fun durationMs(e: EventEntity): Long = payloadLong(e, "durationMs")

    private fun payloadString(e: EventEntity, key: String): String =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""

    private fun payloadLong(e: EventEntity, key: String): Long =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toLongOrNull() ?: 0L
}