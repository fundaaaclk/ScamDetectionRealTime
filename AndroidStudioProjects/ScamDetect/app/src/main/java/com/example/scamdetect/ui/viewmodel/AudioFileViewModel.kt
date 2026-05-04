package com.example.scamdetect.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scamdetect.data.model.TranscribeResult
import com.example.scamdetect.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AudioFileUiState {
    data object Idle    : AudioFileUiState()
    data object Loading : AudioFileUiState()
    data class  Success(val result: TranscribeResult) : AudioFileUiState()
    data class  Error(val message: String)            : AudioFileUiState()
}

class AudioFileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AudioFileUiState>(AudioFileUiState.Idle)
    val uiState: StateFlow<AudioFileUiState> = _uiState

    fun analyzeFile(uri: Uri, context: Context) {
        _uiState.value = AudioFileUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { ApiService.transcribeFile(uri, context) }
                .onSuccess { _uiState.value = AudioFileUiState.Success(it) }
                .onFailure { _uiState.value = AudioFileUiState.Error(it.message ?: "Bilinmeyen hata") }
        }
    }

    fun reset() { _uiState.value = AudioFileUiState.Idle }
}
