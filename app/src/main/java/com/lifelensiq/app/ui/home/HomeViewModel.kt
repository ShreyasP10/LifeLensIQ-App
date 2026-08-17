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
import com.lifelensiq.app.util.WebCategoryMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val pendingSync: Int = 0,
    val productiveMinutesToday: Long = 0,
    val screenTimeMinutesToday: Long = 0,
    val stepsToday: Long = 0,
    val shortsToday: Long = 0,
    val appSessionsToday: Int = 0,
    val usageAccessGranted: Boolean = false,
    // Weekly chart (oldest first, 7 entries, days run 02:00-02:00)
    val weeklyProductive: List<Long> = emptyList(),
    val weeklyScreen: List<Long> = emptyList(),
    // Goals
    val productiveGoalMin: Int = 120,
    val screenLimitMin: Int = 300,
    // Calendar: date -> productive minutes (days run 02:00-02:00)
    val calendar: Map<LocalDate, Long> = emptyMap(),
    // Best day of the week
    val bestDay: String? = null,
    val bestDayMinutes: Long = 0,
    // Wake & sleep (day = 02:00-02:00)
    val pickupsToday: Int = 0,
    val firstWake: String? = null,
    val lastShutdown: String? = null,
    val sleepEstimate: String? = null
)

class HomeViewModel(
    private val events: EventRepository,
    private val deviceId: String
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
            val from = TimeUtils.todayEpochStart() - 400L * 24 * 60 * 60 * 1000
            events.observeEvents(from, Long.MAX_VALUE).collect { all ->
                val todayStart = TimeUtils.todayEpochStart()
                val now = System.currentTimeMillis()

                // Phone-only: ignore events that came from the website.
                val phone = all.filter { it.deviceId == deviceId }
                val todayEvents = phone.filter { it.timestamp >= todayStart }
                val overrides = SettingsStore.categoryOverrides()

                fun productiveOf(e: EventEntity): Long = when (e.eventType) {
                    EventType.STUDY_SESSION.id -> payloadLong(e, "durationMs")
                    EventType.APP_SESSION.id -> {
                        val pkg = payloadString(e, "packageName")
                        val cat = WebCategoryMapper.categoryForPackage(pkg, overrides)
                        if (WebCategoryMapper.isProductive(cat)) payloadLong(e, "durationMs") else 0L
                    }
                    else -> 0L
                }

                val productiveMsToday = todayEvents.sumOf { productiveOf(it) }
                val appEvents = todayEvents.filter { it.eventType == EventType.APP_SESSION.id }
                val screenMsToday = appEvents.sumOf { payloadLong(it, "durationMs") }

                val weeklyProductive = mutableListOf<Long>()
                val weeklyScreen = mutableListOf<Long>()
                val calendar = mutableMapOf<LocalDate, Long>()
                for (day in 0 until 7) {
                    val dayStart = todayStart - (6 - day) * 86_400_000L
                    val dayEnd = dayStart + 86_400_000L
                    val dayEvents = phone.filter { it.timestamp in dayStart until dayEnd }
                    weeklyProductive.add(dayEvents.sumOf { productiveOf(it) } / 60_000)
                    weeklyScreen.add(dayEvents.filter { it.eventType == EventType.APP_SESSION.id }
                        .sumOf { payloadLong(it, "durationMs") } / 60_000)
                }
                // Calendar: last 90 days of productive minutes, aligned to 02:00 days
                val calendarStart = TimeUtils.dayStartFor(now) - 90L * 86_400_000
                phone.forEach { e ->
                    val day = java.time.Instant.ofEpochMilli(TimeUtils.dayStartFor(e.timestamp))
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    if (TimeUtils.dayStartFor(e.timestamp) >= calendarStart) {
                        calendar[day] = (calendar[day] ?: 0L) + productiveOf(e)
                    }
                }
                calendar.entries.removeIf { it.value <= 0 }

                val bestIndex = weeklyProductive.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }
                val bestDay = bestIndex?.let {
                    LocalDate.now().minusDays((6 - it.index).toLong()).let { d ->
                        "${dayLabel(d)} · ${formatMinutes(it.value)}"
                    }
                }

                // Wake & sleep (day = 02:00-02:00):
                //  - firstWake    : first screen-on after 02:00
                //  - lastShutdown : last screen-off before 02:00 (yesterday's last use)
                //  - sleep        : last shutdown before 02:00 -> first wake after 02:00
                val screenOnToday = todayEvents.filter { it.eventType == EventType.SCREEN_ON.id }
                    .sortedBy { it.timestamp }
                val firstWake = screenOnToday.firstOrNull()?.timestamp?.let(::timeLabel)
                val lastShutdown = phone.filter {
                    it.eventType == EventType.SCREEN_OFF.id && it.timestamp < todayStart
                }.maxOfOrNull { it.timestamp }?.let(::timeLabel)
                val bedtime = phone.filter {
                    it.eventType == EventType.SCREEN_OFF.id && it.timestamp in (todayStart - 86_400_000L) until todayStart
                }.maxOfOrNull { it.timestamp }
                val wake = screenOnToday.firstOrNull()?.timestamp
                val sleepMs = if (bedtime != null && wake != null) wake - bedtime else null
                val sleepEstimate = sleepMs
                    ?.takeIf { it in 2_700_000L..14 * 3600_000L }
                    ?.let { formatMinutes(it / 60_000) }

                _uiState.update {
                    it.copy(
                        productiveMinutesToday = productiveMsToday / 60_000,
                        screenTimeMinutesToday = screenMsToday / 60_000,
                        appSessionsToday = appEvents.size,
                        stepsToday = todayEvents.filter { it.eventType == EventType.STEPS.id }
                            .sumOf { payloadLong(it, "stepDelta") },
                        shortsToday = todayEvents.filter { it.eventType == EventType.SHORT_VIDEO.id }
                            .sumOf { payloadLong(it, "views") },
                        weeklyProductive = weeklyProductive,
                        weeklyScreen = weeklyScreen,
                        calendar = calendar,
                        bestDay = bestDay,
                        bestDayMinutes = bestIndex?.value ?: 0,
                        productiveGoalMin = SettingsStore.studyGoalMin,
                        screenLimitMin = SettingsStore.screenLimitMin,
                        pickupsToday = screenOnToday.size,
                        firstWake = firstWake,
                        lastShutdown = lastShutdown,
                        sleepEstimate = sleepEstimate
                    )
                }
            }
        }
        _uiState.update {
            it.copy(usageAccessGranted = PermissionUtils.isUsageAccessGranted(ServiceLocator.context()))
        }
    }

    private fun payloadString(e: EventEntity, key: String): String =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""

    private fun payloadLong(e: EventEntity, key: String): Long =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toLongOrNull() ?: 0L

    private fun timeLabel(ts: Long): String =
        java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault())
            .toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))

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