package com.lifeiq.app.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeiq.app.domain.AttendanceStatus
import com.lifeiq.app.domain.EventType
import com.lifeiq.app.domain.model.SlotType
import com.lifeiq.app.domain.model.TimetableSlot
import com.lifeiq.app.domain.repository.EventRepository
import com.lifeiq.app.domain.repository.TimetableRepository
import com.lifeiq.app.util.JsonUtil
import com.lifeiq.app.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AttendanceUiState(
    val todaySlots: List<TimetableSlot> = emptyList(),
    val marked: Map<String, String> = emptyMap()
)

class AttendanceViewModel(
    private val events: EventRepository,
    private val timetable: TimetableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val today = TimeUtils.todayName()
            timetable.observeDay(today).collect { slots ->
                val attendable = slots.filter { it.applicable && it.type in SlotType.ATTENDABLE }
                _uiState.update { it.copy(todaySlots = attendable) }
            }
        }
        loadMarked()
    }

    private fun loadMarked() {
        viewModelScope.launch {
            val todayStart = TimeUtils.todayEpochStart()
            val marked = events.eventsBetween(todayStart, Long.MAX_VALUE)
                .filter { it.eventType == EventType.CLASS_ATTENDANCE.id }
                .associate { e ->
                    val p = JsonUtil.decodePayload(e.payloadJson)
                    val slotNo = (p["slotRef"] as? kotlinx.serialization.json.JsonObject)
                        ?.get("slotNo")?.toString() ?: ""
                    val status = (p["status"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                    slotNo to status
                }
            _uiState.update { it.copy(marked = marked) }
        }
    }

    fun mark(slot: TimetableSlot, status: AttendanceStatus) {
        viewModelScope.launch {
            events.emit(
                EventType.CLASS_ATTENDANCE.id,
                mapOf(
                    "slotRef" to mapOf("day" to slot.day, "slotNo" to slot.slotNo),
                    "subject" to slot.subject,
                    "status" to status.id,
                    "markedAt" to System.currentTimeMillis()
                )
            )
            loadMarked()
        }
    }
}
