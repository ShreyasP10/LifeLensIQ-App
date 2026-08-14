package com.lifelensiq.app.tracking

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.domain.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Counts Instagram/Facebook Reels and YouTube Shorts viewed inside the
 * native apps (the browser extension already counts shorts in Chrome).
 *
 * Prototype heuristic — Android exposes no official API for in-app content,
 * so this watches for the "Reels"/"Shorts" marker in the active window and
 * counts each distinct content change while it is visible (one swipe ≈ one
 * short). Nothing is read or stored except the counts. Best-effort by design.
 *
 * Performance: accessibility events run on the tracked app's process, so an
 * aggressive service slows YouTube/Instagram down and can alter their UI.
 * To stay invisible we (1) check the cheap event packageName first and only
 * touch the window tree for our four target apps, (2) never scan more than
 * once per SCAN_THROTTLE_MS, and (3) bound the tree walk by node budget so a
 * huge YouTube hierarchy cannot stall the app we are watching.
 */
class ShortsReelsDetector : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var sessionPlatform: String? = null
    private var sessionViews: Int = 0
    private var sessionStartedAt: Long = 0L
    private var lastCountedAt: Long = 0L
    private var lastScanAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = System.currentTimeMillis()
        // Cheap gate first: only our target packages are worth scanning.
        val pkg = event?.packageName?.toString() ?: return
        if (pkg !in TARGET_PACKAGES) return
        // Battery guard: never scan more than once per throttle window.
        if (now - lastScanAt < SCAN_THROTTLE_MS) return
        lastScanAt = now

        val root = runCatching { rootInActiveWindow }.getOrNull() ?: return
        try {
            val marker = findMarker(root)
            if (marker == null) {
                closeSessionIfOpen(now)
                return
            }

            val platform = PLATFORM_BY_PACKAGE[pkg] ?: "unknown"
            if (sessionPlatform != platform) {
                closeSessionIfOpen(now)
                sessionPlatform = platform
                sessionViews = 0
                sessionStartedAt = now
            }
            // A content change while the marker stays visible = one swipe (new short).
            if (now - lastCountedAt >= COUNT_THROTTLE_MS) {
                sessionViews++
                lastCountedAt = now
            }
        } finally {
            root.recycle()
        }
    }

    /** Bounded DFS: walks at most MAX_NODES nodes, releasing them on the way back. */
    private fun findMarker(root: AccessibilityNodeInfo): String? {
        var budget = MAX_NODES
        return findMarkerInTree(root, 0, { budget-- }, { budget > 0 })
    }

    private fun findMarkerInTree(
        node: AccessibilityNodeInfo,
        depth: Int,
        consumeBudget: () -> Unit,
        hasBudget: () -> Boolean
    ): String? {
        consumeBudget()
        val candidates = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
        candidates.firstOrNull { it in MARKERS }?.let { return it }
        if (node.childCount == 0 || depth >= MAX_DEPTH || !hasBudget()) return null
        for (i in 0 until node.childCount) {
            if (!hasBudget()) break
            val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
            try {
                findMarkerInTree(child, depth + 1, consumeBudget, hasBudget)?.let { return it }
            } finally {
                child.recycle()
            }
        }
        return null
    }

    private fun closeSessionIfOpen(now: Long) {
        val platform = sessionPlatform ?: return
        if (sessionViews <= 0) {
            resetSession()
            return
        }
        val durationMs = (now - sessionStartedAt).coerceAtLeast(0)
        scope.launch {
            ServiceLocator.eventEmitter().emit(
                EventType.SHORT_VIDEO.id,
                mapOf(
                    "platform" to platform,
                    "views" to sessionViews,
                    "startedAt" to sessionStartedAt,
                    "endedAt" to now,
                    "durationMs" to durationMs
                )
            )
        }
        resetSession()
    }

    private fun resetSession() {
        sessionPlatform = null
        sessionViews = 0
        sessionStartedAt = 0L
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        closeSessionIfOpen(System.currentTimeMillis())
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val SCAN_THROTTLE_MS = 1_200L
        private const val COUNT_THROTTLE_MS = 1_000L
        private const val MAX_NODES = 400
        private const val MAX_DEPTH = 12

        private val TARGET_PACKAGES = setOf(
            "com.instagram.android", "com.instagram.lite",
            "com.google.android.youtube", "com.facebook.katana"
        )
        private val PLATFORM_BY_PACKAGE = mapOf(
            "com.instagram.android" to "instagram",
            "com.instagram.lite" to "instagram",
            "com.google.android.youtube" to "youtube",
            "com.facebook.katana" to "facebook"
        )
        private val MARKERS = setOf("Reels", "Shorts")
    }
}
