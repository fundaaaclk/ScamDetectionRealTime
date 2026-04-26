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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.scamdetect.ui.components.CallCard
import com.example.scamdetect.ui.components.ChunkItem
import com.example.scamdetect.ui.theme.CardBackground
import com.example.scamdetect.ui.theme.SafeGreen
import com.example.scamdetect.ui.theme.TextPrimary
import com.example.scamdetect.ui.theme.TextSecondary

@Composable
fun SimulationScreen(navController: NavController) {
    val result = MockAnalysisData.result
    // İlk chunk'ı göster (simülasyon başlangıç durumu)
    val firstChunk = result.chunks.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Back button + Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CardBackground)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Geri",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Canlı analiz",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "${result.scenarioName} senaryosu",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Call card — başlangıç durumu: ilk chunk'ın risk skoru
            CallCard(
                callerNumber = "+90 (Bilinmeyen)",
                status = "Kayıt oynatılıyor",
                recordingTime = "00:03",
                riskScore = firstChunk?.riskScore ?: 0,
                riskLabel = when {
                    (firstChunk?.riskScore ?: 0) >= 75 -> "Tehlikeli"
                    (firstChunk?.riskScore ?: 0) >= 50 -> "Şüpheli"
                    else -> "Düşük risk"
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CHUNK ANALİZİ
            Text(
                text = "CHUNK ANALİZİ",
                color = TextSecondary,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sadece ilk chunk gösterilir (simülasyon başlangıcı)
            if (firstChunk != null) {
                ChunkItem(chunk = firstChunk)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Down arrow indicator
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Aşağı kaydır",
                tint = TextSecondary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Tekrar Oynat (green)
            Button(
                onClick = { navController.navigate(Screen.ModeSelection.route) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafeGreen
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Tekrar Oynat",
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            // Raporu gör (outlined)
            OutlinedButton(
                onClick = { navController.navigate(Screen.Report.route) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    "Raporu gör →",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}