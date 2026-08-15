package com.lifelensiq.app.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifelensiq.app.notifications.InsightScheduler
import com.lifelensiq.app.sync.SyncScheduler

/** Restarts tracking and sync after device reboot / app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                LifeLensIQTrackerService.start(context)
                SyncScheduler.enqueue(context)
                InsightScheduler.schedule(context)
            }
        }
    }
}
