package com.lifeiq.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.lifeiq.app.data.local.EventEntity
import com.lifeiq.app.util.JsonUtil
import com.lifeiq.app.util.toPlain
import kotlinx.coroutines.tasks.await

/**
 * Firestore writer for events. Each Firestore document id == eventId, so
 * retried batches are idempotent (set() overwrites the same doc).
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
            val doc = db.collection("users").document(userId)
                .collection("events").document(event.eventId)
            batch.set(doc, mapOf(
                "eventId" to event.eventId,
                "userId" to event.userId,
                "deviceId" to event.deviceId,
                "eventType" to event.eventType,
                "schemaVersion" to event.schemaVersion,
                "timestamp" to event.timestamp,
                "payload" to payload
            ))
        }
        batch.commit().await()
        return events.size
    }

    /** Upload timetable day documents (upsert by day name). */
    suspend fun uploadTimetable(userId: String, day: String, slots: List<Map<String, Any?>>) {
        db.collection("users").document(userId)
            .collection("timetable").document(day)
            .set(mapOf("day" to day, "slots" to slots, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
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
        batch.commit().await()
    }
}
