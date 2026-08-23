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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lifelensiq.app.notifications.InsightScheduler
import com.lifelensiq.app.sync.SyncScheduler
import com.lifelensiq.app.tracking.LifeLensIQTrackerService
import com.lifelensiq.app.ui.navigation.AppNavHost
import com.lifelensiq.app.ui.theme.LifeLensIQTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* notification optional — service runs regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        LifeLensIQTrackerService.start(this)
        SyncScheduler.schedule(this)
        SyncScheduler.enqueue(this)
        InsightScheduler.schedule(this)
        com.lifelensiq.app.widget.LifeLensIQWidgetProvider.refresh(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Request battery optimization exemption for reliable background tracking
        requestIgnoreBatteryOptimizations()

        setContent {
            LifeLensIQTheme {
                AppNavHost(initialRoute = intent.getStringExtra("route"))
            }
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(android.os.PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
