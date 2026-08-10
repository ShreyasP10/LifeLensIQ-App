package com.lifeiq.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeiq.app.data.local.EventEntity
import com.lifeiq.app.domain.EventType
import com.lifeiq.app.domain.model.TimetableSlot
import com.lifeiq.app.domain.repository.EventRepository
import com.lifeiq.app.domain.repository.TimetableRepository
import com.lifeiq.app.util.JsonUtil
import com.lifeiq.app.util.TimeUtils
import com.lifeiq.app.util.TimetableSlotComparable
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
    val appSessionsToday: Int = 0
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
            val appCount = eventsToday.count { it.eventType == EventType.APP_SESSION.id }
            _uiState.update { it.copy(studyMinutesToday = studyMs / 60_000, appSessionsToday = appCount) }
        }
    }

    private fun studyDuration(e: EventEntity): Long =
        (JsonUtil.decodePayload(e.payloadJson)["durationMs"] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toLongOrNull() ?: 0L
}

private fun TimetableSlot.toComparable() = TimetableSlotComparable(
    day = day, slotNo = slotNo, start = start, end = end,
    subject = subject, subjectFull = subjectFull, room = room,
    typeId = type.id, applicable = applicable
)
