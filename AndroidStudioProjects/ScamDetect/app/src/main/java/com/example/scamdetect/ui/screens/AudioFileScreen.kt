package com.example.scamdetect.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.scamdetect.navigation.Screen
import com.example.scamdetect.data.model.TranscribeResult
import com.example.scamdetect.ui.theme.*
import com.example.scamdetect.ui.viewmodel.AudioFileUiState
import com.example.scamdetect.ui.viewmodel.AudioFileViewModel
import kotlin.math.roundToInt

@Composable
fun AudioFileScreen(
    navController: NavController,
    vm: AudioFileViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }

    // Dosya seçici — ses ve video dosyalarını kabul et
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedUri = it
            selectedName = it.lastPathSegment?.substringAfterLast("/") ?: "ses_dosyası"
            vm.reset()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── Başlık satırı ──────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(36.dp).clip(CircleShape).background(CardBackground)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri",
                    tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Ses Dosyası Analizi", color = TextPrimary,
                    fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Ses kaydını seç ve analiz et", color = TextSecondary, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Dosya seç kutusu ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (selectedUri != null) PurplePrimary else CardBorder,
                    RoundedCornerShape(16.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📁", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                if (selectedName != null) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = SafeGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selectedName!!,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text("Dosya seçilmedi", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("mp3 · wav · mp4 · m4a desteklenir",
                        color = TextSecondary.copy(alpha = 0.6f), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("audio/*", "video/mp4", "video/m4v")) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurplePrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PurplePrimary)
                ) {
                    Text(if (selectedUri == null) "Dosya Seç" else "Farklı Dosya Seç",
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Analiz butonu ───────────────────────────────────────────────────────
        Button(
            onClick = { selectedUri?.let { vm.analyzeFile(it, context) } },
            enabled = selectedUri != null && uiState !is AudioFileUiState.Loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
        ) {
            if (uiState is AudioFileUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Analiz ediliyor…", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            } else {
                Text("▶  Analizi Başlat", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Sonuç ─────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState is AudioFileUiState.Success,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut()
        ) {
            val result = (uiState as? AudioFileUiState.Success)?.result
            if (result != null) {
                Column {
                    ResultCard(result = result)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (result.analysisId != null) {
                        OutlinedButton(
                            onClick = { navController.navigate(Screen.Report.createRoute(result.analysisId)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PurplePrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PurplePrimary)
                        ) {
                            Text("Detaylı Raporu Gör", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState is AudioFileUiState.Error,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val msg = (uiState as? AudioFileUiState.Error)?.message ?: ""
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DangerRed.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .border(1.dp, DangerRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text("⚠ Hata", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(msg, color = TextSecondary, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ResultCard(result: TranscribeResult) {
    val riskPercent = (result.scamProbability * 100).roundToInt()
    val riskColor = when {
        riskPercent >= 75 -> DangerRed
        riskPercent >= 50 -> WarningYellow
        else              -> SafeGreen
    }
    val riskLabel = when {
        riskPercent >= 75 -> "Tehlikeli"
        riskPercent >= 50 -> "Şüpheli"
        else              -> "Güvenli"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Text("ANALİZ SONUCU", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Risk skoru satırı
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "%$riskPercent",
                    color = riskColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp
                )
                Text("Risk skoru", color = TextSecondary, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .background(riskColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, riskColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(riskLabel, color = riskColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = CardBorder)
        Spacer(modifier = Modifier.height(16.dp))

        // Risk bar
        LinearProgressIndicator(
            progress = { result.scamProbability },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = riskColor,
            trackColor = CardBorder
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Süre
        result.durationSeconds?.let { dur ->
            InfoRow(label = "Süre", value = "%.1f saniye".format(dur))
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Olasılıklar
        InfoRow(label = "Scam olasılığı", value = "%$riskPercent")
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(label = "Güvenli olasılığı", value = "%${(result.safeProbability * 100).roundToInt()}")

        // Öneri
        if (result.suggestion.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = CardBorder)
            Spacer(modifier = Modifier.height(16.dp))
            Text("ÖNERİ", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarningYellow.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(1.dp, WarningYellow.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(result.suggestion, color = TextPrimary, fontSize = 14.sp, lineHeight = 22.sp)
            }
        }

        // Transcript
        if (result.transcript.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = CardBorder)
            Spacer(modifier = Modifier.height(16.dp))
            Text("TRANSKRİPT", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"${result.transcript}\"",
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
