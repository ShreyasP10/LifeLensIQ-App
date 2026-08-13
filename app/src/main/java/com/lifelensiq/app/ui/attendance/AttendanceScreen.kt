package com.lifelensiq.app.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifelensiq.app.domain.AttendanceStatus
import com.lifelensiq.app.domain.model.TimetableSlot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(vm: AttendanceViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Attendance — Today") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.todaySlots.isEmpty()) {
                item { Text("No lectures/labs today (or timetable not imported).") }
            }
            state.todaySlots.forEach { slot ->
                item { AttendanceCard(slot, state.marked[slot.slotNo.toString()], vm::mark) }
            }
        }
    }
}

@Composable
private fun AttendanceCard(slot: TimetableSlot, marked: String?, onMark: (TimetableSlot, AttendanceStatus) -> Unit) {
    val statusColor = when (marked) {
        AttendanceStatus.ATTENDED.name -> MaterialTheme.colorScheme.primary
        AttendanceStatus.SKIPPED.name -> MaterialTheme.colorScheme.error
        AttendanceStatus.ONLINE.name, AttendanceStatus.LATE.name -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${slot.start} - ${slot.end}", fontWeight = FontWeight.Bold)
                Text(slot.type.id, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${slot.subjectFull} — Room ${slot.room}")
            Spacer(Modifier.height(8.dp))

            if (marked != null) {
                Text("Marked: $marked", color = statusColor, fontWeight = FontWeight.SemiBold)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onMark(slot, AttendanceStatus.ATTENDED) }, modifier = Modifier.weight(1f)) { Text("Attended") }
                    OutlinedButton(onClick = { onMark(slot, AttendanceStatus.SKIPPED) }, modifier = Modifier.weight(1f)) { Text("Skipped") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    OutlinedButton(onClick = { onMark(slot, AttendanceStatus.ONLINE) }, modifier = Modifier.weight(1f)) { Text("Online") }
                    OutlinedButton(onClick = { onMark(slot, AttendanceStatus.LATE) }, modifier = Modifier.weight(1f)) { Text("Late") }
                }
            }
        }
    }
}
