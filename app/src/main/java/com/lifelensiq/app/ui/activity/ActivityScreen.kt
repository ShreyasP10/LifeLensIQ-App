package com.lifelensiq.app.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifelensiq.app.ui.components.DonutChart
import com.lifelensiq.app.ui.components.formatDuration
import com.lifelensiq.app.ui.theme.CategoryColors

@Composable
fun ActivityScreen(vm: ActivityViewModel, onCategoryClick: (String) -> Unit) {
    val state by vm.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeviceFilter.entries.forEach { filter ->
                    androidx.compose.material3.FilterChip(
                        selected = state.deviceFilter == filter,
                        onClick = { vm.setDeviceFilter(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total tracked time today", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            formatDuration(state.totalMinutes),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (state.categories.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "${state.categories.size} categories · ${state.categories.sumOf { it.appSessions }} sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (state.loading) {
            item { Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        if (state.donutSlices.isNotEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Time by category", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        DonutChart(state.donutSlices, Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (state.categories.isEmpty() && !state.loading) {
            item {
                Text(
                    "No activity recorded today yet. Grant Usage Access and use your phone — sessions appear here by category.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(state.categories, key = { it.category }) { usage ->
            CategoryCard(usage, maxMinutes = state.categories.firstOrNull()?.minutes ?: 1, onClick = {
                onCategoryClick(usage.category)
            })
        }

        if (state.topApps.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Top apps today", style = MaterialTheme.typography.titleMedium)
            }
            items(state.topApps, key = { it.name + it.category }) { app ->
                AppRow(app)
            }
        }
    }
}

@Composable
private fun CategoryCard(usage: CategoryUsage, maxMinutes: Long, onClick: () -> Unit) {
    val color = CategoryColors.forCategory(usage.category)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(usage.category, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatDuration(usage.minutes),
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (maxMinutes > 0) usage.minutes.toFloat() / maxMinutes else 0f },
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(Modifier.height(6.dp))
                val details = buildString {
                    if (usage.shortsViews > 0) append("${usage.shortsViews} shorts · ")
                    append("${usage.appCount} apps · ")
                    append("${usage.appSessions} sessions")
                }
                Text(details, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AppRow(app: AppUsageItem) {
    val color = CategoryColors.forCategory(app.category)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                app.name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${formatDuration(app.minutes)} · ${app.sessions}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
