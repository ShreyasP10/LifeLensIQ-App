package com.lifelensiq.app.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.domain.model.SlotType
import com.lifelensiq.app.domain.model.TimetableSlot
import com.lifelensiq.app.domain.repository.EventRepository
import com.lifelensiq.app.domain.repository.TimetableRepository
import com.lifelensiq.app.util.JsonUtil
import com.lifelensiq.app.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val active: Boolean = false,
    val activeSubject: String? = null,
    val activeStartedAt: Long = 0L,
    val subjects: List<String> = emptyList(),
    val todaySessions: List<String> = emptyList()
)

class SessionsViewModel(
    private val events: EventRepository,
    private val timetable: TimetableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val allSlots = timetable.observeAll().first()
            val subjects = allSlots
                .filter { it.type in SlotType.ATTENDABLE && it.applicable && it.subject != "FREE" }
                .map { it.subjectFull }
                .distinct()
            _uiState.update { it.copy(subjects = subjects) }

            val todayStart = TimeUtils.todayEpochStart()
            val today = events.eventsBetween(todayStart, Long.MAX_VALUE)
                .filter { it.eventType == EventType.STUDY_SESSION.id }
                .map { e ->
                    val p = JsonUtil.decodePayload(e.payloadJson)
                    val subject = (p["subject"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "?"
                    val dur = (p["durationMs"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLong() ?: 0L
                    "$subject — ${dur / 60_000} min"
                }
            _uiState.update { it.copy(todaySessions = today) }
        }
    }

    fun startSession(subject: String) {
        viewModelScope.launch {
            events.emit(
                EventType.STUDY_SESSION.id,
                mapOf("subject" to subject, "startedAt" to System.currentTimeMillis(), "endedAt" to 0L, "durationMs" to 0L, "locationType" to "HOME")
            )
            _uiState.update { it.copy(active = true, activeSubject = subject, activeStartedAt = System.currentTimeMillis()) }
        }
    }

    fun stopSession() {
        val s = _uiState.value
        if (!s.active) return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            events.emit(
                EventType.STUDY_SESSION.id,
                mapOf(
                    "subject" to (s.activeSubject ?: ""),
                    "startedAt" to s.activeStartedAt,
                    "endedAt" to now,
                    "durationMs" to (now - s.activeStartedAt),
                    "locationType" to "HOME"
                )
            )
            _uiState.update { it.copy(active = false, activeSubject = null) }
        }
    }
}
