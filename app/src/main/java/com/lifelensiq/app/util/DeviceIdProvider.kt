package com.lifelensiq.app.util

import java.util.UUID

object DeviceIdProvider {
    @Volatile
    private var cached: String? = null

    fun get(context: android.content.Context): String {
        cached?.let { return it }
        val prefs = context.getSharedPreferences("lifelensiq", android.content.Context.MODE_PRIVATE)
        val existing = prefs.getString("device_id", null)
        if (existing != null) {
            cached = existing
            return existing
        }
        val fresh = UUID.randomUUID().toString()
        prefs.edit().putString("device_id", fresh).apply()
        cached = fresh
        return fresh
    }
}
