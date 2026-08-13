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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.lifelensiq.app.ui.navigation.Routes
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel, nav: NavHostController) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("LifeLens IQ") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(greeting(), style = MaterialTheme.typography.titleLarge)
            Text(
                "Understand your time. Improve your life.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!state.usageAccessGranted) {
                BannerCard(
                    title = "Usage access not granted",
                    body = "App usage is not being recorded. Enable it to start collecting data.",
                    action = "Fix in Settings",
                    onClick = { nav.navigate(Routes.SETTINGS) }
                )
            }
            if (!state.timetableImported) {
                BannerCard(
                    title = "No timetable imported",
                    body = "Import your timetable to get class reminders and attendance tracking.",
                    action = "Import",
                    onClick = { nav.navigate(Routes.SETTINGS) }
                )
            }

            NextClassCard(
                subject = state.nextSlot?.subjectFull ?: "No classes left today",
                time = state.nextSlot?.let { "${it.start}-${it.end}" } ?: "Enjoy your free time",
                room = state.nextSlot?.room ?: ""
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Study", "${state.studyMinutesToday}m", Modifier.weight(1f))
                StatCard("Screen", "${state.screenTimeMinutesToday}m", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Shorts/Reels", state.shortsToday.toString(), Modifier.weight(1f))
                StatCard("Pending sync", state.pendingSync.toString(), Modifier.weight(1f))
            }

            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Today's schedule (${state.todaySlots.size} slots)", fontWeight = FontWeight.Bold)
                    if (state.classesToday > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Attendance: ${state.attendanceMarkedToday}/${state.classesToday} marked",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    state.todaySlots.filter { it.type.id != "FREE" }.forEach { slot ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${slot.start}-${slot.end}  ${slot.subjectFull}", style = MaterialTheme.typography.bodyMedium)
                            Text(slot.room, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(
                    label = "Mark Attendance",
                    icon = Icons.Filled.CheckCircle,
                    onClick = { nav.navigate(Routes.ATTENDANCE) },
                    modifier = Modifier.weight(1f)
                )
                QuickAction(
                    label = "Study Session",
                    icon = Icons.Filled.PlayArrow,
                    onClick = { nav.navigate(Routes.SESSIONS) },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedButton(onClick = { nav.navigate(Routes.EXPORT) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Share, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export Data (CSV / JSON / NDJSON)")
            }
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun BannerCard(title: String, body: String, action: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onClick) {
                Text(action, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun NextClassCard(subject: String, time: String, room: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Column(Modifier.padding(16.dp)) {
            Text("Next class", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(subject, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (room.isNotBlank()) {
                Text("$time · Room $room", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
            } else {
                Text(time, color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.height(72.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(6.dp))
            Text(label, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
        }
    }
}
