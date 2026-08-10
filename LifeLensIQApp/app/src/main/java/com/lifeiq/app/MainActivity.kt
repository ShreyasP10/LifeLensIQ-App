package com.lifeiq.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.lifeiq.app.sync.SyncScheduler
import com.lifeiq.app.tracking.LifeiqTrackerService
import com.lifeiq.app.ui.navigation.AppNavHost
import com.lifeiq.app.ui.theme.LifeIQTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* notification optional — service runs regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LifeiqTrackerService.start(this)
        SyncScheduler.schedule(this)
        SyncScheduler.enqueue(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LifeIQTheme {
                AppNavHost()
            }
        }
    }
}
