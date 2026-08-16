package com.lifelensiq.app.ui.trends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifelensiq.app.ui.components.SingleBarChart
import com.lifelensiq.app.ui.theme.CategoryColors

private val PERIODS = listOf(1 to "1D", 7 to "7D", 30 to "30D", 365 to "1Y")

@Composable
fun TrendsScreen(vm: TrendsViewModel) {
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PERIODS.forEach { (days, label) ->
                FilterChip(
                    selected = state.periodDays == days,
                    onClick = { vm.setPeriod(days) },
                    label = { Text(label) },
                    shape = MaterialTheme.shapes.medium
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Screen time — last ${periodLabel(state.periodDays)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                SingleBarChart(
                    values = state.chartValues,
                    labels = state.chartLabels,
                    color = CategoryColors.PRODUCTIVITY
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Overview — Last ${periodLabel(state.periodDays)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                TrendRow("Screen time", formatMinutes(state.screenMin), CategoryColors.PRODUCTIVITY)
                TrendRow("Study time", formatMinutes(state.studyMin), CategoryColors.STUDY)
                TrendRow("Steps", state.steps.toString(), CategoryColors.UTILITIES)
                TrendRow("Reels / Shorts", state.shorts.toString(), CategoryColors.SHORT_FORM)
                TrendRow("Phone pickups", state.pickups.toString(), CategoryColors.OTHER)
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "This month vs last month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Current month (to today) vs the full previous month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                CompareRow("Study", state.monthStudy, state.prevStudy, CategoryColors.STUDY)
                CompareRow("Screen", state.monthScreen, state.prevScreen, CategoryColors.PRODUCTIVITY)
                CompareRow("Steps", state.monthSteps, state.prevSteps, CategoryColors.UTILITIES)
                CompareRow("Shorts", state.monthShorts, state.prevShorts, CategoryColors.SHORT_FORM)
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Charging discipline (last 7 days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                TrendRow("Charge sessions", state.chargeSessions.toString(), CategoryColors.UTILITIES)
                TrendRow("Avg charge time", if (state.chargeAvgMin > 0) formatMinutes(state.chargeAvgMin) else "—", CategoryColors.UTILITIES)
                TrendRow("Overnight charges", state.chargeOvernight.toString(), CategoryColors.OTHER)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Overnight charging can wear the battery. Try unplugging at ~80%.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrendRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun CompareRow(label: String, current: Long, prev: Long, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "${formatMinutes(current)} vs ${formatMinutes(prev)}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            pct(current, prev),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                prev <= 0 && current > 0 -> color
                prev <= 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                current >= prev -> MaterialTheme.colorScheme.error
                else -> color
            }
        )
    }
}

private fun pct(current: Long, prev: Long): String {
    if (prev <= 0) return if (current > 0) "new" else "—"
    val d = (current - prev) * 100 / prev
    return if (d >= 0) "+$d%" else "$d%"
}

private fun formatMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun periodLabel(days: Int): String = when (days) {
    1 -> "day"
    7 -> "week"
    30 -> "30 days"
    365 -> "year"
    else -> "$days days"
}