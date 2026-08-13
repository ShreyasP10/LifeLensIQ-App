package com.lifelensiq.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.lifelensiq.app.data.local.EventEntity
import com.lifelensiq.app.util.JsonUtil
import com.lifelensiq.app.util.WebCategoryMapper
import com.lifelensiq.app.util.toPlain
import kotlinx.coroutines.tasks.await

/**
 * Firestore writer for events. Each Firestore document id == eventId, so
 * retried batches are idempotent (set() overwrites the same doc).
 *
 * Documents use a unified envelope shared with the LifeLensIQ web dashboard
 * (see LifeLensIQ-Web docs/09_Firestore_Schema.md): `id, ts, endTs,
 * durationSeconds, domain, path, title, category, eventType, device,
 * metadata, schemaVersion` plus the app's own identifiers (`eventId`,
 * `userId`, `deviceId`, `timestamp`).
 */
class FirestoreEventSource {

    private val db = FirebaseFirestore.getInstance()

    /** Upload a batch (≤500) of events. Returns the count uploaded. */
    suspend fun uploadBatch(events: List<EventEntity>): Int {
        if (events.isEmpty()) return 0
        val userId = events.first().userId
        val batch = db.batch()
        for (event in events) {
            val payload = JsonUtil.decodePayload(event.payloadJson)
                .mapValues { (_, v) -> v.toPlain() }
            val ts = event.timestamp
            val durationMs = payload["durationMs"]?.let { asLong(it) } ?: 0L
            val durationSeconds = payload["durationSeconds"]?.let { asLong(it) } ?: (durationMs / 1000)
            val endTs = ts + durationSeconds * 1000
            val pkg = payload["packageName"] as? String
            val doc = db.collection("users").document(userId)
                .collection("events").document(event.eventId)
            batch.set(doc, mapOf(
                "id" to event.eventId,
                "eventId" to event.eventId,
                "userId" to event.userId,
                "deviceId" to event.deviceId,
                "device" to "android",
                "ts" to ts,
                "endTs" to endTs,
                "durationSeconds" to durationSeconds,
                "timestamp" to ts,
                "eventType" to event.eventType,
                "category" to WebCategoryMapper.categoryFor(event.eventType, pkg),
                "domain" to WebCategoryMapper.domainFor(pkg),
                "path" to pkg,
                "title" to ((payload["appName"] as? String) ?: pkg),
                "metadata" to payload,
                "schemaVersion" to event.schemaVersion
            ))
        }
        batch.commit().await()
        return events.size
    }

    /**
     * Upload the timetable as a single doc in the web dashboard's format:
     * `users/{uid}/timetable` = { source, generatedAt, batch, entries[] }.
     */
    suspend fun uploadTimetable(userId: String, entries: List<Map<String, Any?>>, batchLabel: String?) {
        db.collection("users").document(userId)
            .collection("timetable").document("data")
            .set(mapOf(
                "source" to "LifeLens IQ Android app",
                "generatedAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "batch" to (batchLabel ?: ""),
                "entries" to entries
            ))
            .await()
    }

    /** Fetch all events from the cloud (for cloud export). Returns raw maps. */
    suspend fun fetchAllEvents(userId: String): List<Map<String, Any?>> {
        val docs = db.collection("users").document(userId)
            .collection("events")
            .orderBy("timestamp")
            .get()
            .await()
        return docs.documents.map { it.data ?: emptyMap() }
    }

    /** Delete every event + timetable doc for a user. */
    suspend fun deleteAllUserData(userId: String) {
        val events = db.collection("users").document(userId).collection("events").get().await()
        val timetable = db.collection("users").document(userId).collection("timetable").get().await()
        val batch = db.batch()
        events.documents.forEach { batch.delete(it.reference) }
        timetable.documents.forEach { batch.delete(it.reference) }
        batch.delete(db.collection("users").document(userId).collection("timetable").document("data"))
        batch.commit().await()
    }

    private fun asLong(value: Any?): Long? = when (value) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
}