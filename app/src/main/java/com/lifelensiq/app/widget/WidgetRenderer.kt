package com.lifelensiq.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.lifelensiq.app.MainActivity
import com.lifelensiq.app.R
import com.lifelensiq.app.data.local.AppDatabase
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.util.JsonUtil
import com.lifelensiq.app.util.SettingsStore
import com.lifelensiq.app.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Shared rendering for both widget sizes: loads today's stats from Room,
 * applies the per-widget theme (dark/light) and stat visibility.
 */
object WidgetRenderer {

    private const val DARK_BG = 0xFF1C1D22.toInt()
    private const val LIGHT_BG = 0xFFFFFFFF.toInt()
    private const val DARK_TITLE = 0xFFE3E2E6.toInt()
    private const val LIGHT_TITLE = 0xFF1A1C20.toInt()
    private const val DARK_VALUE = 0xFFFFFFFF.toInt()
    private const val LIGHT_VALUE = 0xFF1A1C20.toInt()
    private const val DARK_LABEL = 0xFFC4C6CF.toInt()
    private const val LIGHT_LABEL = 0xFF44474F.toInt()

    fun render(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        layoutId: Int,
        finish: (() -> Unit)? = null
    ) {
        val views = RemoteViews(context.packageName, layoutId)
        val dark = SettingsStore.widgetDarkTheme(widgetId)

        views.setInt(R.id.widget_root, "setBackgroundResource",
            if (dark) R.drawable.widget_bg else R.drawable.widget_bg_light)
        listOf(R.id.cell_study, R.id.cell_screen, R.id.cell_shorts, R.id.cell_steps).forEach { cell ->
            views.setInt(cell, "setBackgroundResource",
                if (dark) R.drawable.widget_cell_bg else R.drawable.widget_cell_bg_light)
        }
        views.setTextColor(R.id.widget_title, if (dark) DARK_TITLE else LIGHT_TITLE)
        listOf(R.id.stat_study, R.id.stat_screen, R.id.stat_shorts, R.id.stat_steps).forEach { stat ->
            views.setTextColor(stat, if (dark) DARK_VALUE else LIGHT_VALUE)
        }

        fun show(cell: Int, key: String) {
            views.setViewVisibility(cell,
                if (SettingsStore.widgetShowStat(widgetId, key)) View.VISIBLE else View.GONE)
        }
        show(R.id.cell_study, "study")
        show(R.id.cell_screen, "screen")
        show(R.id.cell_shorts, "shorts")
        show(R.id.cell_steps, "steps")

        val openApp = PendingIntent.getActivity(
            context, widgetId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val syncNow = PendingIntent.getBroadcast(
            context, widgetId,
            Intent(context, LifeLensIQWidgetProvider::class.java)
                .setAction(LifeLensIQWidgetProvider.ACTION_SYNC),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)
        views.setOnClickPendingIntent(R.id.widget_sync, syncNow)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stats = loadStats(context)
                views.setTextViewText(R.id.stat_study, "${stats.studyMin}m")
                views.setTextViewText(R.id.stat_screen, "${stats.screenMin}m")
                views.setTextViewText(R.id.stat_shorts, stats.shorts.toString())
                views.setTextViewText(R.id.stat_steps, stats.steps.toString())
                manager.updateAppWidget(widgetId, views)
            } catch (_: Throwable) {
            } finally {
                finish?.invoke()
            }
        }
    }

    data class WidgetStats(val studyMin: Long, val screenMin: Long, val shorts: Long, val steps: Long)

    suspend fun loadStats(context: Context): WidgetStats {
        val todayStart = TimeUtils.todayEpochStart()
        val now = TimeUtils.now()
        val events = AppDatabase.get(context).eventDao().getBetween(todayStart, now)
        var studyMs = 0L
        var screenMs = 0L
        var shorts = 0L
        var steps = 0L
        events.forEach { e ->
            when (e.eventType) {
                EventType.STUDY_SESSION.id -> studyMs += payloadLong(e, "durationMs")
                EventType.APP_SESSION.id -> screenMs += payloadLong(e, "durationMs")
                EventType.SHORT_VIDEO.id -> shorts += payloadLong(e, "views")
                EventType.STEPS.id -> steps += payloadLong(e, "stepDelta")
            }
        }
        return WidgetStats(studyMs / 60_000, screenMs / 60_000, shorts, steps)
    }

    private fun payloadLong(e: com.lifelensiq.app.data.local.EventEntity, key: String): Long =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toLongOrNull() ?: 0L
}