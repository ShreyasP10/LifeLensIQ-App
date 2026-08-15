package com.lifelensiq.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lifelensiq.app.ui.components.BannerCard
import com.lifelensiq.app.ui.components.ProgressRing
import com.lifelensiq.app.ui.components.QuickAction
import com.lifelensiq.app.ui.components.StatCard
import com.lifelensiq.app.ui.components.StreakHeatmap
import com.lifelensiq.app.ui.components.WeeklyBarChart
import com.lifelensiq.app.ui.navigation.Routes
import com.lifelensiq.app.ui.theme.CategoryColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(vm: HomeViewModel, nav: NavHostController) {
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Column {
            Text(greeting(), style = MaterialTheme.typography.titleLarge)
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!state.usageAccessGranted) {
            BannerCard(
                title = "Usage access not granted",
                body = "App usage is not being recorded. Enable it to start collecting data.",
                action = "Fix in Settings",
                onClick = { nav.navigate(Routes.SETTINGS) }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Study", "${state.studyMinutesToday}m", CategoryColors.STUDY, Modifier.weight(1f))
            StatCard("Screen", "${state.screenTimeMinutesToday}m", CategoryColors.PRODUCTIVITY, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Shorts/Reels", state.shortsToday.toString(), CategoryColors.SHORT_FORM, Modifier.weight(1f))
            StatCard("Steps", state.stepsToday.toString(), CategoryColors.UTILITIES, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Sessions", state.appSessionsToday.toString(), CategoryColors.ENTERTAINMENT, Modifier.weight(1f))
            StatCard("Pending sync", state.pendingSync.toString(), CategoryColors.OTHER, Modifier.weight(1f))
        }

        // Daily goals
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Daily goals", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProgressRing(
                        progress = state.studyMinutesToday.toFloat() / state.studyGoalMin.coerceAtLeast(1),
                        valueLabel = "${state.studyMinutesToday}/${state.studyGoalMin}m",
                        subLabel = "Study goal",
                        color = CategoryColors.STUDY
                    )
                    ProgressRing(
                        progress = (state.screenTimeMinutesToday.toFloat() / state.screenLimitMin.coerceAtLeast(1))
                            .coerceAtMost(1f),
                        valueLabel = "${state.screenTimeMinutesToday}/${state.screenLimitMin}m",
                        subLabel = "Screen limit",
                        color = CategoryColors.SHORT_FORM
                    )
                }
            }
        }

        // Weekly trend
        Card {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("This week", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    state.bestDay?.let { best ->
                        Text(
                            "Best: $best",
                            style = MaterialTheme.typography.labelMedium,
                            color = CategoryColors.STUDY
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                WeeklyBarChart(state.weeklyStudy, state.weeklyScreen)
            }
        }

        // Productivity heatmap (same calendar the website shows)
        Card {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Productivity calendar",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Study minutes per day — app + website data combined",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                StreakHeatmap(state.heatmap)
            }
        }

        Card(
            onClick = { nav.navigate(Routes.ACTIVITY) },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Today's Activity",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "See time by category — Study, DSA, Timepass, Shorts & more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        QuickAction(
            label = "Study Session",
            icon = Icons.Filled.PlayArrow,
            onClick = { nav.navigate(Routes.SESSIONS) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(onClick = { nav.navigate(Routes.EXPORT) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Share, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Export Data (CSV / JSON / NDJSON)")
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}