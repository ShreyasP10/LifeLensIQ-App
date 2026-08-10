package com.lifeiq.app.export

import com.lifeiq.app.data.local.EventEntity
import java.io.OutputStream

/** Writes exported rows to an OutputStream. Implementations must be JVM-testable. */
interface Exporter {
    fun write(events: List<EventEntity>, timetable: List<Map<String, Any?>>, out: OutputStream)
}

enum class ExportFormat(val label: String, val mimeType: String) {
    CSV("CSV", "text/csv"),
    JSON("JSON", "application/json"),
    NDJSON("NDJSON", "application/x-ndjson")
}
