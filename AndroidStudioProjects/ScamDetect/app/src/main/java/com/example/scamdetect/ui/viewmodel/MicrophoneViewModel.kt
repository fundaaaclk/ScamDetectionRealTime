package com.example.scamdetect.ui.viewmodel

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scamdetect.data.model.ChunkResult
import com.example.scamdetect.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

private const val SAMPLE_RATE    = 16000
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT   = AudioFormat.ENCODING_PCM_16BIT
private const val CHUNK_SECONDS  = 3          // Her kaç saniyede chunk gönderilsin

data class MicrophoneUiState(
    val isRecording: Boolean      = false,
    val elapsedSeconds: Int       = 0,
    val chunks: List<ChunkResult> = emptyList(),
    val overallRiskPercent: Int   = 0,
    val finalLabel: String        = "safe",
    val alarm: Boolean            = false,
    val trend: String             = "insufficient_data",
    val topSuggestion: String     = "",
    val errorMessage: String?     = null,
    val analysisId: String?       = null
)

class MicrophoneViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MicrophoneUiState())
    val uiState: StateFlow<MicrophoneUiState> = _uiState

    private var recordingJob: Job? = null
    private var timerJob: Job?     = null
    private var audioRecord: AudioRecord? = null
    private var sessionId: String = UUID.randomUUID().toString()

    // ── Kayıt başlat ──────────────────────────────────────────────────────────

    fun startRecording(context: android.content.Context) {
        if (_uiState.value.isRecording) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            _uiState.value = _uiState.value.copy(errorMessage = "Mikrofon izni gerekli")
            return
        }

        val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(SAMPLE_RATE * 2)   // en az 1 saniyelik buffer

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            minBufSize
        )

        sessionId = UUID.randomUUID().toString()
        _uiState.value = MicrophoneUiState(isRecording = true)

        // Zamanlayıcı
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            var elapsed = 0
            while (isActive) {
                kotlinx.coroutines.delay(1000)
                elapsed++
                _uiState.value = _uiState.value.copy(elapsedSeconds = elapsed)
            }
        }

        // Kayıt + chunk döngüsü
        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            audioRecord?.startRecording()
            var chunkIndex = 0
            val samplesPerChunk = SAMPLE_RATE * CHUNK_SECONDS
            val buffer = ShortArray(samplesPerChunk)

            while (isActive) {
                var totalRead = 0
                // chunk doldur
                while (totalRead < samplesPerChunk && isActive) {
                    val read = audioRecord?.read(buffer, totalRead, samplesPerChunk - totalRead) ?: 0
                    if (read > 0) totalRead += read
                }
                if (!isActive) break

                // Short[] → ByteArray (little-endian PCM 16-bit)
                val pcmBytes = ByteArray(totalRead * 2)
                for (i in 0 until totalRead) {
                    val sample = buffer[i]
                    pcmBytes[i * 2]     = (sample.toInt() and 0xFF).toByte()
                    pcmBytes[i * 2 + 1] = (sample.toInt() shr 8 and 0xFF).toByte()
                }

                // API çağrısını ayrı coroutine'e at — kayıt hiç durmasın
                val currentIndex = chunkIndex++
                val chunkCopy = pcmBytes.copyOf()
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        ApiService.transcribeChunk(chunkCopy, SAMPLE_RATE, currentIndex, sessionId)
                    }.onSuccess { chunk ->
                        val newChunks = if (chunk.transcript.isNotBlank())
                            _uiState.value.chunks + chunk
                        else
                            _uiState.value.chunks
                        val riskiest = newChunks.maxByOrNull { it.finalScore }
                        val bestSuggestion = newChunks
                            .filter { it.suggestion.isNotBlank() }
                            .maxByOrNull { it.finalScore }
                            ?.suggestion ?: ""
                        _uiState.value = _uiState.value.copy(
                            chunks = newChunks,
                            overallRiskPercent = chunk.finalScore,
                            finalLabel = chunk.finalLabel,
                            alarm = chunk.alarm,
                            trend = chunk.trend,
                            topSuggestion = bestSuggestion
                        )
                    }.onFailure { err ->
                        _uiState.value = _uiState.value.copy(errorMessage = err.message)
                    }
                }
            }
        }
    }

    // ── Kayıt durdur ─────────────────────────────────────────────────────────

    fun stopRecording() {
        timerJob?.cancel()
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val currentChunks = _uiState.value.chunks
        val duration = _uiState.value.elapsedSeconds.toFloat()

        if (currentChunks.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    ApiService.endMicrophoneSession(currentChunks, duration)
                }.onSuccess { analysisId ->
                    _uiState.value = _uiState.value.copy(
                        isRecording = false,
                        analysisId = analysisId
                    )
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isRecording = false,
                        errorMessage = err.message
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(isRecording = false)
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
    }
}
