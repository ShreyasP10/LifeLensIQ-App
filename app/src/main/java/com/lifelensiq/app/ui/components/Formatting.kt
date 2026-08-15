package com.lifelensiq.app.ui.components

import java.util.Locale

/** "2h 15m" style duration formatting shared across screens. */
fun formatDuration(totalMinutes: Long): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) {
        if (m > 0) String.format(Locale.ROOT, "%dh %dm", h, m) else String.format(Locale.ROOT, "%dh", h)
    } else String.format(Locale.ROOT, "%dm", m)
}
