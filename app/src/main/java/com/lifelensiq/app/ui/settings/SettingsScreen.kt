package com.lifelensiq.app.ui.settings

import android.Manifest
import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { vm.importTimetable(it) } }

    val stepPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        vm.refreshStepsPermission()
        if (granted) {
            vm.restartTracking()
        }
    }

    var confirmDeleteLocal by remember { mutableStateOf(false) }
    var confirmDeleteCloud by remember { mutableStateOf(false) }

    if (confirmDeleteLocal) {
        AlertDialog(
            onDismissRequest = { confirmDeleteLocal = false },
            title = { Text("Delete local data?") },
            text = { Text("All events, timetable and sync history on this device will be removed. Cloud data stays intact.") },
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.message?.let { msg ->
                Text(
                    msg,
                    color = if (msg.startsWith("Imported") || msg.startsWith("Synced") || msg.startsWith("Deleted"))
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
                    Text("Timetable", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Import your personalized timetable to unlock Home, Attendance and Sessions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.importBundledSample() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Import bundled sample (B1 + IP)")
                    }
                    OutlinedButton(
                        onClick = { filePicker.launch(arrayOf("application/json")) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Import timetable JSON file…")
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
                        "Syncs every 15 min automatically. Requires a working Firebase config.",
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
}
