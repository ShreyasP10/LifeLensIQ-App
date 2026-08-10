package com.lifeiq.app.ui.sessions

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(vm: SessionsViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsState()
    var selected by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Study Sessions") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = if (state.active)
                    androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) else androidx.compose.material3.CardDefaults.cardColors()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Session status", fontWeight = FontWeight.Bold)
                    Text(
                        if (state.active) "ACTIVE — ${state.activeSubject}" else "No active session",
                        color = if (state.active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    if (state.subjects.isEmpty()) {
                        Text("Import your timetable first to get subjects.")
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = selected ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Subject") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                                state.subjects.forEach { subject ->
                                    DropdownMenuItem(
                                        text = { Text(subject) },
                                        onClick = {
                                            selected = subject
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (state.active) vm.stopSession() else selected?.let { vm.startSession(it) }
                        },
                        enabled = state.active || selected != null,
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

            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Today's sessions", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (state.todaySessions.isEmpty()) {
                        Text("No sessions logged today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.todaySessions.forEach {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
