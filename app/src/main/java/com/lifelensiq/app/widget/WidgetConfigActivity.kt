package com.lifelensiq.app.widget

import android.appwidget.AppWidgetManager
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifelensiq.app.R
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.ui.theme.LifeLensIQTheme
import com.lifelensiq.app.util.SettingsStore

/**
 * Widget configuration: theme (dark/light) and which stats to show.
 * Invoked by the launcher when a widget is first added.
 */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED) // default: adding is cancelled unless user saves

        setContent {
            LifeLensIQTheme {
                ConfigContent(
                    widgetId = widgetId,
                    onSave = { dark, stats ->
                        SettingsStore.setWidgetDarkTheme(widgetId, dark)
                        stats.forEach { (key, value) -> SettingsStore.setWidgetShowStat(widgetId, key, value) }
                        val manager = AppWidgetManager.getInstance(this)
                        val ids = manager.getAppWidgetIds(
                            android.content.ComponentName(this, LifeLensIQWidgetProvider::class.java)
                        )
                        ids.forEach { id ->
                            WidgetRenderer.render(this, manager, id, R.layout.widget_lifelensiq)
                        }
                        val smallIds = manager.getAppWidgetIds(
                            android.content.ComponentName(this, LifeLensIQSmallWidgetProvider::class.java)
                        )
                        smallIds.forEach { id ->
                            WidgetRenderer.render(this, manager, id, R.layout.widget_lifelensiq_small)
                        }
                        setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun ConfigContent(widgetId: Int, onSave: (Boolean, Map<String, Boolean>) -> Unit) {
    val ctx = ServiceLocator.context()
    var dark by remember { mutableStateOf(SettingsStore.widgetDarkTheme(widgetId)) }
    var study by remember { mutableStateOf(SettingsStore.widgetShowStat(widgetId, "study")) }
    var screen by remember { mutableStateOf(SettingsStore.widgetShowStat(widgetId, "screen")) }
    var shorts by remember { mutableStateOf(SettingsStore.widgetShowStat(widgetId, "shorts")) }
    var steps by remember { mutableStateOf(SettingsStore.widgetShowStat(widgetId, "steps")) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Widget settings", style = MaterialTheme.typography.titleLarge)
        Text(
            "Choose which stats the widget shows and its theme.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dark theme", modifier = Modifier.weight(1f))
            Switch(checked = dark, onCheckedChange = { dark = it })
        }
        StatToggle("Study time", study) { study = it }
        StatToggle("Screen time", screen) { screen = it }
        StatToggle("Shorts count", shorts) { shorts = it }
        StatToggle("Steps", steps) { steps = it }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                onSave(
                    dark,
                    mapOf("study" to study, "screen" to screen, "shorts" to shorts, "steps" to steps)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save widget")
        }
    }
}

@Composable
private fun StatToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onChange)
    }
}