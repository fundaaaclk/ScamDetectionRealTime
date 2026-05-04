package com.example.scamdetect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scamdetect.data.model.AnalysisRecord
import com.example.scamdetect.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReportDetailUiState(
    val isLoading: Boolean = false,
    val record: AnalysisRecord? = null,
    val error: String? = null
)

class ReportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReportDetailUiState())
    val uiState: StateFlow<ReportDetailUiState> = _uiState

    fun loadReport(analysisId: String) {
        if (analysisId.isBlank()) return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ApiService.getAnalysisById(analysisId)
            }.onSuccess { record ->
                _uiState.value = ReportDetailUiState(
                    isLoading = false,
                    record    = record
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error     = err.message
                )
            }
        }
    }
}
