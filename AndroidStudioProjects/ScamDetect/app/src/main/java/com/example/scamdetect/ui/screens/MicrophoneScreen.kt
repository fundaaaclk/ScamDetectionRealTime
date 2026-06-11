package com.example.scamdetect.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.scamdetect.data.model.AnalysisChunk
import com.example.scamdetect.navigation.Screen
import com.example.scamdetect.ui.components.CallCard
import com.example.scamdetect.ui.components.ChunkItem
import com.example.scamdetect.ui.theme.*
import com.example.scamdetect.ui.viewmodel.MicrophoneViewModel
import kotlin.math.roundToInt

@Composable
fun MicrophoneScreen(
    navController: NavController,
    vm: MicrophoneViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            vm.startRecording(context)
        } else {
            // İzin reddedildi
        }
    }

    DisposableEffect(Unit) {
        onDispose { vm.stopRecording() }
    }

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
                    text = "Mikrofon Analizi",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Canlı konuşma analizi",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val riskLabel = when (uiState.finalLabel) {
            "dangerous"  -> "Tehlikeli"
            "suspicious" -> "Şüpheli"
            else         -> "Güvenli"
        }

        val trendText = when (uiState.trend) {
            "rising"  -> "↑ Yükseliyor"
            "falling" -> "↓ Düşüyor"
            "stable"  -> "→ Stabil"
            else      -> ""
        }

        CallCard(
            callerNumber = "Canlı Kayıt" + if (uiState.alarm) " 🚨" else "",
            status = if (uiState.isRecording) "🎙 Kaydediliyor${if (trendText.isNotEmpty()) " · $trendText" else ""}" else "Durdu${if (trendText.isNotEmpty()) " · $trendText" else ""}",
            recordingTime = String.format("%02d:%02d", uiState.elapsedSeconds / 60, uiState.elapsedSeconds % 60),
            riskScore = uiState.overallRiskPercent,
            riskLabel = riskLabel
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "CHUNK ANALİZİ",
            color = TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.chunks) { chunk ->
                val chunkRiskPercent = (chunk.scamProbability * 100).roundToInt()
                val chunkLabel = when {
                    chunkRiskPercent >= 75 -> "Tehlikeli"
                    chunkRiskPercent >= 50 -> "Şüpheli"
                    else -> "Düşük risk"
                }

                // ChunkItem'ın AnalysisChunk istediğini varsayıyoruz, onu oluştur:
                ChunkItem(
                    chunk = AnalysisChunk(
                        time = String.format("%02d:%02d", (chunk.chunkIndex * 3) / 60, (chunk.chunkIndex * 3) % 60),
                        text = chunk.transcript,
                        riskScore = chunkRiskPercent,
                        label = chunkLabel
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!uiState.isRecording && uiState.topSuggestion.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarningYellow.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(1.dp, WarningYellow.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("ÖNERİ", color = TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(uiState.topSuggestion, color = TextPrimary, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.errorMessage != null) {
            Text(
                text = "⚠ ${uiState.errorMessage}",
                color = DangerRed,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DangerRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isRecording) {
                Button(
                    onClick = { vm.stopRecording() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Durdur", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            } else {
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Başlat", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }

            if (uiState.analysisId != null) {
                OutlinedButton(
                    onClick = { navController.navigate(Screen.Report.createRoute(uiState.analysisId!!)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("Raporu gör →", fontWeight = FontWeight.SemiBold)
                }
            } else if (!uiState.isRecording && uiState.chunks.isNotEmpty()) {
                // Kayıt bitmiş ama sunucudan id henüz dönmemişse
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
