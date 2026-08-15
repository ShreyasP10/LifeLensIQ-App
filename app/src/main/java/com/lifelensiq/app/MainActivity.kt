package com.lifelensiq.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

        setContent {
            LifeLensIQTheme {
                AppNavHost(initialRoute = intent.getStringExtra("route"))
            }
        }
    }
}
