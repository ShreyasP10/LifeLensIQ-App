package com.lifelensiq.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lifelensiq.app.notifications.InsightScheduler
import com.lifelensiq.app.sync.SyncScheduler
import com.lifelensiq.app.tracking.LifeLensIQTrackerService
import com.lifelensiq.app.ui.components.CrashReportScreen
import com.lifelensiq.app.ui.navigation.AppNavHost
import com.lifelensiq.app.ui.theme.LifeLensIQTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* notification optional — service runs regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen must be installed before super.onCreate()
        try {
            installSplashScreen()
        } catch (e: Exception) {
            // Splash screen not available, continue without it
        }
        super.onCreate(savedInstanceState)

        // Start tracker service
        try {
            LifeLensIQTrackerService.start(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start tracker service", e)
        }

        try {
            SyncScheduler.schedule(this)
            SyncScheduler.enqueue(this)
            InsightScheduler.schedule(this)
            com.lifelensiq.app.widget.LifeLensIQWidgetProvider.refresh(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Scheduler/widget init failed", e)
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LifeLensIQTheme {
                val app = application as LifeLensIQApp
                val crashLog = remember { LifeLensIQApp.readCrashLog(app) }
                if (crashLog != null) {
                    CrashReportScreen(logText = crashLog, onDismiss = {
                        LifeLensIQApp.clearCrashLog(app)
                    })
                } else {
                    AppNavHost(initialRoute = intent.getStringExtra("route"))
                }
            }
        }

        // Request battery optimization exemption after UI is ready
        requestIgnoreBatteryOptimizations()
    }

    private fun requestIgnoreBatteryOptimizations() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = getSystemService(android.os.PowerManager::class.java)
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Battery optimization request failed", e)
        }
    }
}
