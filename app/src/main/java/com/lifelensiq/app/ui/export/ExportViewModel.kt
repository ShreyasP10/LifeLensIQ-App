package com.lifelensiq.app.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.export.ExportFormat
import com.lifelensiq.app.export.ExportUseCase
import com.lifelensiq.app.domain.model.ExportFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExportUiState(
    val format: ExportFormat = ExportFormat.CSV,
    val isExporting: Boolean = false,
    val lastMessage: String? = null
)

class ExportViewModel(private val useCase: ExportUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun setFormat(format: ExportFormat) {
        _uiState.update { it.copy(format = format) }
    }

    /** Called after the user picks a destination via SAF CreateDocument. */
    fun exportTo(androidUri: String) {
        val state = _uiState.value
        if (state.isExporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, lastMessage = null) }
            val outcome = useCase.export(
                context = com.lifelensiq.app.di.ServiceLocator.context(),
                uri = android.net.Uri.parse(androidUri),
                format = state.format,
                filter = ExportFilter()
            )
            _uiState.update {
                it.copy(
                    isExporting = false,
                    lastMessage = outcome.error ?: "Exported ${outcome.count} events successfully."
                )
            }
        }
    }
}
