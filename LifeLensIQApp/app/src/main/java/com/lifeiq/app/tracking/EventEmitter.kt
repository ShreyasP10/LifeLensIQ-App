package com.lifeiq.app.tracking

/** Emits events into the local buffer (see EventRepository.emit). */
interface EventEmitter {
    suspend fun emit(eventType: String, payload: Map<String, Any?>): String
}
