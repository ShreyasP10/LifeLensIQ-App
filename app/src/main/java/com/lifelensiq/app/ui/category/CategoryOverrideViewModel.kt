package com.lifelensiq.app.ui.category

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.util.SettingsStore
import com.lifelensiq.app.util.WebCategoryMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppCategoryItem(
    val packageName: String,
    val label: String,
    val category: String,
    val overridden: Boolean
)

data class CategoryOverrideUiState(
    val apps: List<AppCategoryItem> = emptyList(),
    val loading: Boolean = true
)

/** Lists installed launchable apps and lets the user override their category. */
class CategoryOverrideViewModel(app: Application) : AndroidViewModel(app) {

    val allCategories = listOf(
        WebCategoryMapper.STUDY,
        WebCategoryMapper.DSA,
        WebCategoryMapper.DEVELOPMENT,
        WebCategoryMapper.PRODUCTIVITY,
        WebCategoryMapper.ENTERTAINMENT,
        WebCategoryMapper.TIMEPASS,
        WebCategoryMapper.SHORT_FORM,
        WebCategoryMapper.UTILITIES,
        WebCategoryMapper.OTHER
    )

    private val _uiState = MutableStateFlow(CategoryOverrideUiState())
    val uiState: StateFlow<CategoryOverrideUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { queryApps() }
            _uiState.update { it.copy(apps = apps, loading = false) }
        }
    }

    private fun queryApps(): List<AppCategoryItem> {
        val pm = getApplication<Application>().packageManager
        val overrides = SettingsStore.categoryOverrides()
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcherIntent, 0)
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                AppCategoryItem(
                    packageName = pkg,
                    label = resolveInfo.loadLabel(pm).toString(),
                    category = overrides[pkg] ?: WebCategoryMapper.categoryForPackage(pkg),
                    overridden = overrides.containsKey(pkg)
                )
            }
    }

    fun setCategory(pkg: String, category: String) {
        SettingsStore.setCategoryOverride(pkg, if (category == "Default") "" else category)
        _uiState.update { state ->
            state.copy(
                apps = state.apps.map {
                    if (it.packageName == pkg) {
                        it.copy(
                            category = if (category == "Default") WebCategoryMapper.categoryForPackage(pkg) else category,
                            overridden = category != "Default"
                        )
                    } else it
                }
            )
        }
    }
}