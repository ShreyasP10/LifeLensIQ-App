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

data class CategoryDetailState(
    val loading: Boolean = true,
    val totalMinutes: Long = 0,
    val apps: List<AppUsageItem> = emptyList()
)

/** One page per category: every app/domain in it with time spent today. */
class CategoryDetailViewModel(
    private val events: EventRepository,
    private val category: String
) : ViewModel() {

    private val todayStart = TimeUtils.todayEpochStart()
    private val _uiState = MutableStateFlow(CategoryDetailState())
    val uiState: StateFlow<CategoryDetailState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            events.observeEvents(todayStart, Long.MAX_VALUE).collect { list ->
                val filtered = list.filter { event -> categoryOf(event) == category }
                val apps = filtered.map { event -> itemFor(event) }
                    .filterNotNull()
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
                _uiState.value = CategoryDetailState(
                    loading = false,
                    totalMinutes = apps.sumOf { it.minutes },
                    apps = apps
                )
            }
        }
    }

    private fun categoryOf(e: EventEntity): String? = when (e.eventType) {
        EventType.APP_SESSION.id -> WebCategoryMapper.categoryForPackage(payloadString(e, "packageName"), overrides)
        EventType.SHORT_VIDEO.id -> WebCategoryMapper.SHORT_FORM
        EventType.STUDY_SESSION.id -> WebCategoryMapper.STUDY
        else -> null
    }

    private val overrides = SettingsStore.categoryOverrides()

    private fun itemFor(e: EventEntity): AppUsageItem? = when (e.eventType) {
        EventType.APP_SESSION.id -> {
            val pkg = payloadString(e, "packageName")
            AppUsageItem(
                name = payloadString(e, "appName").takeIf { it.isNotBlank() } ?: pkg,
                minutes = payloadLong(e, "durationMs") / 60_000,
                sessions = 1,
                category = WebCategoryMapper.categoryForPackage(pkg, overrides)
            )
        }
        EventType.SHORT_VIDEO.id -> AppUsageItem(
            name = "Reels / Shorts",
            minutes = payloadLong(e, "durationMs") / 60_000,
            sessions = payloadInt(e, "views"),
            category = WebCategoryMapper.SHORT_FORM
        )
        EventType.STUDY_SESSION.id -> AppUsageItem(
            name = "Study session",
            minutes = payloadLong(e, "durationMs") / 60_000,
            sessions = 1,
            category = WebCategoryMapper.STUDY
        )
        else -> null
    }

    private fun payloadString(e: EventEntity, key: String): String =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""

    private fun payloadLong(e: EventEntity, key: String): Long =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toLongOrNull() ?: 0L

    private fun payloadInt(e: EventEntity, key: String): Int =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toIntOrNull() ?: 0
}
