package com.lifelensiq.app.util

import com.lifelensiq.app.domain.EventType
import org.junit.Assert.assertEquals
import org.junit.Test

/** Mirrors the extension's categories.js rules (LifeLensIQ-Web flow.md §1). */
class WebCategoryMapperTest {

    @Test
    fun `study apps map to Study`() {
        assertEquals("Study", WebCategoryMapper.categoryForPackage("com.ril.elearn.apsit"))
        assertEquals("Study", WebCategoryMapper.categoryForPackage("org.moodle.moodlemobile"))
        assertEquals("Study", WebCategoryMapper.categoryForPackage("org.coursera.android"))
    }

    @Test
    fun `coding platforms map to DSA`() {
        assertEquals("DSA", WebCategoryMapper.categoryForPackage("com.leetcode.leetcode_android"))
        assertEquals("DSA", WebCategoryMapper.categoryForPackage("com.codeforces.app"))
    }

    @Test
    fun `dev tools map to Development`() {
        assertEquals("Development", WebCategoryMapper.categoryForPackage("com.google.colab.android"))
        assertEquals("Development", WebCategoryMapper.categoryForPackage("com.stackoverflow.android"))
    }

    @Test
    fun `ai and work apps map to Productivity`() {
        assertEquals("Productivity", WebCategoryMapper.categoryForPackage("com.openai.chatgpt"))
        assertEquals("Productivity", WebCategoryMapper.categoryForPackage("com.github.android"))
        assertEquals("Productivity", WebCategoryMapper.categoryForPackage("com.google.android.apps.docs"))
        assertEquals("Productivity", WebCategoryMapper.categoryForPackage("com.google.android.gm"))
    }

    @Test
    fun `social and video apps match web categories`() {
        assertEquals("Timepass", WebCategoryMapper.categoryForPackage("com.instagram.android"))
        assertEquals("Timepass", WebCategoryMapper.categoryForPackage("com.whatsapp"))
        assertEquals("Entertainment", WebCategoryMapper.categoryForPackage("com.google.android.youtube"))
        assertEquals("Entertainment", WebCategoryMapper.categoryForPackage("com.netflix.mediaclient"))
        assertEquals("Utilities", WebCategoryMapper.categoryForPackage("com.google.android.apps.maps"))
    }

    @Test
    fun `unknown apps fall back to Other`() {
        assertEquals("Other", WebCategoryMapper.categoryForPackage("com.random.app"))
        assertEquals("Other", WebCategoryMapper.categoryForPackage(null))
    }

    @Test
    fun `study sessions map to Study`() {
        assertEquals("Study", WebCategoryMapper.categoryFor(EventType.STUDY_SESSION.id, null))
    }

    @Test
    fun `short video events map to Short-form Video`() {
        assertEquals("Short-form Video", WebCategoryMapper.categoryFor(EventType.SHORT_VIDEO.id, "com.instagram.android"))
    }

    @Test
    fun `device events are neutral Utilities`() {
        assertEquals("Utilities", WebCategoryMapper.categoryFor(EventType.SCREEN_ON.id, null))
        assertEquals("Utilities", WebCategoryMapper.categoryFor(EventType.STEPS.id, null))
        assertEquals("Utilities", WebCategoryMapper.categoryFor(EventType.TRACKING_STATE.id, null))
    }
}