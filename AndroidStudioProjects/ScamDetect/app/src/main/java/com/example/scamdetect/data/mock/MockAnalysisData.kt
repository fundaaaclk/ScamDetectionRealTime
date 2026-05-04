package com.example.scamdetect.data.mock

import com.example.scamdetect.data.model.AnalysisChunk
import com.example.scamdetect.data.model.AnalysisResult

object MockAnalysisData {

    val result = AnalysisResult(
        title = "Banka güncelleme senaryosu",
        scenarioName = "Banka hesabı dondu",
        totalAnalyses = 3,
        dangerousCount = 1,
        safeCount = 1,
        riskScore = 87,
        dangerousChunks = 5,
        totalChunks = 7,
        attackTypes = listOf("Vishing", "Aciliyet yaratma", "Kimlik taklidi"),
        safeSignals = listOf("Selamlama", "Tanışma"),
        chunks = listOf(
            AnalysisChunk(
                time = "0:00",
                text = "Merhaba, ben Ziraat Bankası müşteri hizmetlerinden arıyorum.",
                riskScore = 12,
                label = "Düşük risk"
            ),
            AnalysisChunk(
                time = "0:45",
                text = "Banka kimliğinizi doğrulamamız gerekiyor.",
                riskScore = 61,
                label = "Şüpheli"
            ),
            AnalysisChunk(
                time = "1:30",
                text = "Kart bilginizi hemen paylaşmazsanız hesabınız bloke edilecek.",
                riskScore = 87,
                label = "Tehlikeli"
            ),
            AnalysisChunk(
                time = "2:41",
                text = "Hesabınızın kapanmaması için hızlı davranmalısınız.",
                riskScore = 91,
                label = "Tehlikeli"
            )
        )
    )
}