package com.lifelensiq.app.ui.settings

import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.domain.repository.AuthRepository
import com.lifelensiq.app.domain.repository.EventRepository
import com.lifelensiq.app.domain.repository.TimetableRepository
import com.lifelensiq.app.sync.SyncScheduler
import com.lifelensiq.app.timetable.TimetableImporter
import com.lifelensiq.app.tracking.ClassReminderWorker
import com.lifelensiq.app.tracking.LifeLensIQTrackerService
import com.lifelensiq.app.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SettingsUiState(
    val usageAccessGranted: Boolean = false,
    val stepsPermissionGranted: Boolean = false,
    val message: String? = null,
    val busy: Boolean = false
)

class SettingsViewModel(
    private val auth: AuthRepository,
    private val events: EventRepository,
    private val timetable: TimetableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshUsageAccess()
        refreshStepsPermission()
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

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, message = "Syncing…") }
            SyncScheduler.enqueue(ServiceLocator.context())
            _uiState.update { it.copy(busy = false, message = "Sync enqueued.") }
        }
    }

    /** Import timetable from a JSON file picked via SAF. */
    fun importTimetable(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val raw = withContext(Dispatchers.IO) {
                ServiceLocator.context().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            } ?: return@launch
            val result = TimetableImporter().parse(raw)
            if (result.errors.isNotEmpty()) {
                _uiState.update { it.copy(busy = false, message = "Import issues: ${result.errors.take(3).joinToString("; ")}") }
            }
            if (result.slots.isNotEmpty()) {
                timetable.saveAll(result.slots, result.batch)
                ClassReminderWorker.schedule(ServiceLocator.context())
                _uiState.update { it.copy(busy = false, message = "Imported: ${result.summary}") }
            } else {
                _uiState.update { it.copy(busy = false, message = "Nothing imported. ${result.summary}") }
            }
        }
    }

    /** Import from the bundled sample asset (assets/timetable_personalized.json). */
    fun importBundledSample() {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val raw = runCatching {
                ServiceLocator.context().assets.open("timetable_personalized.json")
                    .bufferedReader().use { it.readText() }
            }.getOrElse { t ->
                _uiState.update { it.copy(busy = false, message = "Sample not found: ${t.message}") }
                return@launch
            }
            val result = TimetableImporter().parse(raw)
            if (result.slots.isNotEmpty()) {
                timetable.saveAll(result.slots, result.batch)
                ClassReminderWorker.schedule(ServiceLocator.context())
                _uiState.update { it.copy(busy = false, message = "Imported sample: ${result.summary}") }
            }
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
}
