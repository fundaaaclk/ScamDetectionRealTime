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

data class HomeUiState(
    val isLoading: Boolean = false,
    val stats: AnalysisStats = AnalysisStats(0, 0, 0, 0, 0f),
    val recentAnalyses: List<AnalysisRecord> = emptyList(),
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState

    init { loadData() }

    fun loadData() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val stats   = ApiService.getStats()
                val recent  = ApiService.getAnalyses(limit = 5)   // Son 5 analiz
                Pair(stats, recent)
            }.onSuccess { (stats, recent) ->
                _uiState.value = HomeUiState(
                    isLoading       = false,
                    stats           = stats,
                    recentAnalyses  = recent
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
