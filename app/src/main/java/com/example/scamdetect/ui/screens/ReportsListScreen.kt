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

@Composable
fun ReportsListScreen() {
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
                    text = "5",
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
                    text = "2",
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
                    text = "3",
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

        // Report cards
        AnalysisHistoryCard(
            scenarioName = "Banka güncelleme senaryosu",
            date = "Bugün",
            duration = "3dk",
            riskLabel = "Tehlikeli",
            riskScore = 87,
            isDangerous = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        AnalysisHistoryCard(
            scenarioName = "Kargo bildirimi senaryosu",
            date = "Dün",
            duration = "1dk",
            riskLabel = "Şüpheli",
            riskScore = 61,
            isDangerous = false
        )

        Spacer(modifier = Modifier.height(10.dp))

        AnalysisHistoryCard(
            scenarioName = "Komşu arama senaryosu",
            date = "2 gün önce",
            duration = "2dk",
            riskLabel = "Güvenli",
            riskScore = 15,
            isDangerous = false
        )

        Spacer(modifier = Modifier.height(10.dp))

        AnalysisHistoryCard(
            scenarioName = "SGK dolandırıcılığı",
            date = "3 gün önce",
            duration = "4dk",
            riskLabel = "Tehlikeli",
            riskScore = 92,
            isDangerous = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        AnalysisHistoryCard(
            scenarioName = "Tanıdık arama testi",
            date = "1 hafta önce",
            duration = "1dk",
            riskLabel = "Güvenli",
            riskScore = 8,
            isDangerous = false
        )

        Spacer(modifier = Modifier.height(24.dp))

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
                text = "%52.6",
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
