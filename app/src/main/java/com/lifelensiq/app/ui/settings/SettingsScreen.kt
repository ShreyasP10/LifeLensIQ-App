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

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        vm.refreshNotifications()
        if (granted) {
            vm.restartTracking()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        vm.refreshAll()
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
                Text("Permissions", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Grant or revoke every permission from here — no need to hunt through system Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                PermissionRow(
                    title = "Usage access",
                    description = "Records which apps you open",
                    granted = state.usageAccessGranted,
                    onGrant = { vm.openUsageAccessSettings(context) },
                    onRevoke = { vm.openUsageAccessSettings(context) }
                )
                PermissionRow(
                    title = "Notifications",
                    description = "Daily summary, morning report, alerts",
                    granted = state.notificationsEnabled,
                    onGrant = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    onRevoke = { vm.revokeNotifications(context) }
                )
                PermissionRow(
                    title = "Activity recognition (steps)",
                    description = "Counts steps walked",
                    granted = state.stepsPermissionGranted,
                    onGrant = { stepPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                    onRevoke = { vm.revokeStepsPermission(context) }
                )
                PermissionRow(
                    title = "Accessibility (Reels & Shorts)",
                    description = "Detects short-form video (optional)",
                    granted = state.shortsDetectorEnabled,
                    onGrant = { vm.openAccessibilitySettings(context) },
                    onRevoke = { vm.openAccessibilitySettings(context) }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Usage access and accessibility are Android system-level — tapping Grant/Revoke opens the exact system page for you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
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
                ToggleRow("Morning report (first wake)", state.morningReportEnabled, vm::setMorningReport)
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

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit,
    onRevoke: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (granted) "GRANTED" else "OFF",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onGrant) { Text("Grant") }
            TextButton(onClick = onRevoke) { Text("Revoke", color = MaterialTheme.colorScheme.error) }
        }
    }
}