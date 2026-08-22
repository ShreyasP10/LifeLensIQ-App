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
                            durationMs = items.sumOf { it.durationMs },
                            sessions = items.sumOf { it.sessions },
                            category = key.second
                        )
                    }
                    .sortedByDescending { it.durationMs }
                _uiState.value = CategoryDetailState(
                    loading = false,
                    totalMinutes = apps.sumOf { it.durationMs } / 60_000,
                    apps = apps
                )
            }
        }
    }

    private fun categoryOf(e: EventEntity): String? {
        val payloadCat = payloadString(e, "category")
        if (payloadCat.isNotBlank()) return payloadCat

        return when (e.eventType) {
            EventType.APP_SESSION.id -> WebCategoryMapper.categoryForPackage(payloadString(e, "packageName"), overrides)
            EventType.SHORT_VIDEO.id -> WebCategoryMapper.SHORT_FORM
            EventType.STUDY_SESSION.id -> WebCategoryMapper.STUDY
            else -> {
                val domain = payloadString(e, "domain")
                val path = payloadString(e, "path")
                WebCategoryMapper.categoryFor(e.eventType, domain.ifBlank { path }.ifBlank { null })
            }
        }
    }

    private val overrides = SettingsStore.categoryOverrides()

    private fun itemFor(e: EventEntity): AppUsageItem? {
        val cat = categoryOf(e) ?: return null
        val name = when (e.eventType) {
            EventType.APP_SESSION.id -> {
                val pkg = payloadString(e, "packageName")
                payloadString(e, "appName").takeIf { it.isNotBlank() } ?: pkg
            }
            EventType.SHORT_VIDEO.id -> "Reels / Shorts"
            EventType.STUDY_SESSION.id -> "Study session"
            else -> {
                payloadString(e, "title").ifBlank {
                    payloadString(e, "domain").ifBlank {
                        payloadString(e, "path").ifBlank { e.eventType }
                    }
                }
            }
        }
        return AppUsageItem(
            name = name,
            durationMs = payloadLong(e, "durationMs"),
            sessions = if (e.eventType == EventType.SHORT_VIDEO.id) payloadInt(e, "views") else 1,
            category = cat
        )
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
