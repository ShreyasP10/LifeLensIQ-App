package com.lifelensiq.app.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifelensiq.app.ui.components.formatDuration
import com.lifelensiq.app.ui.theme.CategoryColors

@Composable
fun CategoryDetailScreen(vm: CategoryDetailViewModel, category: String) {
    val state by vm.uiState.collectAsState()
    val color = CategoryColors.forCategory(category)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Total today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        formatDuration(state.totalMinutes),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    if (!state.loading) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${state.apps.size} apps · ${state.apps.sumOf { it.sessions }} sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (state.loading) {
            item { Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        if (state.apps.isEmpty() && !state.loading) {
            item {
                Text(
                    "Nothing recorded in this category today yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val max = state.apps.firstOrNull()?.minutes?.coerceAtLeast(1) ?: 1
        items(state.apps, key = { it.name + it.category }) { app ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(app.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(
                            formatDuration(app.minutes),
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { app.minutes.toFloat() / max },
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${app.sessions} session(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
