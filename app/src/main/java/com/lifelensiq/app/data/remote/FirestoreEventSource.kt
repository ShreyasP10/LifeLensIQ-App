package com.lifelensiq.app.data.remote

import com.google.firebase.firestore.FieldPath
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

    /**
     * Upload a batch (≤500) of events under the CURRENT user's subtree.
     * The collection path uses `userId` (the logged-in uid at sync time) —
     * events captured before login carry userId="anonymous" and must still
     * land under the real account, otherwise the security rules reject them.
     */
    suspend fun uploadBatch(userId: String, events: List<EventEntity>): Int {
        if (events.isEmpty()) return 0
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
            ).let { JsonUtil.forFirestore(it) })
        }
        batch.commit().await()
        return events.size
    }

    /**
     * Fetch every event doc for a user (chunked by document id), including
     * events written by the web dashboard. Returns raw cloud maps.
     */
    suspend fun fetchAllEvents(userId: String): List<Map<String, Any?>> {
        val out = mutableListOf<Map<String, Any?>>()
        var lastId: String? = null
        while (true) {
            var query = db.collection("users").document(userId)
                .collection("events")
                .orderBy(FieldPath.documentId())
                .limit(FETCH_CHUNK_SIZE)
            if (lastId != null) query = query.startAfter(lastId)
            val docs = query.get().await()
            if (docs.documents.isEmpty()) break
            docs.documents.forEach { out.add(it.data ?: emptyMap()) }
            lastId = docs.documents.last().id
            if (docs.documents.size < FETCH_CHUNK_SIZE) break
        }
        return out
    }

    /**
     * Delete every event doc for a user. Deletes in chunks because
     * Firestore batches are capped at 500 writes per commit.
     */
    suspend fun deleteAllUserData(userId: String) {
        val eventsRef = db.collection("users").document(userId).collection("events")
        while (true) {
            val docs = eventsRef.orderBy(FieldPath.documentId())
                .limit(DELETE_CHUNK_SIZE).get().await()
            if (docs.documents.isEmpty()) break
            val batch = db.batch()
            docs.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (docs.documents.size < DELETE_CHUNK_SIZE) break
        }
    }

    private fun asLong(value: Any?): Long? = when (value) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

    companion object {
        private const val DELETE_CHUNK_SIZE = 400L
        private const val FETCH_CHUNK_SIZE = 500L
    }
}