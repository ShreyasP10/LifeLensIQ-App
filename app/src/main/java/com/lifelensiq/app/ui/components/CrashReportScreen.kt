package com.lifelensiq.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CrashReportScreen(
    logText: String,
    onDismiss: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Crash Report", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "The app crashed on the previous run. Please copy the text below and send it so the bug can be fixed.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = logText,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Dismiss")
            }
        }
    }
}
