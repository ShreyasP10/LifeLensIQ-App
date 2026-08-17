package com.lifelensiq.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifelensiq.app.ui.theme.CategoryColors
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.max

/**
 * Canvas-drawn charts — no chart library needed.
 */

/** Grouped 7-day bars: study (green) + screen (indigo) per day. */
@Composable
fun WeeklyBarChart(
    studyMinutes: List<Long>,
    screenMinutes: List<Long>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = androidx.compose.ui.text.TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 9.sp
    )
    val barColor = MaterialTheme.colorScheme.primary
    val studyColor = CategoryColors.STUDY

    Column(modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            val chartHeight = size.height - 16.dp.toPx()
            val dayWidth = size.width / 7f
            val barWidth = dayWidth * 0.28f
            val maxValue = max(1L, studyMinutes.maxOrNull() ?: 0L).coerceAtLeast(1L)

            studyMinutes.forEachIndexed { i, minutes ->
                val ratio = (minutes.toFloat() / maxValue).coerceIn(0.03f, 1f)
                val x = i * dayWidth + dayWidth * 0.14f
                val h = chartHeight * ratio
                drawRoundRect(
                    color = studyColor,
                    topLeft = Offset(x, chartHeight - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
            screenMinutes.forEachIndexed { i, minutes ->
                val ratio = (minutes.toFloat() / maxValue).coerceIn(0.03f, 1f)
                val x = i * dayWidth + dayWidth * 0.54f
                val h = chartHeight * ratio
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, chartHeight - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
            // Day labels
            studyMinutes.indices.forEach { i ->
                val day = LocalDate.now().minusDays((6 - i).toLong())
                    .dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault())
                val layout = textMeasurer.measure(day, labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        i * dayWidth + (dayWidth - layout.size.width) / 2f,
                        chartHeight + 4.dp.toPx()
                    )
                )
            }
        }
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            LegendDot(studyColor, "Productive")
            Spacer(Modifier.width(12.dp))
            LegendDot(barColor, "Screen")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

/** Single-series bar chart (trends): values + sparse labels. */
@Composable
fun SingleBarChart(
    values: List<Long>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = androidx.compose.ui.text.TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 9.sp
    )
    val count = values.size.coerceAtLeast(1)

    Column(modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            val chartHeight = size.height - 16.dp.toPx()
            val slotWidth = size.width / count
            val barWidth = slotWidth * 0.6f
            val maxValue = max(1L, values.maxOrNull() ?: 0L).coerceAtLeast(1L)

            values.forEachIndexed { i, minutes ->
                val ratio = (minutes.toFloat() / maxValue).coerceIn(0.03f, 1f)
                val x = i * slotWidth + (slotWidth - barWidth) / 2f
                val h = chartHeight * ratio
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, chartHeight - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
            // Sparse labels so they don't overlap.
            val labelStep = (count / 7).coerceAtLeast(1)
            values.indices.forEach { i ->
                if (i % labelStep == 0 || i == values.lastIndex) {
                    val label = labels.getOrNull(i) ?: ""
                    val layout = textMeasurer.measure(label, labelStyle)
                    drawText(
                        layout,
                        topLeft = Offset(
                            (i * slotWidth + (slotWidth - layout.size.width) / 2f)
                                .coerceIn(0f, size.width - layout.size.width),
                            chartHeight + 4.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}

/** Circular progress ring with a label in the center. */
@Composable
fun ProgressRing(
    progress: Float,
    valueLabel: String,
    subLabel: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(96.dp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(valueLabel, style = MaterialTheme.typography.titleMedium)
            Text(
                subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Donut chart of category minutes. */
@Composable
fun DonutChart(
    slices: List<Pair<String, Long>>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.second }.coerceAtLeast(1L)
    val colors = slices.map { colorForCategory(it.first) }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(120.dp)) {
                val stroke = 26.dp.toPx()
                val inset = stroke / 2
                var start = -90f
                slices.forEachIndexed { i, slice ->
                    val sweep = 360f * slice.second / total
                    drawArc(
                        color = colors[i],
                        startAngle = start,
                        sweepAngle = sweep - 1.5f,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Butt),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke)
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatMinutes(total), style = MaterialTheme.typography.titleMedium)
                Text("total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            slices.forEachIndexed { i, slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(colors[i], RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${slice.first} · ${formatMinutes(slice.second)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/** Month-grid productivity calendar: days are colored by productive minutes
 *  (days run 02:00-02:00), with month navigation and a today highlight. */
@Composable
fun ProductivityCalendar(
    minutesByDay: Map<LocalDate, Long>,
    modifier: Modifier = Modifier
) {
    var monthOffset by remember { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val shown = today.withDayOfMonth(1).plusMonths(monthOffset.toLong())
    val daysInMonth = shown.lengthOfMonth()
    val leading = shown.withDayOfMonth(1).dayOfWeek.value % 7 // 0 = Sunday
    val cellSize = 40.dp
    val base = CategoryColors.STUDY

    fun level(minutes: Long): Float = when {
        minutes <= 0 -> 0.12f
        minutes < 30 -> 0.28f
        minutes < 90 -> 0.5f
        minutes < 180 -> 0.72f
        else -> 1f
    }

    fun colorOf(date: LocalDate): Color =
        base.copy(alpha = level(minutesByDay[date] ?: 0L))

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { monthOffset -= 1 }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = shown.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) +
                    " ${shown.year}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { monthOffset += 1 }, enabled = monthOffset < 0) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        val cells = mutableListOf<LocalDate?>()
        repeat(leading) { cells.add(null) }
        for (d in 1..daysInMonth) cells.add(shown.withDayOfMonth(d))
        while (cells.size % 7 != 0) cells.add(null)

        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(3.dp)
                            .let { box ->
                                if (date != null) {
                                    val m = minutesByDay[date] ?: 0L
                                    val borderColor = when {
                                        date == today -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                    box
                                        .background(colorOf(date), RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (date == today) 2.dp else 1.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                } else box
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val minutes = minutesByDay[date] ?: 0L
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (minutes >= 30) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal
                            )
                            if (minutes >= 60) {
                                Box(
                                    Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                ) {
                                    Text(
                                        "${(minutes / 60).coerceAtMost(9)}h",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            listOf(0.12f, 0.28f, 0.5f, 0.72f, 1f).forEach { a ->
                Box(Modifier.padding(1.dp).size(10.dp).background(base.copy(alpha = a), RoundedCornerShape(2.dp)))
            }
            Spacer(Modifier.width(4.dp))
            Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(
                "Productive minutes per day",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun colorForCategory(category: String): Color = when (category) {
    "Study" -> CategoryColors.STUDY
    "DSA" -> CategoryColors.DSA
    "Development" -> CategoryColors.DEVELOPMENT
    "Productivity" -> CategoryColors.PRODUCTIVITY
    "Entertainment" -> CategoryColors.ENTERTAINMENT
    "Short-form Video" -> CategoryColors.SHORT_FORM
    "Utilities" -> CategoryColors.UTILITIES
    else -> CategoryColors.OTHER
}

fun formatMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

fun dayLabel(date: LocalDate): String = when (date.dayOfWeek) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}