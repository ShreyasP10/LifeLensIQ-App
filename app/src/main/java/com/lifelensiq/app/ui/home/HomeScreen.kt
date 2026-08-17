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
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import com.lifelensiq.app.ui.components.ProductivityCalendar
import com.lifelensiq.app.ui.components.ProgressRing
import com.lifelensiq.app.ui.components.QuickAction
import com.lifelensiq.app.ui.components.StatCard
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = greeting(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!state.usageAccessGranted) {
            BannerCard(
                title = "Permission Required",
                body = "Usage access is needed to track your app activity automatically.",
                action = "Grant Access",
                onClick = { nav.navigate(Routes.SETTINGS) }
            )
        }

        Text("Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Productive Time", "${state.productiveMinutesToday}m", CategoryColors.PRODUCTIVITY, Modifier.weight(1f))
            StatCard("Screen Time", "${state.screenTimeMinutesToday}m", CategoryColors.SHORT_FORM, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Shorts", state.shortsToday.toString(), CategoryColors.SHORT_FORM, Modifier.weight(1f))
            StatCard("Steps", state.stepsToday.toString(), CategoryColors.UTILITIES, Modifier.weight(1f))
        }

        // Wake & Sleep
        Text("Rest & Wake", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Pickups", state.pickupsToday.toString(), CategoryColors.OTHER, Modifier.weight(1f))
                    StatCard("First Wake", state.firstWake ?: "—", CategoryColors.STUDY, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Last Sleep", state.lastShutdown ?: "—", CategoryColors.UTILITIES, Modifier.weight(1f))
                    StatCard("Sleep Est.", state.sleepEstimate ?: "—", CategoryColors.ENTERTAINMENT, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (state.sleepEstimate != null)
                        "Estimated from last night's shutdown to this morning's first wake."
                    else "Sleep estimate will appear after your first wake-up.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Daily goals
        Text("Daily Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProgressRing(
                        progress = state.productiveMinutesToday.toFloat() / state.productiveGoalMin.coerceAtLeast(1),
                        valueLabel = "${state.productiveMinutesToday}/${state.productiveGoalMin}m",
                        subLabel = "Productive",
                        color = CategoryColors.PRODUCTIVITY
                    )
                    ProgressRing(
                        progress = (state.screenTimeMinutesToday.toFloat() / state.screenLimitMin.coerceAtLeast(1))
                            .coerceAtMost(1f),
                        valueLabel = "${state.screenTimeMinutesToday}/${state.screenLimitMin}m",
                        subLabel = "Screen",
                        color = CategoryColors.SHORT_FORM
                    )
                }
            }
        }

        // Weekly trend
        Text("Weekly Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Productive vs Screen",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.bestDay?.let { best ->
                        Text(
                            "Best: $best",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                WeeklyBarChart(state.weeklyProductive, state.weeklyScreen)
            }
        }

        // Productivity calendar
        Text("Productivity Calendar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Days run 2 AM to 2 AM — tap the arrows to browse months",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                ProductivityCalendar(state.calendar)
            }
        }

        Spacer(Modifier.height(8.dp))

        QuickAction(
            label = "Start Productive Session",
            icon = androidx.compose.material.icons.Icons.Rounded.PlayArrow,
            onClick = { nav.navigate(Routes.SESSIONS) },
            modifier = Modifier.fillMaxWidth()
        )

        ElevatedCard(
            onClick = { nav.navigate(Routes.ACTIVITY) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    androidx.compose.material.icons.Icons.AutoMirrored.Rounded.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "Detailed Activity",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Breakdown by apps and categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { nav.navigate(Routes.EXPORT) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(androidx.compose.material.icons.Icons.Rounded.Share, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Export Your Data")
        }
        
        Spacer(Modifier.height(16.dp))
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}