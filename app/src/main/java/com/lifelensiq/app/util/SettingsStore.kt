package com.lifelensiq.app.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight typed access to SharedPreferences for user settings:
 * daily goals, notification toggles, category overrides, onboarding state
 * and per-widget configuration.
 */
object SettingsStore {

    private const val PREFS_NAME = "lifelensiq_settings"

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences {
        return cachedPrefs ?: context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also { cachedPrefs = it }
    }

    // ---- Daily goals ----
    var studyGoalMin: Int
        get() = prefs(getContext()).getInt("study_goal_min", 120)
        set(v) = prefs(getContext()).edit().putInt("study_goal_min", v).apply()

    var screenLimitMin: Int
        get() = prefs(getContext()).getInt("screen_limit_min", 300)
        set(v) = prefs(getContext()).edit().putInt("screen_limit_min", v).apply()

    var shortsAlertViews: Int
        get() = prefs(getContext()).getInt("shorts_alert_views", 60)
        set(v) = prefs(getContext()).edit().putInt("shorts_alert_views", v).apply()

    // ---- Notification toggles ----
    var dailySummaryEnabled: Boolean
        get() = prefs(getContext()).getBoolean("daily_summary_enabled", true)
        set(v) = prefs(getContext()).edit().putBoolean("daily_summary_enabled", v).apply()

    var screenLimitAlertEnabled: Boolean
        get() = prefs(getContext()).getBoolean("screen_limit_alert_enabled", true)
        set(v) = prefs(getContext()).edit().putBoolean("screen_limit_alert_enabled", v).apply()

    var shortsNudgeEnabled: Boolean
        get() = prefs(getContext()).getBoolean("shorts_nudge_enabled", true)
        set(v) = prefs(getContext()).edit().putBoolean("shorts_nudge_enabled", v).apply()

    var bedtimeReminderEnabled: Boolean
        get() = prefs(getContext()).getBoolean("bedtime_reminder_enabled", true)
        set(v) = prefs(getContext()).edit().putBoolean("bedtime_reminder_enabled", v).apply()

    // ---- Onboarding ----
    var onboardingDone: Boolean
        get() = prefs(getContext()).getBoolean("onboarding_done", false)
        set(v) = prefs(getContext()).edit().putBoolean("onboarding_done", v).apply()

    // ---- Category overrides (pkg -> category) ----
    fun categoryOverrides(): Map<String, String> {
        val raw = prefs(getContext()).getString("category_overrides", null) ?: return emptyMap()
        return raw.split(";").filter { it.isNotBlank() }.mapNotNull { entry ->
            val i = entry.indexOf('=')
            if (i <= 0) null else entry.substring(0, i) to entry.substring(i + 1)
        }.toMap()
    }

    fun setCategoryOverride(pkg: String, category: String) {
        val current = categoryOverrides().toMutableMap()
        if (category.isBlank()) current.remove(pkg) else current[pkg] = category
        prefs(getContext()).edit()
            .putString("category_overrides", current.entries.joinToString(";") { "${it.key}=${it.value}" })
            .apply()
    }

    // ---- Per-widget config ----
    fun widgetDarkTheme(widgetId: Int): Boolean =
        prefs(getContext()).getBoolean("widget_${widgetId}_dark", true)

    fun setWidgetDarkTheme(widgetId: Int, dark: Boolean) {
        prefs(getContext()).edit().putBoolean("widget_${widgetId}_dark", dark).apply()
    }

    fun widgetShowStat(widgetId: Int, key: String): Boolean =
        prefs(getContext()).getBoolean("widget_${widgetId}_stat_$key", true)

    fun setWidgetShowStat(widgetId: Int, key: String, show: Boolean) {
        prefs(getContext()).edit().putBoolean("widget_${widgetId}_stat_$key", show).apply()
    }

    // ---- Alert dedup (one alert per day) ----
    fun alertFiredToday(key: String, today: String = java.time.LocalDate.now().toString()): Boolean {
        val p = prefs(getContext())
        return p.getString("alert_last_fired_$key", "") == today
    }

    fun markAlertFired(key: String, today: String = java.time.LocalDate.now().toString()) {
        prefs(getContext()).edit().putString("alert_last_fired_$key", today).apply()
    }

    // ---- Focus mode ----
    var focusActive: Boolean
        get() = prefs(getContext()).getBoolean("focus_active", false)
        set(v) = prefs(getContext()).edit().putBoolean("focus_active", v).apply()

    var focusStartMs: Long
        get() = prefs(getContext()).getLong("focus_start_ms", 0L)
        set(v) = prefs(getContext()).edit().putLong("focus_start_ms", v).apply()

    var focusSubject: String
        get() = prefs(getContext()).getString("focus_subject", "") ?: ""
        set(v) = prefs(getContext()).edit().putString("focus_subject", v).apply()

    fun focusBlockedApps(): Set<String> {
        val raw = prefs(getContext()).getString("focus_blocked_apps", null) ?: return emptySet()
        return raw.split(";").filter { it.isNotBlank() }.toSet()
    }

    fun setFocusBlockedApps(apps: Set<String>) {
        prefs(getContext()).edit()
            .putString("focus_blocked_apps", apps.joinToString(";"))
            .apply()
    }

    // ---- Morning report ----
    var morningReportEnabled: Boolean
        get() = prefs(getContext()).getBoolean("morning_report_enabled", true)
        set(v) = prefs(getContext()).edit().putBoolean("morning_report_enabled", v).apply()

    var lastMorningReportDate: String
        get() = prefs(getContext()).getString("last_morning_report_date", "") ?: ""
        set(v) = prefs(getContext()).edit().putString("last_morning_report_date", v).apply()

    private fun getContext(): Context = com.lifelensiq.app.di.ServiceLocator.context()
}