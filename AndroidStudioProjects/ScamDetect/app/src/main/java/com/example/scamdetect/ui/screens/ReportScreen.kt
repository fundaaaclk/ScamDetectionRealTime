package com.example.scamdetect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.scamdetect.navigation.Screen
import com.example.scamdetect.ui.components.AttackTag
import com.example.scamdetect.ui.components.RiskAlertCard
import com.example.scamdetect.ui.components.TimelineItem
import com.example.scamdetect.ui.theme.CardBackground
import com.example.scamdetect.ui.theme.CardBorder
import com.example.scamdetect.ui.theme.DangerRed
import com.example.scamdetect.ui.theme.PurplePrimary
import com.example.scamdetect.ui.theme.TextPrimary
import com.example.scamdetect.ui.theme.TextSecondary
import com.example.scamdetect.ui.theme.WarningYellow
import com.example.scamdetect.ui.theme.SafeGreen

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.CircularProgressIndicator
import com.example.scamdetect.ui.viewmodel.ReportViewModel

@Composable
fun ReportScreen(
    navController: NavController,
    analysisId: String,
    vm: ReportViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(analysisId) {
        vm.loadReport(analysisId)
    }

    val record = uiState.record

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
                    text = "Analiz raporu",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = record?.fileName ?: if (record?.source == "microphone") "Canlı Kayıt" else "Metin Analizi",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (record == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Rapor bulunamadı veya yüklenemedi.", color = TextSecondary)
            }
        } else {
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Big risk cards
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Risk score card - large
                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .background(CardBackground, RoundedCornerShape(16.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = when {
                                record.riskPercent >= 75 -> "Tehlikeli"
                                record.riskPercent >= 40 -> "Şüpheli"
                                else -> "Güvenli"
                            },
                            color = if (record.riskPercent >= 75) DangerRed else if (record.riskPercent >= 40) WarningYellow else SafeGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                        Text(
                            text = "Risk skoru",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    // Source card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(CardBackground, RoundedCornerShape(16.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = record.source.replaceFirstChar { it.uppercase() },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Kaynak",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

            Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(16.dp))

                // SUGGESTION
                if (record.suggestion.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WarningYellow.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .border(1.dp, WarningYellow.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "ÖNERİ",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = record.suggestion,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // TRANSCRIPT
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(16.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "SES DÖKÜMÜ (TRANSCRIPT)",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = record.transcript.ifBlank { "Metin bulunamadı." },
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
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

        // Bottom button
        Button(
            onClick = {
                navController.navigate(Screen.ModeSelection.route) {
                    popUpTo(Screen.Home.route)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PurplePrimary
            )
        ) {
            Text(
                "Yeni Analiz Başlat",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}