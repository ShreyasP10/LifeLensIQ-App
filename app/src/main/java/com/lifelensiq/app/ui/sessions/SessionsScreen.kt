package com.lifelensiq.app.ui.sessions

import android.content.pm.PackageManager
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SessionsScreen(vm: SessionsViewModel) {
    val state by vm.uiState.collectAsState()
    var subject by remember { mutableStateOf("") }
    var focusSubject by remember { mutableStateOf("") }
    var manualSubject by remember { mutableStateOf("") }
    var manualMinutes by remember { mutableStateOf("30") }
    var showAppPicker by remember { mutableStateOf(false) }
    val timeFormat = DateTimeFormatter.ofPattern("d MMM, h:mm a")

    if (showAppPicker) {
        BlockedAppsDialog(
            selected = state.focusBlockedApps,
            onToggle = vm::toggleFocusApp,
            onDismiss = { showAppPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = if (state.active)
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) else CardDefaults.cardColors()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Session status", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (state.active) {
                        Text(
                            formatElapsed(state.elapsedSeconds),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Text(
                    if (state.active) "ACTIVE — ${state.activeSubject}" else "No active session",
                    color = if (state.active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    enabled = !state.active,
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (state.active) {
                            vm.stopSession()
                        } else {
                            vm.startSession(subject)
                            subject = ""
                        }
                    },
                    enabled = state.active || subject.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (state.active) Icons.Filled.Check else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.active) "Stop Session" else "Start Session")
                }
            }
        }

        // Focus mode
        Card(
            colors = if (state.focusActive)
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            else CardDefaults.cardColors()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        Modifier.size(18.dp),
                        tint = if (state.focusActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Focus mode", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (state.focusActive) {
                        Text(
                            formatElapsed(state.focusElapsedSeconds),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    if (state.focusActive) "ACTIVE — ${state.focusSubject}" else "Block distracting apps while studying",
                    color = if (state.focusActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.focusActive) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Opening a blocked app pulls you back to a full-screen notice. " +
                            "Ending focus writes a study session (locationType FOCUS).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = focusSubject,
                    onValueChange = { focusSubject = it },
                    enabled = !state.focusActive,
                    label = { Text("Focus subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (state.focusBlockedApps.isNotEmpty()) {
                    Text(
                        "Blocking ${state.focusBlockedApps.size} app(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showAppPicker = true },
                        enabled = !state.focusActive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Choose apps")
                    }
                    Button(
                        onClick = {
                            if (state.focusActive) {
                                vm.endFocus()
                            } else {
                                vm.startFocus(focusSubject.ifBlank { "Focus session" }, state.focusBlockedApps)
                                focusSubject = ""
                            }
                        },
                        enabled = state.focusActive || focusSubject.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.focusActive) "End Focus" else "Start Focus")
                    }
                }
            }
        }

        // Manual logger
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Quick log", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Add a study session you finished without the timer — shows up in history and syncs to the website.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manualSubject,
                        onValueChange = { manualSubject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.weight(1.6f)
                    )
                    OutlinedTextField(
                        value = manualMinutes,
                        onValueChange = { manualMinutes = it.filter(Char::isDigit).take(4) },
                        label = { Text("Minutes") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        vm.logStudyManually(manualSubject, manualMinutes.toIntOrNull() ?: 0)
                        manualSubject = ""
                        manualMinutes = "30"
                    },
                    enabled = manualSubject.isNotBlank() && (manualMinutes.toIntOrNull() ?: 0) > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log study session")
                }
            }
        }

        state.lastSummary?.let { summary ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    summary,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Session history (30 days)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (state.history.isEmpty()) {
                    Text(
                        "No sessions logged yet. Start one above.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.history.forEach {
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(it.subject, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                Instant.ofEpochMilli(it.endedAt)
                                    .atZone(ZoneId.systemDefault()).toLocalDateTime()
                                    .format(timeFormat),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${it.durationMin} min",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedAppsDialog(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val apps = remember {
        runCatching {
            val pm = context.packageManager
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).mapNotNull { resolve ->
                val pkg = resolve.activityInfo.packageName
                if (pkg == context.packageName) null else {
                    val label = runCatching {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    }.getOrDefault(pkg)
                    pkg to label
                }
            }.sortedBy { it.second }
        }.getOrDefault(emptyList())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Block during focus") },
        text = {
            LazyColumn(Modifier.height(360.dp)) {
                items(apps, key = { it.first }) { (pkg, label) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = pkg in selected,
                            onCheckedChange = { onToggle(pkg) }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

private fun formatElapsed(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}