package com.lifelensiq.app.ui.settings

import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.domain.repository.AuthRepository
import com.lifelensiq.app.domain.repository.EventRepository
import com.lifelensiq.app.sync.SyncScheduler
import com.lifelensiq.app.tracking.LifeLensIQTrackerService
import com.lifelensiq.app.tracking.ShortsReelsDetector
import com.lifelensiq.app.util.PermissionUtils
import com.lifelensiq.app.util.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val usageAccessGranted: Boolean = false,
    val stepsPermissionGranted: Boolean = false,
    val shortsDetectorEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val message: String? = null,
    val busy: Boolean = false,
    // Goals & notifications
    val studyGoalMin: Int = SettingsStore.studyGoalMin,
    val screenLimitMin: Int = SettingsStore.screenLimitMin,
    val shortsAlertViews: Int = SettingsStore.shortsAlertViews,
    val dailySummaryEnabled: Boolean = SettingsStore.dailySummaryEnabled,
    val screenLimitAlertEnabled: Boolean = SettingsStore.screenLimitAlertEnabled,
    val shortsNudgeEnabled: Boolean = SettingsStore.shortsNudgeEnabled,
    val bedtimeReminderEnabled: Boolean = SettingsStore.bedtimeReminderEnabled,
    val morningReportEnabled: Boolean = SettingsStore.morningReportEnabled
)

class SettingsViewModel(
    private val auth: AuthRepository,
    private val events: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        refreshUsageAccess()
        refreshStepsPermission()
        refreshShortsDetector()
        refreshNotifications()
    }

    fun refreshNotifications() {
        val enabled = androidx.core.app.NotificationManagerCompat
            .from(ServiceLocator.context()).areNotificationsEnabled()
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun revokeNotifications(context: Context) {
        context.revokeSelfPermissionOnKill(android.Manifest.permission.POST_NOTIFICATIONS)
        refreshNotifications()
        _uiState.update { it.copy(message = "Notification permission revoked. Grant it again anytime from this screen.") }
    }

    fun revokeStepsPermission(context: Context) {
        context.revokeSelfPermissionOnKill(android.Manifest.permission.ACTIVITY_RECOGNITION)
        refreshStepsPermission()
        _uiState.update { it.copy(message = "Activity-recognition (steps) permission revoked.") }
    }

    fun refreshUsageAccess() {
        _uiState.update { it.copy(usageAccessGranted = PermissionUtils.isUsageAccessGranted(ServiceLocator.context())) }
    }

    fun refreshStepsPermission() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            ServiceLocator.context(), android.Manifest.permission.ACTIVITY_RECOGNITION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(stepsPermissionGranted = granted) }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            _uiState.update { it.copy(message = "Usage access settings not available on this device.") }
        }
    }

    fun refreshShortsDetector() {
        val context = ServiceLocator.context()
        val component = android.content.ComponentName(context, ShortsReelsDetector::class.java)
        val enabledServices = runCatching {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
        }.getOrDefault("")
        _uiState.update { it.copy(shortsDetectorEnabled = enabledServices.contains(component.flattenToString())) }
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            _uiState.update { it.copy(message = "Accessibility settings not available on this device.") }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, message = "Syncing…") }
            SyncScheduler.enqueue(ServiceLocator.context())
            _uiState.update { it.copy(busy = false, message = "Sync enqueued — uploads and downloads your cloud data.") }
        }
    }

    fun deleteLocalData() {
        viewModelScope.launch {
            events.deleteAllLocal()
            _uiState.update { it.copy(message = "Local data deleted.") }
        }
    }

    fun deleteCloudData() {
        viewModelScope.launch {
            events.deleteAllCloud()
            _uiState.update { it.copy(message = "Cloud data deleted.") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.logout()
        }
    }

    fun restartTracking() {
        LifeLensIQTrackerService.start(ServiceLocator.context())
        _uiState.update { it.copy(message = "Tracking restarted.") }
    }

    // ---- Goals & notification settings ----
    fun updateGoals(studyGoal: Int, screenLimit: Int, shortsViews: Int) {
        SettingsStore.studyGoalMin = studyGoal
        SettingsStore.screenLimitMin = screenLimit
        SettingsStore.shortsAlertViews = shortsViews
        _uiState.update {
            it.copy(
                studyGoalMin = studyGoal,
                screenLimitMin = screenLimit,
                shortsAlertViews = shortsViews
            )
        }
    }

    fun setDailySummary(enabled: Boolean) {
        SettingsStore.dailySummaryEnabled = enabled
        _uiState.update { it.copy(dailySummaryEnabled = enabled) }
    }

    fun setScreenLimitAlert(enabled: Boolean) {
        SettingsStore.screenLimitAlertEnabled = enabled
        _uiState.update { it.copy(screenLimitAlertEnabled = enabled) }
    }

    fun setShortsNudge(enabled: Boolean) {
        SettingsStore.shortsNudgeEnabled = enabled
        _uiState.update { it.copy(shortsNudgeEnabled = enabled) }
    }

    fun setBedtimeReminder(enabled: Boolean) {
        SettingsStore.bedtimeReminderEnabled = enabled
        _uiState.update { it.copy(bedtimeReminderEnabled = enabled) }
    }

    fun setMorningReport(enabled: Boolean) {
        SettingsStore.morningReportEnabled = enabled
        _uiState.update { it.copy(morningReportEnabled = enabled) }
    }
}