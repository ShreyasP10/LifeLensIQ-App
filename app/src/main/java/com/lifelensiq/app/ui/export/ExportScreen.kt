package com.lifelensiq.app.ui.export

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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifelensiq.app.export.ExportFormat

@Composable
fun ExportScreen(vm: ExportViewModel) {
    val state by vm.uiState.collectAsState()

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(state.format.mimeType)
    ) { uri: Uri? ->
        uri?.let { vm.exportTo(it.toString()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Format", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormat.entries.forEach { format ->
                        FilterChip(
                            selected = state.format == format,
                            onClick = { vm.setFormat(format) },
                            label = { Text(format.label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "CSV opens directly in Excel (UTF-8 with BOM). " +
                        "JSON includes envelope + events; NDJSON streams one event per line.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = {
                val name = "lifelensiq_export_${System.currentTimeMillis()}.${state.format.label.lowercase()}"
                createDocument.launch(name)
            },
            enabled = !state.isExporting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isExporting) {
                CircularProgressIndicator(Modifier.height(20.dp))
            } else {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = null,
                    Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Choose location & Export")
            }
        }

        state.lastMessage?.let { msg ->
            Text(
                msg,
                color = if (msg.startsWith("Exported")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Note", fontWeight = FontWeight.Bold)
                Text(
                    "Exports read from the local Room database (works offline). " +
                        "Use this data to train your ML model — see assets/docs/05_ML_Data_Strategy.md.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
