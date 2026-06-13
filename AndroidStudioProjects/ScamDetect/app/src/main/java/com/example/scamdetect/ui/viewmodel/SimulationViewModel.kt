package com.example.scamdetect.ui.viewmodel

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scamdetect.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume

data class ScenarioLine(val speaker: String, val text: String, val delayMs: Long)

data class ScenarioData(
    val id: Int,
    val title: String,
    val category: String,
    val preview: String,
    val lines: List<ScenarioLine>
)

data class SimLine(
    val speaker: String,
    val text: String,
    val isScam: Boolean = false,
    val scamScore: Int = 0,
    val scamType: String = "",
    val suggestion: String = ""
)

data class SimulationUiState(
    val scenarioTitle: String = "",
    val visibleLines: List<SimLine> = emptyList(),
    val currentRiskPercent: Int = 0,
    val riskLevel: String = "safe",
    val isRecordingUser: Boolean = false,
    val showAlert: Boolean = false,
    val alertSuggestion: String = "",
    val alertIsDanger: Boolean = false,
    val isFinished: Boolean = false,
    val elapsedSeconds: Int = 0
)

class SimulationViewModel : ViewModel() {

    companion object {
        val SCENARIOS = listOf(
            ScenarioData(
                id = 1,
                title = "Banka Kartı Donduruldu",
                category = "banka_kart_hesap",
                preview = "Yapı Kredi güvenlik birimi arıyor — hesabınızda şüpheli işlem var.",
                lines = listOf(
                    ScenarioLine("caller", "Merhaba, Yapı Kredi Bankası güvenlik biriminden arıyorum.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Hesabınızda şüpheli bir işlem tespit ettik ve kartınızı geçici olarak dondurduk.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Hesabınızı yeniden aktif etmek için kimliğinizi doğrulamamız gerekiyor.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Lütfen kart şifrenizi ve son 3 hane CVV numaranızı bana söyler misiniz?", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "İşlemi hızlıca tamamlamazsak hesabınız 24 saat bloke kalacak.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Ayrıca size gelen SMS doğrulama kodunu da paylaşmanız gerekiyor.", 0L),
                )
            ),
            ScenarioData(
                id = 2,
                title = "Kargo Gümrük Ödemesi",
                category = "kargo_gumruk",
                preview = "Uluslararası kargonuz gümrükte bekliyor, ödeme yapılması gerekiyor.",
                lines = listOf(
                    ScenarioLine("caller", "İyi günler, PTT kargo müşteri hizmetlerinden arıyorum.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Yurt dışından size gelen bir paketiniz gümrükte bekliyor.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Paketi serbest bırakmak için 187 TL gümrük vergisi ödemeniz gerekiyor.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Ödemeyi şu an telefonda kart bilgilerinizle halledebiliriz.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Bugün ödeme yapmazsanız paket iade edilecek ve tekrar gönderim ücreti ödersiniz.", 0L),
                )
            ),
            ScenarioData(
                id = 3,
                title = "Kripto Yatırım Teklifi",
                category = "finans_kripto",
                preview = "Günde %30 garantili kripto kazancı için acil fırsat sunuluyor.",
                lines = listOf(
                    ScenarioLine("caller", "Merhaba, sizi bir yatırım fırsatı için arıyorum.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Platformumuz yapay zeka ile kripto ticareti yapıyor ve günlük %30 getiri garantisi sunuyoruz.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Şu an sadece 12 kontenjanımız kaldı, bugün kaydolursanız ilk ay komisyon almıyoruz.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Minimum 500 dolar ile başlayabilirsiniz, USDT veya banka transferi kabul ediyoruz.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Hemen IBAN'ınızı veya kripto cüzdan adresinizi verir misiniz?", 0L),
                )
            ),
            ScenarioData(
                id = 4,
                title = "SGK Prim Borç Uyarısı",
                category = "resmi_kurum",
                preview = "SGK müdürlüğünden aranıyorsunuz — prim borcu nedeniyle dosyanız kapanacak.",
                lines = listOf(
                    ScenarioLine("caller", "İyi günler, Sosyal Güvenlik Kurumu İstanbul müdürlüğünden arıyorum.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Kayıtlarımıza göre 2023 yılına ait 3.840 TL prim borcunuz bulunmaktadır.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Bu borç bugün ödenmediği takdirde dosyanız kapatılacak ve sağlık güvenceniz askıya alınacak.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "Ödemeyi şimdi kredi kartıyla telefonda yapabilirsiniz, size özel indirimli ödeme planı sunabiliriz.", 0L),
                    ScenarioLine("user", "", 0L),
                    ScenarioLine("caller", "TC kimlik numaranızı ve kart bilgilerinizi alabilir miyim?", 0L),
                )
            )
        )
    }

    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var playbackJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var alertShownAt = 0
    private var sessionId: String = UUID.randomUUID().toString()

    private suspend fun recordAndTranscribeUser(context: Context, durationMs: Long) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED) return

        val sampleRate = 16000
        val totalSamples = (sampleRate * durationMs / 1000).toInt()
        val minBuf = android.media.AudioRecord.getMinBufferSize(
            sampleRate,
            android.media.AudioFormat.CHANNEL_IN_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate)

