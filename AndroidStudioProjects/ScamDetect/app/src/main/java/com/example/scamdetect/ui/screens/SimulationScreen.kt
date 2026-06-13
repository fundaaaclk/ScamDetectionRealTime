package com.example.scamdetect.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.scamdetect.navigation.Screen
import com.example.scamdetect.ui.components.CallCard
import com.example.scamdetect.ui.theme.AppBackground
import com.example.scamdetect.ui.theme.CardBackground
import com.example.scamdetect.ui.theme.CardBorder
import com.example.scamdetect.ui.theme.DangerRed
import com.example.scamdetect.ui.theme.PurplePrimary
import com.example.scamdetect.ui.theme.SafeGreen
import com.example.scamdetect.ui.theme.TextPrimary
import com.example.scamdetect.ui.theme.TextSecondary
import com.example.scamdetect.ui.theme.WarningYellow
import com.example.scamdetect.ui.viewmodel.SimLine
import com.example.scamdetect.ui.viewmodel.SimulationViewModel

@Composable
fun SimulationScreen(navController: NavController, scenarioId: Int) {
    val vm: SimulationViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) { vm.startSimulation(scenarioId, context) }

    LaunchedEffect(state.visibleLines.size) {
        if (state.visibleLines.isNotEmpty()) {
            listState.animateScrollToItem(state.visibleLines.lastIndex)
        }
    }

    val riskColor = when {
        state.currentRiskPercent >= 75 -> DangerRed
        state.currentRiskPercent >= 50 -> WarningYellow
        else -> SafeGreen
    }

    val riskLabel = when (state.riskLevel) {
        "dangerous" -> "Tehlikeli"
        "suspicious" -> "Şüpheli"
        else -> "Güvenli"
    }

    val elapsedFormatted = "%02d:%02d".format(
        state.elapsedSeconds / 60, state.elapsedSeconds % 60
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Ana içerik ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        vm.stopSimulation()
                        navController.popBackStack()
                    },
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
                        text = "Simülasyon",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = state.scenarioTitle,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CallCard(
                callerNumber = "+90 (Bilinmeyen)",
                status = if (state.isFinished) "Görüşme tamamlandı" else "Konuşma analiz ediliyor…",
                recordingTime = elapsedFormatted,
                riskScore = state.currentRiskPercent,
                riskLabel = riskLabel
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "KONUŞMA",
                color = TextSecondary,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.visibleLines) { line ->
                    ChatBubble(line)
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Risk progress bar
            LinearProgressIndicator(
                progress = { state.currentRiskPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = riskColor,
                trackColor = CardBorder
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isFinished) {
                Button(
                    onClick = { navController.navigate(Screen.ModeSelection.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                ) {
                    Text(
                        "Yeni Simülasyon →",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = AppBackground
                    )
                }
            } else if (state.isRecordingUser) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(PurplePrimary.copy(alpha = 0.15f), RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null,
                            tint = PurplePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dinleniyor...", color = PurplePrimary,
                            fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            } else {
                Button(
                    onClick = { vm.stopSimulation() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Görüşmeyi Sonlandır", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Uyarı overlay ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.showAlert,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .background(CardBackground, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.alertIsDanger) "TEHLİKELİ ARAMA" else "ŞÜPHELI KONUŞMA",
                        color = if (state.alertIsDanger) DangerRed else WarningYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bu konuşma dolandırıcılık olabilir!",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.alertSuggestion,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { vm.dismissAlert() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.alertIsDanger) DangerRed else WarningYellow
                        )
                    ) {
                        Text(
                            "Anladım",
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.alertIsDanger) TextPrimary else AppBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(line: SimLine) {
    val isUser = line.speaker == "user"
    val riskColor = when {
        line.scamScore >= 75 -> DangerRed
        line.scamScore >= 50 -> WarningYellow
        else -> null
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isUser) PurplePrimary.copy(alpha = 0.2f)
                            else if (riskColor != null) riskColor.copy(alpha = 0.12f)
                            else CardBackground,
                    shape = if (isUser)
                        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    else
                        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = line.text,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        if (!isUser && line.scamScore > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                if (riskColor != null) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = riskColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "%${line.scamScore} risk",
                    color = riskColor ?: SafeGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (line.scamType.isNotEmpty()) {
                    Text(
                        text = " · ${line.scamType.replace("_", " ")}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
