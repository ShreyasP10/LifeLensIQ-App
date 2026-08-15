package com.lifelensiq.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.domain.EventType
import java.util.concurrent.TimeUnit

/**
 * Uploads unsynced events to Firestore in batches of 500.
 * Idempotent: Firestore doc id == eventId.
 */
class SyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = ServiceLocator.eventRepository()
        repo.prune(EVENTS_RETENTION_MS)
        var syncedInThisRun = 0
        var uploaded = 0

        while (true) {
            val batch = repo.getUnsynced(MAX_BATCH)
            if (batch.isEmpty()) break
            val result = repo.syncBatch(batch)
            if (result.failed) {
                return if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure(workDataOf("error" to (result.error ?: "sync failed")))
                }
            }
            uploaded += result.uploaded
            syncedInThisRun += batch.size
            if (batch.size < MAX_BATCH) break
        }

        // Pull events written by the web dashboard (same users/{uid}/events
        // collection) so both app and website data appear together.
        val downloaded = repo.downloadCloud()

        if (uploaded > 0 || downloaded > 0) {
            ServiceLocator.eventEmitter().emit(
                EventType.SYNC_STATUS.id,
                mapOf("batchUploaded" to uploaded, "downloaded" to downloaded)
            )
        }
        com.lifelensiq.app.widget.LifeLensIQWidgetProvider.refresh(applicationContext)
        return Result.success()
    }

    companion object {
        const val MAX_BATCH = 500
        const val WORK_NAME = "lifelensiq_sync"
        const val EVENTS_RETENTION_MS = 90L * 24 * 60 * 60 * 1000
    }
}

object SyncScheduler {

    /** Periodic sync every 15 minutes when network is available. */
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(SyncWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** One-time sync now (used on app open and from Settings). */
    fun enqueue(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SyncWorker.WORK_NAME,
                // APPEND (not REPLACE) — REPLACE would cancel the periodic
                // chain created by schedule() and kill background sync.
                ExistingWorkPolicy.APPEND,
                request
            )
    }
}
