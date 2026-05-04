package com.example.scamdetect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.scamdetect.data.mock.MockAnalysisData
import com.example.scamdetect.navigation.Screen
import com.example.scamdetect.ui.components.AnalysisHistoryCard
import com.example.scamdetect.ui.components.RiskCard
import com.example.scamdetect.ui.theme.CardBackground
import com.example.scamdetect.ui.theme.DangerRed
import com.example.scamdetect.ui.theme.PurplePrimary
import com.example.scamdetect.ui.theme.SafeGreen
import com.example.scamdetect.ui.theme.TextPrimary
import com.example.scamdetect.ui.theme.TextSecondary

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamdetect.ui.viewmodel.HomeViewModel
import androidx.compose.runtime.getValue
import androidx.compose.material3.CircularProgressIndicator

@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel = viewModel()
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

        // App Logo + Name + Camera icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PurplePrimary),
                contentAlignment = Alignment.Center
            ) {
                Text("🛡", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "SE Shield",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Sosyal mühendislik tespiti",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // GENEL DURUM
        Text(
            text = "GENEL DURUM",
            color = TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RiskCard(
                value = "${stats.total}",
                label = "Analiz",
                valueColor = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            RiskCard(
                value = "${stats.dangerous}",
                label = "Tehlikeli",
                valueColor = DangerRed,
                modifier = Modifier.weight(1f)
            )
            RiskCard(
                value = "${stats.safe}",
                label = "Güvenli",
                valueColor = SafeGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SON ANALİZLER
        Text(
            text = "SON ANALİZLER",
            color = TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (uiState.recentAnalyses.isEmpty()) {
            Text("Henüz analiz yapılmamış.", color = TextSecondary, fontSize = 14.sp)
        } else {
            uiState.recentAnalyses.forEach { record ->
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

        // Bottom button
        Button(
            onClick = { navController.navigate(Screen.ModeSelection.route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PurplePrimary
            )
        ) {
            Text(
                "Yeni simülasyon başlat",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}