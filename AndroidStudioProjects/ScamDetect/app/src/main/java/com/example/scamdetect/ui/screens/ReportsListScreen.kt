package com.example.scamdetect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetect.ui.components.AnalysisHistoryCard
import com.example.scamdetect.ui.theme.CardBackground
import com.example.scamdetect.ui.theme.CardBorder
import com.example.scamdetect.ui.theme.DangerRed
import com.example.scamdetect.ui.theme.SafeGreen
import com.example.scamdetect.ui.theme.TextPrimary
import com.example.scamdetect.ui.theme.TextSecondary
import com.example.scamdetect.ui.theme.WarningYellow

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import com.example.scamdetect.ui.viewmodel.ReportsViewModel

@Composable
fun ReportsListScreen(
    vm: ReportsViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val stats = uiState.stats
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = "Raporlar",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Text(
            text = "Geçmiş analiz sonuçları",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Summary stats
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Toplam
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(CardBackground, RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "${stats.total}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    text = "Toplam",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Tehlikeli
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(CardBackground, RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "${stats.dangerous}",
                    color = DangerRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    text = "Tehlikeli",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Güvenli
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(CardBackground, RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "${stats.safe}",
                    color = SafeGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    text = "Güvenli",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section header
        Text(
            text = "GEÇMİŞ ANALİZLER",
            color = TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (uiState.analyses.isEmpty()) {
            Text("Kayıt bulunamadı.", color = TextSecondary, fontSize = 14.sp)
        } else {
            uiState.analyses.forEach { record ->
                val riskLabelTr = when (record.riskLevel) {
                    "dangerous" -> "Tehlikeli"
                    "suspicious" -> "Şüpheli"
                    else -> "Güvenli"
                }
                val title = record.fileName ?: if (record.source == "microphone") "Canlı Kayıt" else "Metin Analizi"
                val dateStr = record.createdAt.take(10) // YYYY-MM-DD
                val durStr = record.durationSeconds?.let { "${it.toInt()}sn" } ?: "-"

                AnalysisHistoryCard(
                    scenarioName = title,
                    date = dateStr,
                    duration = durStr,
                    riskLabel = riskLabelTr,
                    riskScore = record.riskPercent,
                    isDangerous = record.isScam
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Ortalama risk
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "ORTALAMA RİSK SKORU",
                color = TextSecondary,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "%${stats.avgRisk}",
                color = WarningYellow,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            Text(
                text = "Son 5 analiz ortalaması",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
