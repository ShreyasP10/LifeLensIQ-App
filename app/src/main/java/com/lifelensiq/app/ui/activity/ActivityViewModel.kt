package com.lifelensiq.app.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.data.local.EventEntity
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.domain.repository.EventRepository
import com.lifelensiq.app.util.JsonUtil
import com.lifelensiq.app.util.SettingsStore
import com.lifelensiq.app.util.TimeUtils
import com.lifelensiq.app.util.WebCategoryMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoryUsage(
    val category: String,
    val minutes: Long,
    val appSessions: Int,
    val appCount: Int,
    val shortsViews: Int
)

data class AppUsageItem(
    val name: String,
    val minutes: Long,
    val sessions: Int,
    val category: String
)

enum class DeviceFilter(val label: String) {
    ALL("All"),
    THIS_DEVICE("This device"),
    WEB("Website")
}

data class ActivityUiState(
    val loading: Boolean = true,
    val totalMinutes: Long = 0,
    val categories: List<CategoryUsage> = emptyList(),
    val topApps: List<AppUsageItem> = emptyList(),
    val donutSlices: List<Pair<String, Long>> = emptyList(),
    val deviceFilter: DeviceFilter = DeviceFilter.ALL
)

/**
 * Aggregates today's APP_SESSION / SHORT_VIDEO / STUDY_SESSION events into
 * the web dashboard's category vocabulary (WebCategoryMapper) so the phone
 * shows the same Study/DSA/Development/... breakdown the dashboard does.
 */
class ActivityViewModel(
    private val events: EventRepository,
    private val deviceId: String
) : ViewModel() {

    private val todayStart = TimeUtils.todayEpochStart()
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            events.observeEvents(todayStart, Long.MAX_VALUE).collect { list ->
                cachedEvents = list
                _uiState.value = buildState(filterFor(_uiState.value.deviceFilter, list))
                    .copy(deviceFilter = _uiState.value.deviceFilter)
            }
        }
    }

    fun setDeviceFilter(filter: DeviceFilter) {
        _uiState.value = buildState(filterFor(filter, cachedEvents)).copy(deviceFilter = filter)
    }

    private fun filterFor(filter: DeviceFilter, list: List<EventEntity>): List<EventEntity> =
        when (filter) {
            DeviceFilter.ALL -> list
            DeviceFilter.THIS_DEVICE -> list.filter { it.deviceId == deviceId }
            DeviceFilter.WEB -> list.filter { it.deviceId == "web" }
        }

    private var cachedEvents: List<EventEntity> = emptyList()

    private fun buildState(list: List<EventEntity>): ActivityUiState {
        val overrides = SettingsStore.categoryOverrides()
        val byCategory = linkedMapOf<String, MutableList<AppUsageItem>>()

        list.forEach { event ->
            when (event.eventType) {
                EventType.APP_SESSION.id -> {
                    val pkg = payloadString(event, "packageName")
                    val category = WebCategoryMapper.categoryForPackage(pkg, overrides)
                    val name = payloadString(event, "appName").takeIf { it.isNotBlank() } ?: pkg
                    byCategory.getOrPut(category) { mutableListOf() }.add(
                        AppUsageItem(
                            name = name,
                            minutes = durationMinutes(event),
                            sessions = 1,
                            category = category
                        )
                    )
                }
                EventType.SHORT_VIDEO.id -> {
                    val views = payloadInt(event, "views")
                    byCategory.getOrPut(WebCategoryMapper.SHORT_FORM) { mutableListOf() }.add(
                        AppUsageItem(
                            name = "Reels / Shorts",
                            minutes = durationMinutes(event),
                            sessions = views,
                            category = WebCategoryMapper.SHORT_FORM
                        )
                    )
                }
                EventType.STUDY_SESSION.id -> {
                    byCategory.getOrPut(WebCategoryMapper.STUDY) { mutableListOf() }.add(
                        AppUsageItem(
                            name = "Study session",
                            minutes = durationMinutes(event),
                            sessions = 1,
                            category = WebCategoryMapper.STUDY
                        )
                    )
                }
                else -> Unit
            }
        }

        val categories = byCategory.map { (category, apps) ->
            val totalMin = apps.sumOf { it.minutes }
            CategoryUsage(
                category = category,
                minutes = totalMin,
                appSessions = apps.sumOf { it.sessions },
                appCount = apps.map { it.name }.distinct().size,
                shortsViews = if (category == WebCategoryMapper.SHORT_FORM) apps.sumOf { it.sessions } else 0
            )
        }.sortedByDescending { it.minutes }

        val topApps = byCategory.values.flatten()
            .groupBy { it.name to it.category }
            .map { (key, items) ->
                AppUsageItem(
                    name = key.first,
                    minutes = items.sumOf { it.minutes },
                    sessions = items.sumOf { it.sessions },
                    category = key.second
                )
            }
            .sortedByDescending { it.minutes }
            .take(8)

        return ActivityUiState(
            loading = false,
            totalMinutes = categories.sumOf { it.minutes },
            categories = categories,
            topApps = topApps,
            donutSlices = categories.map { it.category to it.minutes }.filter { it.second > 0 }
        )
    }

    private fun durationMinutes(e: EventEntity): Long =
        payloadLong(e, "durationMs") / 60_000

    private fun payloadString(e: EventEntity, key: String): String =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""

    private fun payloadLong(e: EventEntity, key: String): Long =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toLongOrNull() ?: 0L

    private fun payloadInt(e: EventEntity, key: String): Int =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toIntOrNull() ?: 0
}
