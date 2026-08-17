package com.lifelensiq.app.ui.website

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.data.local.EventEntity
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.domain.repository.EventRepository
import com.lifelensiq.app.util.JsonUtil
import com.lifelensiq.app.util.TimeUtils
import com.lifelensiq.app.util.WebCategoryMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class WebsiteSection(val label: String) {
    OVERVIEW("Overview"),
    CATEGORIES("Categories"),
    RECENT("Recent")
}

data class WebEventItem(
    val title: String,
    val domain: String,
    val eventType: String,
    val category: String,
    val minutes: Long,
    val at: Long
)

data class WebsiteUiState(
    val section: WebsiteSection = WebsiteSection.OVERVIEW,
    val loading: Boolean = true,
    val eventCount: Long = 0,
    val screenMinutes: Long = 0,
    val productiveMinutes: Long = 0,
    val shortsViews: Long = 0,
    val studySessions: Long = 0,
    val categories: List<Pair<String, Long>> = emptyList(),
    val donutSlices: List<Pair<String, Long>> = emptyList(),
    val recent: List<WebEventItem> = emptyList()
)

/**
 * Everything the website (LifeLensIQ-Web) recorded: every synced event whose
 * device is not this phone (chrome/firefox/web dashboards). Mirrors the
 * dashboard's category grouping so both sides show the same numbers.
 */
class WebsiteViewModel(
    private val events: EventRepository,
    private val thisDeviceId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebsiteUiState())
    val uiState: StateFlow<WebsiteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val from = System.currentTimeMillis() - 400L * 24 * 60 * 60 * 1000
            events.observeEvents(from, Long.MAX_VALUE).collect { all ->
                val web = all.filter { it.deviceId != thisDeviceId && it.deviceId.isNotBlank() }
                val todayStart = TimeUtils.todayEpochStart()

                val screenMinutes = web.sumOf { durationMs(it) } / 60_000
                val productive = web.filter { categoryOf(it)?.let(WebCategoryMapper::isProductive) == true }
                    .sumOf { durationMs(it) } / 60_000
                val shortsViews = web.filter { it.eventType == EventType.SHORT_VIDEO.id }
                    .sumOf { payloadLong(it, "views") }
                val studySessions = web.filter { it.eventType == EventType.STUDY_SESSION.id }
                    .sumOf { durationMs(it) } / 60_000

                val byCategory = linkedMapOf<String, Long>()
                web.forEach { e ->
                    val cat = categoryOf(e) ?: return@forEach
                    byCategory[cat] = (byCategory[cat] ?: 0L) + durationMs(e)
                }
                val categories = byCategory.map { it.key to it.value / 60_000 }
                    .filter { it.second > 0 }
                    .sortedByDescending { it.second }

                val recent = web.sortedByDescending { it.timestamp }
                    .take(30)
                    .map { e ->
                        WebEventItem(
                            title = payloadString(e, "title").ifBlank {
                                payloadString(e, "path").ifBlank { e.eventType }
                            },
                            domain = payloadString(e, "domain").ifBlank { e.deviceId },
                            eventType = e.eventType,
                            category = categoryOf(e) ?: WebCategoryMapper.OTHER,
                            minutes = durationMs(e) / 60_000,
                            at = e.timestamp
                        )
                    }

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    eventCount = web.size.toLong(),
                    screenMinutes = screenMinutes,
                    productiveMinutes = productive,
                    shortsViews = shortsViews,
                    studySessions = studySessions,
                    categories = categories,
                    donutSlices = categories,
                    recent = recent
                )
            }
        }
    }

    fun setSection(section: WebsiteSection) {
        _uiState.value = _uiState.value.copy(section = section)
    }

    private fun categoryOf(e: EventEntity): String? {
        val payloadCat = payloadString(e, "category")
        if (payloadCat.isNotBlank()) return payloadCat
        val domain = payloadString(e, "domain")
        val path = payloadString(e, "path")
        return WebCategoryMapper.categoryFor(e.eventType, domain.ifBlank { path }.ifBlank { null })
    }

    private fun durationMs(e: EventEntity): Long = payloadLong(e, "durationMs")

    private fun payloadString(e: EventEntity, key: String): String =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""

    private fun payloadLong(e: EventEntity, key: String): Long =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toLongOrNull() ?: 0L
}