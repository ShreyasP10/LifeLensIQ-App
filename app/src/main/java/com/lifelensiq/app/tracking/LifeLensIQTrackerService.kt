package com.lifelensiq.app.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.lifelensiq.app.MainActivity
import com.lifelensiq.app.R
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.domain.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that drives all passive tracking:
 * app usage poller, screen/charge receivers, step counter.
 */
class LifeLensIQTrackerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appPollJob: Job? = null
    private var stepTracker: StepTracker? = null

    private val screenReceiver: ScreenStateReceiver by lazy { ScreenStateReceiver(scope) }
    private val chargeReceiver: ChargeReceiver by lazy { ChargeReceiver(scope) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        startTrackingIfNeeded()
        // Always re-check the step counter (e.g. permission granted while the
        // service was already running) — no-op when it is already running.
        startStepTracker(ServiceLocator.eventEmitter())
        return START_STICKY
    }

    private fun startTrackingIfNeeded() {
        if (appPollJob != null) return
        val emitter = ServiceLocator.eventEmitter()

        appPollJob = scope.launch {
            AppUsagePoller(this@LifeLensIQTrackerService, emitter).pollLoop()
        }

        ServiceLocator.wakeDetector()?.let { wake ->
            screenReceiver.onScreenOn = { emitter2 -> wake.onScreenOn(emitter2) }
            screenReceiver.onScreenOff = { wake.onScreenOff() }
        }

        registerReceiver(screenReceiver, screenReceiver.intentFilter())
        registerReceiver(chargeReceiver, chargeReceiver.intentFilter())

        scope.launch { emitter.emit(EventType.TRACKING_STATE.id, mapOf("state" to "STARTED")) }
    }

    /**
     * (Re)starts the step counter. Called on every start command so that
     * granting Activity Recognition while the service is already running
     * takes effect immediately.
     */
    private fun startStepTracker(emitter: EventEmitter) {
        if (stepTracker == null) stepTracker = runCatching { StepTracker(this) }.getOrNull()
        val tracker = stepTracker ?: return
        if (tracker.isRunning) return
        if (!tracker.hasPermission(this)) return
        tracker.start(scope, emitter)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LifeLens IQ tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        appPollJob?.cancel()
        runCatching { unregisterReceiver(screenReceiver) }
        runCatching { unregisterReceiver(chargeReceiver) }
        stepTracker?.stop()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "lifelensiq_tracking"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, LifeLensIQTrackerService::class.java)
            context.startForegroundService(intent)
        }
    }
}