        val ar = android.media.AudioRecord(
            android.media.MediaRecorder.AudioSource.MIC,
            sampleRate,
            android.media.AudioFormat.CHANNEL_IN_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT,
            minBuf
        )
        ar.startRecording()
        _uiState.update { it.copy(isRecordingUser = true) }

        val buffer = ShortArray(totalSamples)
        var read = 0
        while (read < totalSamples) {
            val r = ar.read(buffer, read, totalSamples - read)
            if (r <= 0) break
            read += r
        }
        ar.stop(); ar.release()
        _uiState.update { it.copy(isRecordingUser = false) }

        val bytes = ByteArray(read * 2)
        for (i in 0 until read) {
            bytes[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (buffer[i].toInt() shr 8 and 0xFF).toByte()
        }

        runCatching { ApiService.transcribeChunk(bytes, sampleRate, 0, sessionId) }
            .onSuccess { result ->
                if (result.transcript.isNotBlank()) {
                    _uiState.update { state ->
                        state.copy(visibleLines = state.visibleLines + SimLine("user", result.transcript))
                    }
                }
            }
    }

    private suspend fun playAsset(context: Context, assetPath: String) {
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { cont ->
                try {
                    val afd = context.assets.openFd(assetPath)
                    val mp = MediaPlayer().apply {
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        prepare()
                    }
                    mediaPlayer = mp
                    mp.setOnCompletionListener {
                        it.release()
                        mediaPlayer = null
                        if (cont.isActive) cont.resume(Unit)
                    }
                    mp.start()
                    cont.invokeOnCancellation { mp.release(); mediaPlayer = null }
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    fun startSimulation(scenarioId: Int, context: Context) {
        val scenario = SCENARIOS.find { it.id == scenarioId } ?: SCENARIOS.first()
        sessionId = UUID.randomUUID().toString()
        alertShownAt = 0
        _uiState.update { SimulationUiState(scenarioTitle = scenario.title) }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }

        var callerLineCount = 0

        playbackJob = viewModelScope.launch(Dispatchers.IO) {
            scenario.lines.forEachIndexed { index, line ->

                // User satırı → kaydet + transcribe et
                if (line.speaker == "user") {
                    recordAndTranscribeUser(context, 5000L)
                    return@forEachIndexed
                }

                // Caller satırı
                callerLineCount++
                val audioAsset = "simulation/s${scenarioId}_line${callerLineCount}.mp4"

                _uiState.update { state ->
                    state.copy(visibleLines = state.visibleLines + SimLine(line.speaker, line.text))
                }

                playAsset(context, audioAsset)

                try {
                    val result = ApiService.analyzeText(line.text, index, sessionId)

                    _uiState.update { state ->
                        val updatedLines = state.visibleLines.toMutableList()
                        val lastIdx = updatedLines.lastIndex
                        if (lastIdx >= 0) {
                            updatedLines[lastIdx] = updatedLines[lastIdx].copy(
                                isScam = result.isScam,
                                scamScore = result.scamScore,
                                scamType = result.scamType,
                                suggestion = result.suggestion
                            )
                        }

                        val newRisk = result.ewmaScore
                        val newLevel = result.finalLabel

                        val crossedDanger = newRisk >= 70 && alertShownAt < 70
                        val crossedSuspicious = newRisk >= 40 && alertShownAt < 40

                        val shouldAlert = (crossedDanger || crossedSuspicious) && result.suggestion.isNotEmpty()
                        if (crossedDanger) alertShownAt = 75
                        else if (crossedSuspicious) alertShownAt = 50

                        state.copy(
                            visibleLines = updatedLines,
                            currentRiskPercent = newRisk,
                            riskLevel = newLevel,
                            showAlert = if (shouldAlert) true else state.showAlert,
                            alertSuggestion = if (shouldAlert) result.suggestion else state.alertSuggestion,
                            alertIsDanger = if (shouldAlert) crossedDanger else state.alertIsDanger
                        )
                    }
                } catch (_: Exception) {
                    // Satır gösterilir, sadece analiz skoru olmaz
                }
            }

            timerJob?.cancel()

            // Simülasyon bitince özet bildirim — şüpheli veya tehlikeliyse
            val finalState = _uiState.value
            val finalRisk = finalState.currentRiskPercent
            if (finalRisk >= 40 && !finalState.showAlert) {
                val isDanger = finalRisk >= 70
                val suggestion = finalState.visibleLines
                    .filter { it.suggestion.isNotBlank() }
                    .maxByOrNull { it.scamScore }
                    ?.suggestion ?: ""
                _uiState.update { it.copy(
                    isFinished = true,
                    showAlert = true,
                    alertIsDanger = isDanger,
                    alertSuggestion = suggestion.ifBlank {
                        if (isDanger) "Bu görüşme yüksek risk taşıyor. Kişisel bilgilerinizi paylaşmayın."
                        else "Bu görüşme şüpheli işaretler içeriyor. Dikkatli olun."
                    }
                )}
            } else {
                _uiState.update { it.copy(isFinished = true) }
            }
        }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(showAlert = false) }
    }

    fun stopSimulation() {
        timerJob?.cancel()
        playbackJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        _uiState.update { it.copy(isFinished = true) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        playbackJob?.cancel()
        mediaPlayer?.release()
    }
}
