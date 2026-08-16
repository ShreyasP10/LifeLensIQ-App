package com.lifelensiq.app.tracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.domain.EventType
import com.lifelensiq.app.util.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen "Return to focus" notice shown when a blocked app is opened
 * during Focus Mode. Ending focus here writes the STUDY_SESSION event.
 */
class FocusBlockActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isShowing = true
        setContent {
            FocusBlockContent(
                subject = SettingsStore.focusSubject,
                startedAt = SettingsStore.focusStartMs,
                onResume = { finish() },
                onEndFocus = {
                    endFocusAndWriteSession()
                    finish()
                }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        // Pressing Home (or any other dismissal) must not leave a stale
        // block screen: finish so the next blocked app re-triggers it.
        if (!isChangingConfigurations && !isFinishing) finish()
    }

    override fun onDestroy() {
        isShowing = false
        scope.cancel()
        super.onDestroy()
    }

    private fun endFocusAndWriteSession() {
        if (!SettingsStore.focusActive) return
        val start = SettingsStore.focusStartMs
        val now = System.currentTimeMillis()
        val subject = SettingsStore.focusSubject.ifBlank { "Focus session" }
        SettingsStore.focusActive = false
        scope.launch {
            ServiceLocator.eventEmitter().emit(
                EventType.STUDY_SESSION.id,
                mapOf(
                    "subject" to subject,
                    "startedAt" to start,
                    "endedAt" to now,
                    "durationMs" to (now - start),
                    "locationType" to "FOCUS"
                )
            )
        }
    }

    companion object {
        /** Guards against launching multiple block screens from the poller. */
        @Volatile
        var isShowing: Boolean = false
            private set
    }
}

@Composable
private fun FocusBlockContent(
    subject: String,
    startedAt: Long,
    onResume: () -> Unit,
    onEndFocus: () -> Unit
) {
    var elapsed by remember { mutableLongStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            elapsed = (System.currentTimeMillis() - startedAt) / 1000
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text("Focus mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "You opened an app you blocked during your focus session.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(subject.ifBlank { "Focus session" }, fontWeight = FontWeight.SemiBold)
                Text(
                    "Elapsed ${formatElapsed(elapsed)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Return to focus")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onEndFocus, modifier = Modifier.fillMaxWidth()) {
            Text("End focus mode")
        }
    }
}

private fun formatElapsed(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}