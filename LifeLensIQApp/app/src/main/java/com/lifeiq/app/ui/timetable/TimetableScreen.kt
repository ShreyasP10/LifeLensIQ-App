package com.lifeiq.app.ui.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeiq.app.domain.model.TimetableSlot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(vm: TimetableViewModel, onBack: () -> Unit) {
    val all by vm.all.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Weekly Timetable") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val byDay = all.groupBy { it.day }
            DAYS.forEach { day ->
                item {
                    Text(
                        day,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                val slots = byDay[day].orEmpty()
                if (slots.isEmpty()) {
                    item { Text("No timetable imported yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                slots.forEach { slot ->
                    item { SlotRow(slot) }
                }
            }
        }
    }
}

@Composable
private fun SlotRow(slot: TimetableSlot) {
    val muted = !slot.applicable || slot.type.id == "FREE"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (muted) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${slot.start} - ${slot.end}  ${slot.subjectFull}",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (slot.room.isNotBlank()) {
                    Text("Room: ${slot.room}  •  ${slot.faculty}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                slot.type.id,
                style = MaterialTheme.typography.labelSmall,
                color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
        }
    }
}

private val DAYS = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
