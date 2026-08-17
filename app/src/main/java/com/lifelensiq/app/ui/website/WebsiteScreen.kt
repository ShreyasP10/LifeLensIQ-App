package com.lifelensiq.app.ui.website

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifelensiq.app.ui.components.DonutChart
import com.lifelensiq.app.ui.components.formatDuration
import com.lifelensiq.app.ui.theme.CategoryColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Website stats — everything the LifeLensIQ dashboard recorded, with its
 * own bottom navigation (Overview / Categories / Recent) so the mobile and
 * website sections of the app stay separate.
 */
@Composable
fun WebsiteScreen(vm: WebsiteViewModel) {
    val state by vm.uiState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Website data",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${state.eventCount} events synced from the LifeLensIQ website",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            when (state.section) {
                WebsiteSection.OVERVIEW -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Stat("Screen Time", formatDuration(state.screenMinutes * 60_000), CategoryColors.SHORT_FORM, Modifier.weight(1f))
                            Stat("Productive", formatDuration(state.productiveMinutes * 60_000), CategoryColors.PRODUCTIVITY, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Stat("Shorts/Reels", state.shortsViews.toString(), CategoryColors.TIMEPASS, Modifier.weight(1f))
                            Stat("Study logged", formatDuration(state.studySessions * 60_000), CategoryColors.STUDY, Modifier.weight(1f))
                        }
                    }
                    item {
                        Text(
                            "The dashboard records every productive, timepass, study and entertainment session you browse — study, DSA, development, LinkedIn and more.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                WebsiteSection.CATEGORIES -> {
                    item {
                        Text("By category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (state.donutSlices.isEmpty()) {
                        item { Text("No website data yet — open the website to start tracking.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        item { DonutChart(state.donutSlices) }
                        items(state.categories) { (category, minutes) ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        formatDuration(minutes * 60_000),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${minutes}m",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CategoryColors.forCategory(category)
                                )
                            }
                        }
                    }
                }
                WebsiteSection.RECENT -> {
                    item {
                        Text("Recent website sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (state.recent.isEmpty()) {
                        item { Text("No website data yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(state.recent) { item ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                item.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            Text(
                                                "${item.domain} · ${item.eventType}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            formatDuration(item.minutes * 60_000),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CategoryColors.forCategory(item.category)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        Instant.ofEpochMilli(item.at).atZone(ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofPattern("d MMM, h:mm a")),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Website section's own bottom navigation bar.
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            WebsiteSection.entries.forEach { section ->
                NavigationBarItem(
                    selected = state.section == section,
                    onClick = { vm.setSection(section) },
                    icon = { Text(if (state.section == section) "●" else "○", style = MaterialTheme.typography.bodyMedium) },
                    label = {
                        Text(
                            section.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (state.section == section) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}