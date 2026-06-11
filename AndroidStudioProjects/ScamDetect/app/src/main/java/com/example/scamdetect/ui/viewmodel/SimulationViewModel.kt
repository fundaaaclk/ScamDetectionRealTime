package com.example.scamdetect.ui.viewmodel

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
                    ScenarioLine("caller", "Hesabınızda şüpheli bir işlem tespit ettik ve kartınızı geçici olarak dondurduk.", 3500L),
                    ScenarioLine("caller", "Hesabınızı yeniden aktif etmek için kimliğinizi doğrulamamız gerekiyor.", 3500L),
                    ScenarioLine("caller", "Lütfen kart şifrenizi ve son 3 hane CVV numaranızı bana söyler misiniz?", 3500L),
                    ScenarioLine("caller", "İşlemi hızlıca tamamlamazsak hesabınız 24 saat bloke kalacak.", 4000L),
                    ScenarioLine("caller", "Ayrıca size gelen SMS doğrulama kodunu da paylaşmanız gerekiyor.", 3500L),
                )
            ),
            ScenarioData(
                id = 2,
                title = "Kargo Gümrük Ödemesi",
                category = "kargo_gumruk",
                preview = "Uluslararası kargonuz gümrükte bekliyor, ödeme yapılması gerekiyor.",
                lines = listOf(
                    ScenarioLine("caller", "İyi günler, PTT kargo müşteri hizmetlerinden arıyorum.", 0L),
                    ScenarioLine("caller", "Yurt dışından size gelen bir paketiniz gümrükte bekliyor.", 3500L),
                    ScenarioLine("caller", "Paketi serbest bırakmak için 187 TL gümrük vergisi ödemeniz gerekiyor.", 3500L),
                    ScenarioLine("caller", "Ödemeyi şu an telefonda kart bilgilerinizle halledebiliriz.", 3500L),
                    ScenarioLine("caller", "Bugün ödeme yapmazsanız paket iade edilecek ve tekrar gönderim ücreti ödersiniz.", 4000L),
                )
            ),
            ScenarioData(
                id = 3,
                title = "Kripto Yatırım Teklifi",
                category = "finans_kripto",
                preview = "Günde %30 garantili kripto kazancı için acil fırsat sunuluyor.",
                lines = listOf(
                    ScenarioLine("caller", "Merhaba, sizi bir yatırım fırsatı için arıyorum.", 0L),
                    ScenarioLine("caller", "Platformumuz yapay zeka ile kripto ticareti yapıyor ve günlük %30 getiri garantisi sunuyoruz.", 3500L),
                    ScenarioLine("caller", "Şu an sadece 12 kontenjanımız kaldı, bugün kaydolursanız ilk ay komisyon almıyoruz.", 4000L),
                    ScenarioLine("caller", "Minimum 500 dolar ile başlayabilirsiniz, USDT veya banka transferi kabul ediyoruz.", 3500L),
                    ScenarioLine("caller", "Hemen IBAN'ınızı veya kripto cüzdan adresinizi verir misiniz?", 3500L),
                )
            ),
            ScenarioData(
                id = 4,
                title = "SGK Prim Borç Uyarısı",
                category = "resmi_kurum",
                preview = "SGK müdürlüğünden aranıyorsunuz — prim borcu nedeniyle dosyanız kapanacak.",
                lines = listOf(
                    ScenarioLine("caller", "İyi günler, Sosyal Güvenlik Kurumu İstanbul müdürlüğünden arıyorum.", 0L),
                    ScenarioLine("caller", "Kayıtlarımıza göre 2023 yılına ait 3.840 TL prim borcunuz bulunmaktadır.", 3500L),
                    ScenarioLine("caller", "Bu borç bugün ödenmediği takdirde dosyanız kapatılacak ve sağlık güvenceniz askıya alınacak.", 4000L),
                    ScenarioLine("caller", "Ödemeyi şimdi kredi kartıyla telefonda yapabilirsiniz, size özel indirimli ödeme planı sunabiliriz.", 4000L),
                    ScenarioLine("caller", "TC kimlik numaranızı ve kart bilgilerinizi alabilir miyim?", 3500L),
                )
            )
        )
    }

    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var playbackJob: Job? = null
    private var alertShownAt = 0  // 0=none, 50=suspicious, 75=dangerous

    fun startSimulation(scenarioId: Int) {
        val scenario = SCENARIOS.find { it.id == scenarioId } ?: SCENARIOS.first()
        _uiState.update { it.copy(scenarioTitle = scenario.title) }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }

        playbackJob = viewModelScope.launch(Dispatchers.IO) {
            scenario.lines.forEachIndexed { index, line ->
                if (line.delayMs > 0) delay(line.delayMs)

                _uiState.update { state ->
                    state.copy(visibleLines = state.visibleLines + SimLine(line.speaker, line.text))
                }

                try {
                    val result = ApiService.analyzeText(line.text, index)

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

                        val newRisk = maxOf(state.currentRiskPercent, result.scamScore)
                        val newLevel = when {
                            newRisk >= 75 -> "dangerous"
                            newRisk >= 50 -> "suspicious"
                            else -> "safe"
                        }

                        // Tehlike eşiğini geçince bir kez uyarı göster
                        val crossedDanger = newRisk >= 75 && alertShownAt < 75
                        val crossedSuspicious = newRisk >= 50 && alertShownAt < 50

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
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(showAlert = false) }
    }

    fun stopSimulation() {
        timerJob?.cancel()
        playbackJob?.cancel()
        _uiState.update { it.copy(isFinished = true) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        playbackJob?.cancel()
    }
}
