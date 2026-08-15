package com.lifelensiq.app.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.domain.repository.EventRepository
import com.lifelensiq.app.util.JsonUtil
import com.lifelensiq.app.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionItem(
    val subject: String,
    val durationMin: Long,
    val endedAt: Long
)

data class SessionUiState(
    val active: Boolean = false,
    val activeSubject: String? = null,
    val activeStartedAt: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val history: List<SessionItem> = emptyList(),
    val lastSummary: String? = null
)

class SessionsViewModel(
    private val events: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val monthAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            val history = events.eventsBetween(monthAgo, Long.MAX_VALUE)
                .filter { it.eventType == EventType.STUDY_SESSION.id }
                .sortedByDescending { it.timestamp }
                .mapNotNull { e ->
                    val p = JsonUtil.decodePayload(e.payloadJson)
                    val subject = (p["subject"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@mapNotNull null
                    val dur = (p["durationMs"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLong() ?: 0L
                    if (dur <= 0) null else SessionItem(subject, dur / 60_000, e.timestamp)
                }
            _uiState.update { it.copy(history = history) }
        }
    }

    fun startSession(subject: String) {
        val trimmed = subject.trim()
        if (trimmed.isEmpty() || _uiState.value.active) return
        // One event per session — written on stop so a killed app
        // leaves no phantom 0-duration event behind.
        _uiState.update {
            it.copy(active = true, activeSubject = trimmed, activeStartedAt = System.currentTimeMillis(), elapsedSeconds = 0)
        }
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (_uiState.value.active) {
                delay(1000)
                val s = _uiState.value
                if (s.active) {
                    _uiState.update { it.copy(elapsedSeconds = (System.currentTimeMillis() - s.activeStartedAt) / 1000) }
                }
            }
        }
    }

    fun stopSession() {
        val s = _uiState.value
        if (!s.active) return
        val now = System.currentTimeMillis()
        val durationMs = now - s.activeStartedAt
        val subject = s.activeSubject ?: ""
        tickerJob?.cancel()
        viewModelScope.launch {
            events.emit(
                EventType.STUDY_SESSION.id,
                mapOf(
                    "subject" to subject,
                    "startedAt" to s.activeStartedAt,
                    "endedAt" to now,
                    "durationMs" to durationMs,
                    "locationType" to "HOME"
                )
            )
            _uiState.update {
                it.copy(
                    active = false,
                    activeSubject = null,
                    elapsedSeconds = 0,
                    lastSummary = "Studied \"$subject\" for ${formatMinutes(durationMs / 60_000)}."
                )
            }
            loadHistory()
        }
    }

    private fun formatMinutes(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}