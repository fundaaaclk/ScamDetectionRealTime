package com.example.scamdetect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scamdetect.data.model.AnalysisRecord
import com.example.scamdetect.data.model.AnalysisStats
import com.example.scamdetect.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReportsUiState(
    val isLoading: Boolean = false,
    val stats: AnalysisStats = AnalysisStats(0, 0, 0, 0, 0f),
    val analyses: List<AnalysisRecord> = emptyList(),
    val error: String? = null
)

class ReportsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState(isLoading = true))
    val uiState: StateFlow<ReportsUiState> = _uiState

    init { loadData() }

    fun loadData() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val stats    = ApiService.getStats()
                val analyses = ApiService.getAnalyses(limit = 50)
                Pair(stats, analyses)
            }.onSuccess { (stats, analyses) ->
                _uiState.value = ReportsUiState(
                    isLoading = false,
                    stats     = stats,
                    analyses  = analyses
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
