package com.lifelensiq.app.util

import com.lifelensiq.app.domain.EventType

/**
 * Maps Android events onto the web dashboard's category vocabulary
 * (see LifeLensIQ-Web extension/src/shared/categories.js) so that app
 * events and browser events aggregate together in the same dashboard,
 * scores and ML exports.
 */
object WebCategoryMapper {

    const val STUDY = "Study"
    const val DSA = "DSA"
    const val DEVELOPMENT = "Development"
    const val PRODUCTIVITY = "Productivity"
    const val ENTERTAINMENT = "Entertainment"
    const val TIMEPASS = "Timepass"
    const val SHORT_FORM = "Short-form Video"
    const val UTILITIES = "Utilities"
    const val OTHER = "Other"

    /** eventType-independent device events have no duration and are neutral. */
    private val neutralTypes = setOf(
        EventType.SCREEN_ON.id, EventType.SCREEN_OFF.id, EventType.UNLOCK.id,
        EventType.CHARGE_START.id, EventType.CHARGE_END.id, EventType.STEPS.id,
        EventType.WAKE_UP.id, EventType.TRACKING_STATE.id, EventType.SYNC_STATUS.id
    )

    /** Ordered (substring, category) rules mirroring the extension's categories.js. */
    private val packageRules = listOf(
        "moodle" to STUDY, "apsit" to STUDY, "coursera" to STUDY,
        "nptel" to STUDY, "khanacademy" to STUDY, "byjus" to STUDY, "unacademy" to STUDY,
        "leetcode" to DSA, "codeforces" to DSA, "hackerrank" to DSA,
        "codechef" to DSA, "geeksforgeeks" to DSA, "cses" to DSA,
        "stackoverflow" to DEVELOPMENT, "w3schools" to DEVELOPMENT,
        "stackblitz" to DEVELOPMENT, "codesandbox" to DEVELOPMENT, "mdn" to DEVELOPMENT,
        "kaggle" to DEVELOPMENT, "huggingface" to DEVELOPMENT, "colab" to DEVELOPMENT,
        "replit" to DEVELOPMENT,
        "chatgpt" to PRODUCTIVITY, "openai" to PRODUCTIVITY, "claude" to PRODUCTIVITY,
        "anthropic" to PRODUCTIVITY, "gemini" to PRODUCTIVITY, "deepseek" to PRODUCTIVITY,
        "perplexity" to PRODUCTIVITY, "copilot" to PRODUCTIVITY, "poe" to PRODUCTIVITY,
        "grok" to PRODUCTIVITY,
        "github" to PRODUCTIVITY, "gitlab" to PRODUCTIVITY, "linkedin" to PRODUCTIVITY,
        "notion" to PRODUCTIVITY, "todoist" to PRODUCTIVITY,
        "docs" to PRODUCTIVITY, "sheets" to PRODUCTIVITY, "slides" to PRODUCTIVITY,
        "drive" to PRODUCTIVITY, "gmail" to PRODUCTIVITY, "com.google.android.gm" to PRODUCTIVITY,
        "youtube" to ENTERTAINMENT, "netflix" to ENTERTAINMENT,
        "prime" to ENTERTAINMENT, "spotify" to ENTERTAINMENT, "twitch" to ENTERTAINMENT,
        "instagram" to TIMEPASS, "whatsapp" to TIMEPASS, "telegram" to TIMEPASS,
        "discord" to TIMEPASS, "reddit" to TIMEPASS, "snapchat" to TIMEPASS,
        "facebook" to TIMEPASS, "twitter" to TIMEPASS, "x." to TIMEPASS,
        "wikipedia" to UTILITIES, "translate" to UTILITIES, "maps" to UTILITIES,
        "paytm" to UTILITIES, "quicksearchbox" to UTILITIES
    )

    fun categoryFor(eventType: String, packageName: String?): String = when {
        eventType == EventType.SHORT_VIDEO.id -> SHORT_FORM
        eventType == EventType.STUDY_SESSION.id || eventType == EventType.CLASS_ATTENDANCE.id -> STUDY
        eventType == EventType.APP_SESSION.id -> categoryForPackage(packageName)
        eventType in neutralTypes -> UTILITIES
        else -> OTHER
    }

    fun categoryForPackage(packageName: String?): String {
        if (packageName.isNullOrBlank()) return OTHER
        val pkg = packageName.lowercase()
        return packageRules.firstOrNull { pkg.contains(it.first) }?.second ?: OTHER
    }

    /** Domain used by the dashboard's byDomain grouping. */
    fun domainFor(packageName: String?): String = packageName ?: "unknown"
}