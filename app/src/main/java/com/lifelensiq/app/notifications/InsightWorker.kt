package com.lifelensiq.app.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifelensiq.app.data.local.EventEntity
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.util.JsonUtil
import com.lifelensiq.app.util.SettingsStore
import com.lifelensiq.app.util.TimeUtils
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Insight notifications:
 *  - SUMMARY  : daily recap at 21:00 (screen, study, steps, shorts)
 *  - BEDTIME  : wind-down reminder at 22:30
 *  - ALERT    : every 30 min — screen-limit and shorts-nudge checks (once/day)
 */
class InsightWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (inputData.getString(KEY_KIND)) {
            KIND_SUMMARY -> runSummary()
            KIND_BEDTIME -> runBedtime()
            KIND_ALERT -> runAlerts()
            else -> Result.success()
        }
    }

    private suspend fun runSummary(): Result {
        if (!SettingsStore.dailySummaryEnabled) return Result.success()
        val todayStart = TimeUtils.todayEpochStart()
        val events = ServiceLocator.eventRepository().eventsBetween(todayStart, Long.MAX_VALUE)

        val studyMin = events.filter { it.eventType == EventType.STUDY_SESSION.id }
            .sumOf { durationMs(it) } / 60_000
        val screenMin = events.filter { it.eventType == EventType.APP_SESSION.id }
            .sumOf { durationMs(it) } / 60_000
        val steps = events.filter { it.eventType == EventType.STEPS.id }
            .sumOf { payloadLong(it, "stepDelta") }
        val shorts = events.filter { it.eventType == EventType.SHORT_VIDEO.id }
            .sumOf { payloadLong(it, "views") }

        NotificationHelper.post(
            applicationContext,
            NotificationHelper.CHANNEL_SUMMARY,
            NOTIF_SUMMARY,
            "Your day in numbers",
            "Study $studyMin min · Screen $screenMin min · Steps $steps · Reels $shorts"
        )
        return Result.success()
    }

    private suspend fun runBedtime(): Result {
        if (!SettingsStore.bedtimeReminderEnabled) return Result.success()
        val hour = LocalTime.now().hour
        val recentActivity = ServiceLocator.eventRepository()
            .eventsBetween(System.currentTimeMillis() - 60 * 60 * 1000L, Long.MAX_VALUE)
            .any { it.eventType == EventType.APP_SESSION.id || it.eventType == EventType.SCREEN_ON.id }
        if (recentActivity) {
            NotificationHelper.post(
                applicationContext,
                NotificationHelper.CHANNEL_SUMMARY,
                NOTIF_BEDTIME,
                "Time to wind down",
                "It's late and you're still active. Sleep helps tomorrow's focus — try wrapping up."
            )
        }
        return Result.success()
    }

    private suspend fun runAlerts(): Result {
        val todayStart = TimeUtils.todayEpochStart()
        val events = ServiceLocator.eventRepository().eventsBetween(todayStart, Long.MAX_VALUE)

        if (SettingsStore.screenLimitAlertEnabled) {
            val screenMin = events.filter { it.eventType == EventType.APP_SESSION.id }
                .sumOf { durationMs(it) } / 60_000
            val limit = SettingsStore.screenLimitMin
            if (screenMin >= limit && !SettingsStore.alertFiredToday("screen_limit")) {
                SettingsStore.markAlertFired("screen_limit")
                NotificationHelper.post(
                    applicationContext,
                    NotificationHelper.CHANNEL_ALERTS,
                    NOTIF_SCREEN_LIMIT,
                    "Screen-time limit reached",
                    "You've spent $screenMin min on your phone today (limit $limit min). Time to put it down?"
                )
            }
        }
        if (SettingsStore.shortsNudgeEnabled) {
            val shorts = events.filter { it.eventType == EventType.SHORT_VIDEO.id }
                .sumOf { payloadLong(it, "views") }
            val threshold = SettingsStore.shortsAlertViews
            if (shorts >= threshold && !SettingsStore.alertFiredToday("shorts_nudge")) {
                SettingsStore.markAlertFired("shorts_nudge")
                NotificationHelper.post(
                    applicationContext,
                    NotificationHelper.CHANNEL_ALERTS,
                    NOTIF_SHORTS_NUDGE,
                    "Reels check-in",
                    "You've watched $shorts reels/shorts today. A quick break could help focus."
                )
            }
        }
        return Result.success()
    }

    private fun durationMs(e: EventEntity): Long = payloadLong(e, "durationMs")

    private fun payloadLong(e: EventEntity, key: String): Long =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toLongOrNull() ?: 0L

    companion object {
        const val WORK_PREFIX = "lifelensiq_insight_"
        const val KEY_KIND = "kind"
        const val KIND_SUMMARY = "summary"
        const val KIND_BEDTIME = "bedtime"
        const val KIND_ALERT = "alert"

        private const val NOTIF_SUMMARY = 2001
        private const val NOTIF_BEDTIME = 2002
        private const val NOTIF_SCREEN_LIMIT = 2003
        private const val NOTIF_SHORTS_NUDGE = 2004

        /** Millis until the next occurrence of [hour]:[minute] today (or tomorrow). */
        fun millisUntil(hour: Int, minute: Int): Long {
            val now = ZonedDateTime.now()
            var target = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
            if (!target.isAfter(now)) target = target.plusDays(1)
            return Duration.between(now, target).toMillis()
        }

        /** Period + initial delay for a daily job at [hour]:[minute]. */
        fun dailyAt(hour: Int, minute: Int): Pair<Long, Long> =
            TimeUnit.DAYS.toMillis(1) to millisUntil(hour, minute)
    }
}

object InsightScheduler {

    /** Schedule daily summary (21:00), bedtime (22:30) and 30-min alert checks. */
    fun schedule(context: Context) {
        NotificationHelper.ensureChannels(context)
        val wm = WorkManager.getInstance(context)

        fun periodic(kind: String, periodMs: Long, initialDelayMs: Long) {
            val request = PeriodicWorkRequestBuilder<InsightWorker>(periodMs, TimeUnit.MILLISECONDS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setInputData(androidx.work.workDataOf(InsightWorker.KEY_KIND to kind))
                .build()
            wm.enqueueUniquePeriodicWork(
                InsightWorker.WORK_PREFIX + kind,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        val (summaryPeriod, summaryDelay) = InsightWorker.dailyAt(21, 0)
        periodic(InsightWorker.KIND_SUMMARY, summaryPeriod, summaryDelay)

        val (bedtimePeriod, bedtimeDelay) = InsightWorker.dailyAt(22, 30)
        periodic(InsightWorker.KIND_BEDTIME, bedtimePeriod, bedtimeDelay)

        periodic(
            InsightWorker.KIND_ALERT,
            TimeUnit.MINUTES.toMillis(30),
            0L
        )
    }
}