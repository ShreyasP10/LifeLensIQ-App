package com.lifelensiq.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.data.local.EventEntity
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.domain.model.SlotType
import com.lifelensiq.app.domain.model.TimetableSlot
import com.lifelensiq.app.domain.repository.EventRepository
import com.lifelensiq.app.domain.repository.TimetableRepository
import com.lifelensiq.app.util.JsonUtil
import com.lifelensiq.app.util.PermissionUtils
import com.lifelensiq.app.util.TimeUtils
import com.lifelensiq.app.util.TimetableSlotComparable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val todaySlots: List<TimetableSlot> = emptyList(),
    val nextSlot: TimetableSlot? = null,
    val pendingSync: Int = 0,
    val studyMinutesToday: Long = 0,
    val appSessionsToday: Int = 0,
    val screenTimeMinutesToday: Long = 0,
    val attendanceMarkedToday: Int = 0,
    val classesToday: Int = 0,
    val usageAccessGranted: Boolean = false,
    val timetableImported: Boolean = false
)

class HomeViewModel(
    private val events: EventRepository,
    private val timetable: TimetableRepository
) : ViewModel() {

    private val today = TimeUtils.todayName()
    private val todayStart = TimeUtils.todayEpochStart()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            timetable.observeDay(today).collect { slots ->
                val applicable = slots.filter { it.applicable }
                _uiState.update {
                    it.copy(
                        todaySlots = applicable,
                        timetableImported = slots.isNotEmpty(),
                        classesToday = applicable.count { s -> s.type in SlotType.ATTENDABLE },
                        nextSlot = TimeUtils.currentOrNext(
                            applicable.map { slot -> slot.toComparable() },
                            System.currentTimeMillis()
                        )?.toSlot()
                    )
                }
            }
        }
        viewModelScope.launch {
            events.observePendingCount().collect { pending ->
                _uiState.update { it.copy(pendingSync = pending) }
            }
        }
        viewModelScope.launch {
            val eventsToday = events.eventsBetween(todayStart, Long.MAX_VALUE)
            val studyMs = eventsToday
                .filter { it.eventType == EventType.STUDY_SESSION.id }
                .sumOf { studyDuration(it) }
            val appEvents = eventsToday.filter { it.eventType == EventType.APP_SESSION.id }
            val screenMs = appEvents.sumOf { sessionDuration(it) }
            val attendanceEvents = eventsToday.filter { it.eventType == EventType.CLASS_ATTENDANCE.id }
            _uiState.update {
                it.copy(
                    studyMinutesToday = studyMs / 60_000,
                    appSessionsToday = appEvents.size,
                    screenTimeMinutesToday = screenMs / 60_000,
                    attendanceMarkedToday = attendanceEvents.size
                )
            }
        }
        _uiState.update {
            it.copy(usageAccessGranted = PermissionUtils.isUsageAccessGranted(ServiceLocator.context()))
        }
    }

    private fun studyDuration(e: EventEntity): Long =
        (JsonUtil.decodePayload(e.payloadJson)["durationMs"] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toLongOrNull() ?: 0L

    private fun sessionDuration(e: EventEntity): Long =
        (JsonUtil.decodePayload(e.payloadJson)["durationMs"] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toLongOrNull() ?: 0L
}

private fun TimetableSlot.toComparable() = TimetableSlotComparable(
    day = day, slotNo = slotNo, start = start, end = end,
    subject = subject, subjectFull = subjectFull, room = room,
    typeId = type.id, applicable = applicable
)
