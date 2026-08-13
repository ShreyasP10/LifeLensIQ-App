package com.lifelensiq.app.export

import android.content.Context
import android.net.Uri
import com.lifelensiq.app.data.local.AppDatabase
import com.lifelensiq.app.domain.model.ExportFilter
import com.lifelensiq.app.domain.repository.AuthRepository
import com.lifelensiq.app.domain.repository.TimetableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStream

/**
 * Orchestrates an export: reads events (Room by default; Firestore option
 * reserved for Phase 2), streams them through the requested exporter into
 * a SAF Uri.
 */
class ExportUseCase(
    private val db: AppDatabase,
    private val auth: AuthRepository,
    private val timetableRepo: TimetableRepository
) {

    suspend fun export(
        context: Context,
        uri: Uri,
        format: ExportFormat,
        filter: ExportFilter
    ): ExportOutcome = withContext(Dispatchers.IO) {
        try {
            val from = filter.from ?: 0L
            val to = filter.to ?: Long.MAX_VALUE
            val events = db.eventDao().getBetween(from, to)
                .let { list -> filter.eventTypes?.let { types -> list.filter { it.eventType in types } } ?: list }
            val allSlots = timetableRepo.observeAll().first()
            val timetable = allSlots.map { slot -> slot.toExportMap() }

            val exporter: Exporter = when (format) {
                ExportFormat.CSV -> CsvExporter()
                ExportFormat.JSON -> JsonExporter(ndjson = false)
                ExportFormat.NDJSON -> JsonExporter(ndjson = true)
            }
            val out = context.contentResolver.openOutputStream(uri)
                ?: return@withContext ExportOutcome(error = "Could not open output file")
            out.use { exporter.write(events, timetable, it) }
            ExportOutcome(count = events.size)
        } catch (t: Throwable) {
            ExportOutcome(error = t.message ?: "Export failed")
        }
    }

    data class ExportOutcome(
        val count: Int = 0,
        val error: String? = null
    )
}

private fun com.lifelensiq.app.domain.model.TimetableSlot.toExportMap(): Map<String, Any?> = mapOf(
    "day" to day,
    "slotNo" to slotNo,
    "start" to start,
    "end" to end,
    "subject" to subject,
    "subjectFull" to subjectFull,
    "room" to room,
    "faculty" to faculty,
    "type" to type.id,
    "applicable" to applicable
)
