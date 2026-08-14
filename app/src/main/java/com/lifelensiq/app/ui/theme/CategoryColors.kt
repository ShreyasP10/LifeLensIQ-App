package com.lifelensiq.app.ui.theme

import androidx.compose.ui.graphics.Color

/** One stable color per web-dashboard category, used across all screens. */
object CategoryColors {
    val STUDY = Color(0xFF4CAF50)          // green
    val DSA = Color(0xFF2196F3)            // blue
    val DEVELOPMENT = Color(0xFF7C4DFF)    // deep purple
    val PRODUCTIVITY = Color(0xFF00ACC1)   // cyan
    val ENTERTAINMENT = Color(0xFFFF9800)  // orange
    val TIMEPASS = Color(0xFFF44336)       // red
    val SHORT_FORM = Color(0xFFE91E63)     // pink
    val UTILITIES = Color(0xFF607D8B)      // blue grey
    val OTHER = Color(0xFF9E9E9E)          // grey

    fun forCategory(category: String): Color = when (category) {
        "Study" -> STUDY
        "DSA" -> DSA
        "Development" -> DEVELOPMENT
        "Productivity" -> PRODUCTIVITY
        "Entertainment" -> ENTERTAINMENT
        "Timepass" -> TIMEPASS
        "Short-form Video" -> SHORT_FORM
        "Utilities" -> UTILITIES
        else -> OTHER
    }
}
