package com.lifelensiq.app.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.navigation.NavHostController
import com.lifelensiq.app.ui.navigation.Routes

@Composable
fun SettingsScreen(vm: SettingsViewModel, nav: NavHostController) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    val stepPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        vm.refreshStepsPermission()
        if (granted) {
            vm.restartTracking()
        }
    }

    var studyGoal by remember { mutableStateOf(state.studyGoalMin.toString()) }
    var screenLimit by remember { mutableStateOf(state.screenLimitMin.toString()) }
    var shortsViews by remember { mutableStateOf(state.shortsAlertViews.toString()) }

    var confirmDeleteLocal by remember { mutableStateOf(false) }
    var confirmDeleteCloud by remember { mutableStateOf(false) }

    if (confirmDeleteLocal) {
        AlertDialog(
            onDismissRequest = { confirmDeleteLocal = false },
            title = { Text("Delete local data?") },
            text = { Text("All events and sync history on this device will be removed. Cloud data stays intact.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteLocal = false
                    vm.deleteLocalData()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteLocal = false }) { Text("Cancel") } }
        )
    }

    if (confirmDeleteCloud) {
        AlertDialog(
            onDismissRequest = { confirmDeleteCloud = false },
            title = { Text("Delete cloud data?") },
            text = { Text("All your events in Firestore will be deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteCloud = false
                    vm.deleteCloudData()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteCloud = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.message?.let { msg ->
            Text(
                msg,
                color = if (msg.startsWith("Deleted") || msg.startsWith("Synced"))
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Tracking", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Usage access: " + if (state.usageAccessGranted) "GRANTED" else "NOT GRANTED — app usage is not recorded",
                    color = if (state.usageAccessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.openUsageAccessSettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Usage Access Settings")
                }
                OutlinedButton(
                    onClick = { vm.refreshUsageAccess() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Re-check permission")
                }
                OutlinedButton(
                    onClick = { vm.restartTracking() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Restart tracking service")
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Goals & notifications", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = studyGoal,
                    onValueChange = { studyGoal = it.filter(Char::isDigit).take(4) },
                    label = { Text("Daily study goal (minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = screenLimit,
                    onValueChange = { screenLimit = it.filter(Char::isDigit).take(4) },
                    label = { Text("Daily screen-time limit (minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = shortsViews,
                    onValueChange = { shortsViews = it.filter(Char::isDigit).take(5) },
                    label = { Text("Shorts nudge threshold (views)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        vm.updateGoals(
                            studyGoal.toIntOrNull() ?: state.studyGoalMin,
                            screenLimit.toIntOrNull() ?: state.screenLimitMin,
                            shortsViews.toIntOrNull() ?: state.shortsAlertViews
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save goals") }
                Spacer(Modifier.height(8.dp))
                ToggleRow("Daily summary (9 PM)", state.dailySummaryEnabled, vm::setDailySummary)
                ToggleRow("Screen-limit alert", state.screenLimitAlertEnabled, vm::setScreenLimitAlert)
                ToggleRow("Shorts nudge", state.shortsNudgeEnabled, vm::setShortsNudge)
                ToggleRow("Bedtime reminder (10:30 PM)", state.bedtimeReminderEnabled, vm::setBedtimeReminder)
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("App categories", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Reclassify any installed app into a different category (Study, Timepass, …).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { nav.navigate(Routes.CATEGORY_OVERRIDES) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.List, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit app categories")
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Steps", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Step tracking: " + if (state.stepsPermissionGranted) "ENABLED" else "DISABLED — grant activity recognition to collect step data",
                    color = if (state.stepsPermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { stepPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.stepsPermissionGranted) "Re-check step permission" else "Enable step tracking")
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Reels & Shorts", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Counts Instagram/Facebook Reels and YouTube Shorts viewed (accessibility heuristic). " +
                        if (state.shortsDetectorEnabled) "ENABLED" else "DISABLED — enable it to track short-form video on your phone.",
                    color = if (state.shortsDetectorEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.openAccessibilitySettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.shortsDetectorEnabled) "Manage in Accessibility settings" else "Enable Reels & Shorts detection")
                }
                OutlinedButton(
                    onClick = { vm.refreshShortsDetector() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Re-check status")
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Sync", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Syncs every 15 min automatically. Uploads app events and downloads events written by the web dashboard, so app + website data stay in one place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.syncNow() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sync now")
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Privacy", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Only metadata is collected — app-usage, screen, charging, steps and study events. " +
                        "No messages, calls, media, location or keyboard input. " +
                        "Data stays on-device until you sign in, then syncs only to your account. " +
                        "No analytics or ads. You can delete everything below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cloud sync is only available after signing in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Danger zone", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { confirmDeleteLocal = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Delete, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete local data")
                }
                OutlinedButton(onClick = { confirmDeleteCloud = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Delete, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete cloud data (Firestore)")
                }
            }
        }

        Button(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth()) {
            Text("Logout")
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}