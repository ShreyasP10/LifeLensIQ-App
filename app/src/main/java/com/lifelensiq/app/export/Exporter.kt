package com.lifelensiq.app.export

import com.lifelensiq.app.data.local.EventEntity
import java.io.OutputStream

/** Writes exported rows to an OutputStream. Implementations must be JVM-testable. */
interface Exporter {
    fun write(events: List<EventEntity>, out: OutputStream)
}

enum class ExportFormat(val label: String, val mimeType: String) {
    CSV("CSV", "text/csv"),
    JSON("JSON", "application/json"),
    NDJSON("NDJSON", "application/x-ndjson")
}