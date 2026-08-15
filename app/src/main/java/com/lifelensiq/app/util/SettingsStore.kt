package com.lifelensiq.app.util

import android.content.Context
import android.content.SharedPreferences
import com.lifelensiq.app.di.ServiceLocator

/**
 * Lightweight typed access to SharedPreferences for user settings:
 * daily goals, notification toggles, category overrides, onboarding state
 * and per-widget configuration.
 */
object SettingsStore {

    private const val PREFS_NAME = "lifelensiq_settings"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- Daily goals ----
    var studyGoalMin: Int
        get() = prefs(ServiceLocator.context()).getInt("study_goal_min", 120)
        set(v) = prefs(ServiceLocator.context()).edit().putInt("study_goal_min", v).apply()

    var screenLimitMin: Int
        get() = prefs(ServiceLocator.context()).getInt("screen_limit_min", 300)
        set(v) = prefs(ServiceLocator.context()).edit().putInt("screen_limit_min", v).apply()

    var shortsAlertViews: Int
        get() = prefs(ServiceLocator.context()).getInt("shorts_alert_views", 60)
        set(v) = prefs(ServiceLocator.context()).edit().putInt("shorts_alert_views", v).apply()

    // ---- Notification toggles ----
    var dailySummaryEnabled: Boolean
        get() = prefs(ServiceLocator.context()).getBoolean("daily_summary_enabled", true)
        set(v) = prefs(ServiceLocator.context()).edit().putBoolean("daily_summary_enabled", v).apply()

    var screenLimitAlertEnabled: Boolean
        get() = prefs(ServiceLocator.context()).getBoolean("screen_limit_alert_enabled", true)
        set(v) = prefs(ServiceLocator.context()).edit().putBoolean("screen_limit_alert_enabled", v).apply()

    var shortsNudgeEnabled: Boolean
        get() = prefs(ServiceLocator.context()).getBoolean("shorts_nudge_enabled", true)
        set(v) = prefs(ServiceLocator.context()).edit().putBoolean("shorts_nudge_enabled", v).apply()

    var bedtimeReminderEnabled: Boolean
        get() = prefs(ServiceLocator.context()).getBoolean("bedtime_reminder_enabled", true)
        set(v) = prefs(ServiceLocator.context()).edit().putBoolean("bedtime_reminder_enabled", v).apply()

    // ---- Onboarding ----
    var onboardingDone: Boolean
        get() = prefs(ServiceLocator.context()).getBoolean("onboarding_done", false)
        set(v) = prefs(ServiceLocator.context()).edit().putBoolean("onboarding_done", v).apply()

    // ---- Category overrides (pkg -> category) ----
    fun categoryOverrides(): Map<String, String> {
        val raw = prefs(ServiceLocator.context()).getString("category_overrides", null) ?: return emptyMap()
        return raw.split(";").filter { it.isNotBlank() }.mapNotNull { entry ->
            val i = entry.indexOf('=')
            if (i <= 0) null else entry.substring(0, i) to entry.substring(i + 1)
        }.toMap()
    }

    fun setCategoryOverride(pkg: String, category: String) {
        val current = categoryOverrides().toMutableMap()
        if (category.isBlank()) current.remove(pkg) else current[pkg] = category
        prefs(ServiceLocator.context()).edit()
            .putString("category_overrides", current.entries.joinToString(";") { "${it.key}=${it.value}" })
            .apply()
    }

    // ---- Per-widget config ----
    fun widgetDarkTheme(widgetId: Int): Boolean =
        prefs(ServiceLocator.context()).getBoolean("widget_${widgetId}_dark", true)

    fun setWidgetDarkTheme(widgetId: Int, dark: Boolean) {
        prefs(ServiceLocator.context()).edit().putBoolean("widget_${widgetId}_dark", dark).apply()
    }

    fun widgetShowStat(widgetId: Int, key: String): Boolean =
        prefs(ServiceLocator.context()).getBoolean("widget_${widgetId}_stat_$key", true)

    fun setWidgetShowStat(widgetId: Int, key: String, show: Boolean) {
        prefs(ServiceLocator.context()).edit().putBoolean("widget_${widgetId}_stat_$key", show).apply()
    }

    // ---- Alert dedup (one alert per day) ----
    fun alertFiredToday(key: String, today: String = java.time.LocalDate.now().toString()): Boolean {
        val p = prefs(ServiceLocator.context())
        return p.getString("alert_last_fired_$key", "") == today
    }

    fun markAlertFired(key: String, today: String = java.time.LocalDate.now().toString()) {
        prefs(ServiceLocator.context()).edit().putString("alert_last_fired_$key", today).apply()
    }
}