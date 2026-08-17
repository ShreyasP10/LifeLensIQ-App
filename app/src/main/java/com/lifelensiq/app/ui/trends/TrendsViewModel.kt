package com.lifelensiq.app.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.data.local.EventEntity
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.domain.repository.EventRepository
import com.lifelensiq.app.util.JsonUtil
import com.lifelensiq.app.util.SettingsStore
import com.lifelensiq.app.util.WebCategoryMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TrendsUiState(
    val periodDays: Int = 7,
    // Totals for the selected period
    val screenMin: Long = 0,
    val productiveMin: Long = 0,
    val steps: Long = 0,
    val shorts: Long = 0,
    val pickups: Long = 0,
    // Chart buckets (daily, or monthly for 1 year)
    val chartValues: List<Long> = emptyList(),
    val chartLabels: List<String> = emptyList(),
    // Monthly comparison: this month vs previous month
    val monthProductive: Long = 0,
    val monthScreen: Long = 0,
    val monthSteps: Long = 0,
    val monthShorts: Long = 0,
    val prevProductive: Long = 0,
    val prevScreen: Long = 0,
    val prevSteps: Long = 0,
    val prevShorts: Long = 0,
    // Charging discipline (last 7 days)
    val chargeSessions: Int = 0,
    val chargeAvgMin: Long = 0,
    val chargeOvernight: Int = 0
)

