package com.lifelensiq.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.data.local.EventEntity
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.domain.repository.EventRepository
import com.lifelensiq.app.util.JsonUtil
import com.lifelensiq.app.util.PermissionUtils
import com.lifelensiq.app.util.SettingsStore
import com.lifelensiq.app.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val pendingSync: Int = 0,
    val studyMinutesToday: Long = 0,
    val screenTimeMinutesToday: Long = 0,
    val stepsToday: Long = 0,
    val shortsToday: Long = 0,
    val appSessionsToday: Int = 0,
    val usageAccessGranted: Boolean = false,
    // Weekly chart (oldest first, 7 entries)
    val weeklyStudy: List<Long> = emptyList(),
    val weeklyScreen: List<Long> = emptyList(),
    // Goals
    val studyGoalMin: Int = 120,
    val screenLimitMin: Int = 300,
    // Productivity heatmap: date -> study minutes
    val heatmap: Map<LocalDate, Long> = emptyMap(),
    // Best day of the week
    val bestDay: String? = null,
    val bestDayMinutes: Long = 0
)

class HomeViewModel(
    private val events: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            events.observePendingCount().collect { pending ->
                _uiState.update { it.copy(pendingSync = pending) }
            }
        }
        viewModelScope.launch {
            val from = TimeUtils.todayEpochStart() - 90L * 24 * 60 * 60 * 1000
            events.observeEvents(from, Long.MAX_VALUE).collect { all ->
                val todayStart = TimeUtils.todayEpochStart()
                val now = System.currentTimeMillis()

                val todayEvents = all.filter { it.timestamp >= todayStart }
                val studyMsToday = todayEvents
                    .filter { it.eventType == EventType.STUDY_SESSION.id }
                    .sumOf { payloadLong(it, "durationMs") }
                val appEvents = todayEvents.filter { it.eventType == EventType.APP_SESSION.id }
                val screenMsToday = appEvents.sumOf { payloadLong(it, "durationMs") }

                val weeklyStudy = mutableListOf<Long>()
                val weeklyScreen = mutableListOf<Long>()
                val heatmap = mutableMapOf<LocalDate, Long>()
                for (day in 0 until 7) {
                    val dayStart = TimeUtils.todayEpochStart() - (6 - day) * 86_400_000L
                    val dayEnd = dayStart + 86_400_000L
                    val dayEvents = all.filter { it.timestamp in dayStart until dayEnd }
                    weeklyStudy.add(dayEvents.filter { it.eventType == EventType.STUDY_SESSION.id }
                        .sumOf { payloadLong(it, "durationMs") } / 60_000)
                    weeklyScreen.add(dayEvents.filter { it.eventType == EventType.APP_SESSION.id }
                        .sumOf { payloadLong(it, "durationMs") } / 60_000)
                }
                // Heatmap: last 90 days of study minutes
                val heatmapStart = LocalDate.now().minusDays(90)
                all.groupBy {
                    java.time.Instant.ofEpochMilli(it.timestamp)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                }.forEach { (date, list) ->
                    if (!date.isBefore(heatmapStart) && !date.isAfter(LocalDate.now())) {
                        heatmap[date] = list.filter { it.eventType == EventType.STUDY_SESSION.id }
                            .sumOf { payloadLong(it, "durationMs") } / 60_000
                    }
                }

                val bestIndex = weeklyStudy.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }
                val bestDay = bestIndex?.let {
                    LocalDate.now().minusDays((6 - it.index).toLong()).let { d ->
                        "${dayLabel(d)} · ${formatMinutes(it.value)}"
                    }
                }

                _uiState.update {
                    it.copy(
                        studyMinutesToday = studyMsToday / 60_000,
                        screenTimeMinutesToday = screenMsToday / 60_000,
                        appSessionsToday = appEvents.size,
                        stepsToday = todayEvents.filter { it.eventType == EventType.STEPS.id }
                            .sumOf { payloadLong(it, "stepDelta") },
                        shortsToday = todayEvents.filter { it.eventType == EventType.SHORT_VIDEO.id }
                            .sumOf { payloadLong(it, "views") },
                        weeklyStudy = weeklyStudy,
                        weeklyScreen = weeklyScreen,
                        heatmap = heatmap,
                        bestDay = bestDay,
                        bestDayMinutes = bestIndex?.value ?: 0,
                        studyGoalMin = SettingsStore.studyGoalMin,
                        screenLimitMin = SettingsStore.screenLimitMin
                    )
                }
            }
        }
        _uiState.update {
            it.copy(usageAccessGranted = PermissionUtils.isUsageAccessGranted(ServiceLocator.context()))
        }
    }

    private fun payloadLong(e: EventEntity, key: String): Long =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toLongOrNull() ?: 0L

    private fun dayLabel(date: LocalDate): String = when (date.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "Mon"
        java.time.DayOfWeek.TUESDAY -> "Tue"
        java.time.DayOfWeek.WEDNESDAY -> "Wed"
        java.time.DayOfWeek.THURSDAY -> "Thu"
        java.time.DayOfWeek.FRIDAY -> "Fri"
        java.time.DayOfWeek.SATURDAY -> "Sat"
        java.time.DayOfWeek.SUNDAY -> "Sun"
    }

    private fun formatMinutes(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}