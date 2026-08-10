package com.lifeiq.app.domain

/** All event types captured by LifeIQ. Values are persisted verbatim. */
enum class EventType(val id: String) {
    APP_SESSION("APP_SESSION"),
    SCREEN_ON("SCREEN_ON"),
    SCREEN_OFF("SCREEN_OFF"),
    UNLOCK("UNLOCK"),
    CHARGE_START("CHARGE_START"),
    CHARGE_END("CHARGE_END"),
    STEPS("STEPS"),
    WAKE_UP("WAKE_UP"),
    CLASS_ATTENDANCE("CLASS_ATTENDANCE"),
    STUDY_SESSION("STUDY_SESSION"),
    TRACKING_STATE("TRACKING_STATE"),
    SYNC_STATUS("SYNC_STATUS");

    companion object {
        fun from(id: String?): EventType? = entries.firstOrNull { it.id == id }
    }
}

enum class AttendanceStatus(val id: String) {
    ATTENDED("ATTENDED"),
    SKIPPED("SKIPPED"),
    ONLINE("ONLINE"),
    LATE("LATE")
}
