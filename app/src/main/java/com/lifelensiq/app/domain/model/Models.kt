package com.lifelensiq.app.domain.model

data class ExportFilter(
    val from: Long? = null,
    val to: Long? = null,
    val eventTypes: Set<String>? = null
)

data class SyncResult(
    val uploaded: Int,
    val failed: Boolean = false,
    val error: String? = null
)