class TrendsViewModel(
    private val events: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    private var cached: List<EventEntity> = emptyList()

    init {
        viewModelScope.launch {
            val from = System.currentTimeMillis() - 400L * 24 * 60 * 60 * 1000
            events.observeEvents(from, Long.MAX_VALUE).collect { list ->
                cached = list
                recompute(_uiState.value.periodDays)
            }
        }
    }

    fun setPeriod(days: Int) {
        recompute(days)
    }

    private fun recompute(days: Int) {
        val now = System.currentTimeMillis()
        val from = now - days * 86_400_000L
        val inPeriod = cached.filter { it.timestamp in from..now }
        val overrides = SettingsStore.categoryOverrides()

        fun productiveOf(e: EventEntity): Long = when (e.eventType) {
            EventType.STUDY_SESSION.id -> durationMs(e)
            EventType.APP_SESSION.id -> {
                val cat = WebCategoryMapper.categoryForPackage(payloadString(e, "packageName"), overrides)
                if (WebCategoryMapper.isProductive(cat)) durationMs(e) else 0L
            }
            else -> 0L
        }

        val screenMin = inPeriod.filter { it.eventType == EventType.APP_SESSION.id }
            .sumOf { durationMs(it) } / 60_000
        val productiveMin = inPeriod.sumOf { productiveOf(it) } / 60_000
        val steps = inPeriod.filter { it.eventType == EventType.STEPS.id }
            .sumOf { payloadLong(it, "stepDelta") }
        val shorts = inPeriod.filter { it.eventType == EventType.SHORT_VIDEO.id }
            .sumOf { payloadLong(it, "views") }
        val pickups = inPeriod.count { it.eventType == EventType.SCREEN_ON.id }.toLong()

        val (chartValues, chartLabels) = buckets(days, inPeriod)

        val monthCompare = monthlyComparison()

        val charging = chargingStats()

        _uiState.update {
            it.copy(
                periodDays = days,
                screenMin = screenMin,
                productiveMin = productiveMin,
                steps = steps,
                shorts = shorts,
                pickups = pickups,
                chartValues = chartValues,
                chartLabels = chartLabels,
                monthProductive = monthCompare.first.productive,
                monthScreen = monthCompare.first.screen,
                monthSteps = monthCompare.first.steps,
                monthShorts = monthCompare.first.shorts,
                prevProductive = monthCompare.second.productive,
                prevScreen = monthCompare.second.screen,
                prevSteps = monthCompare.second.steps,
                prevShorts = monthCompare.second.shorts,
                chargeSessions = charging.sessions,
                chargeAvgMin = charging.avgMin,
                chargeOvernight = charging.overnight
            )
        }
    }

    /** Daily buckets for <= 62 days; monthly buckets (last 12 months) for 1 year. */
    private fun buckets(days: Int, inPeriod: List<EventEntity>): Pair<List<Long>, List<String>> {
        val zone = ZoneId.systemDefault()
        if (days > 62) {
            val months = mutableListOf<Pair<String, Long>>()
            val now = LocalDate.now()
            for (i in 11 downTo 0) {
                val start = now.withDayOfMonth(1).minusMonths(i.toLong())
                val from = start.atStartOfDay(zone).toInstant().toEpochMilli()
                val to = start.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val ms = inPeriod.filter { it.timestamp in from until to && it.eventType == EventType.APP_SESSION.id }
                    .sumOf { durationMs(it) }
                months.add(start.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()) to ms / 60_000)
            }
            return months.map { it.second } to months.map { it.first }
        }
        val days_ = days.coerceAtLeast(1)
        val values = LongArray(days_) { 0L }
        val labels = Array(days_) { "" }
        val today = LocalDate.now()
        inPeriod.filter { it.eventType == EventType.APP_SESSION.id }.forEach { e ->
            val date = Instant.ofEpochMilli(e.timestamp).atZone(zone).toLocalDate()
            val idx = days_ - 1 - java.time.temporal.ChronoUnit.DAYS.between(date, today).toInt()
            if (idx in 0 until days_) values[idx] += durationMs(e)
        }
        for (i in 0 until days_) {
            val date = today.minusDays((days_ - 1 - i).toLong())
            labels[i] = if (days_ <= 7) {
                date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault())
            } else {
                date.dayOfMonth.toString()
            }
        }
        return values.map { it / 60_000 } to labels.toList()
    }

    private data class MonthTotals(val productive: Long, val screen: Long, val steps: Long, val shorts: Long)

    /** Calendar-month comparison: current month (to date) vs previous month (full). */
    private fun monthlyComparison(): Pair<MonthTotals, MonthTotals> {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()
        val currentStart = now.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val prevStart = now.withDayOfMonth(1).minusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val prevEnd = currentStart
        val overrides = SettingsStore.categoryOverrides()

        fun productiveOf(e: EventEntity): Long = when (e.eventType) {
            EventType.STUDY_SESSION.id -> durationMs(e)
            EventType.APP_SESSION.id -> {
                val cat = WebCategoryMapper.categoryForPackage(payloadString(e, "packageName"), overrides)
                if (WebCategoryMapper.isProductive(cat)) durationMs(e) else 0L
            }
            else -> 0L
        }

        fun totals(from: Long, to: Long): MonthTotals {
            val list = cached.filter { it.timestamp in from until to }
            return MonthTotals(
                productive = list.sumOf { productiveOf(it) } / 60_000,
                screen = list.filter { it.eventType == EventType.APP_SESSION.id }.sumOf { durationMs(it) } / 60_000,
                steps = list.filter { it.eventType == EventType.STEPS.id }.sumOf { payloadLong(it, "stepDelta") },
                shorts = list.filter { it.eventType == EventType.SHORT_VIDEO.id }.sumOf { payloadLong(it, "views") }
            )
        }
        return totals(currentStart, System.currentTimeMillis()) to totals(prevStart, prevEnd)
    }

    private data class ChargingStats(val sessions: Int, val avgMin: Long, val overnight: Int)

    /** Charge sessions in the last 7 days: count, average duration, overnight ones. */
    private fun chargingStats(): ChargingStats {
        val from = System.currentTimeMillis() - 7L * 86_400_000
        val starts = cached.filter { it.eventType == EventType.CHARGE_START.id && it.timestamp >= from }
        val durations = starts.mapNotNull { start ->
            cached.filter { it.eventType == EventType.CHARGE_END.id && it.timestamp >= start.timestamp }
                .minByOrNull { it.timestamp }
                ?.timestamp?.minus(start.timestamp)
                ?.takeIf { it in 5 * 60_000L..24 * 3600_000L }
        }
        val overnight = starts.count { start ->
            val hour = Instant.ofEpochMilli(start.timestamp).atZone(ZoneId.systemDefault()).hour
            hour >= 21 || hour <= 6
        }
        return ChargingStats(
            sessions = starts.size,
            avgMin = if (durations.isEmpty()) 0 else durations.sum() / durations.size / 60_000,
            overnight = overnight
        )
    }

    private fun durationMs(e: EventEntity): Long = payloadLong(e, "durationMs")

    private fun payloadString(e: EventEntity, key: String): String =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""

    private fun payloadLong(e: EventEntity, key: String): Long =
        (JsonUtil.decodePayload(e.payloadJson)[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toLongOrNull() ?: 0L
}