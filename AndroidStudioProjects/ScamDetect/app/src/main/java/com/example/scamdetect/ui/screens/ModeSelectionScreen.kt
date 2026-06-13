package com.example.scamdetect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.scamdetect.navigation.Screen
import com.example.scamdetect.ui.theme.CardBackground
import com.example.scamdetect.ui.theme.CardBorder
import com.example.scamdetect.ui.theme.DangerRed
import com.example.scamdetect.ui.theme.PurplePrimary
import com.example.scamdetect.ui.theme.TextPrimary
import com.example.scamdetect.ui.theme.TextSecondary
import com.example.scamdetect.ui.theme.WarningYellow
import com.example.scamdetect.ui.viewmodel.ScenarioData
import com.example.scamdetect.ui.viewmodel.SimulationViewModel

@Composable
fun ModeSelectionScreen(navController: NavController) {
    var selectedScenarioId by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

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
                    text = "Giriş modu seç",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Analiz kaynağını belirle",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hazır senaryo — RECOMMENDED
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = PurplePrimary.copy(alpha = 0.25f),
                        topLeft = Offset(-4f, -4f),
                        size = Size(size.width + 8f, size.height + 8f),
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, PurplePrimary, RoundedCornerShape(16.dp))
                    .background(CardBackground, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.PlayCircle,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hazır senaryo",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "ONERILIR",
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Gerçek scam konuşması oynatılır, sistem canlı analiz eder",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ModeCard(
            icon = Icons.Outlined.AudioFile,
            title = "Ses dosyası yükle",
            subtitle = ".mp3 / .wav — kendi kaydınızı analiz edin",
            onClick = { navController.navigate("audio_file") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ModeCard(
            icon = Icons.Outlined.Mic,
            title = "Mikrofon kaydı",
            subtitle = "Anlık konuşma kaydı ve canlı analiz",
            onClick = { navController.navigate("microphone") }
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "SENARYO SEÇ",
            color = TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        SimulationViewModel.SCENARIOS.forEach { scenario ->
            ScenarioCard(
                scenario = scenario,
                isSelected = selectedScenarioId == scenario.id,
                onClick = { selectedScenarioId = scenario.id }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                navController.navigate(Screen.Simulation.createRoute(selectedScenarioId))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
        ) {
            Text(
                "Simülasyonu Başlat",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ScenarioCard(
    scenario: ScenarioData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = when (scenario.category) {
        "banka_kart_hesap" -> DangerRed
        "kargo_gumruk"     -> WarningYellow
        "finans_kripto"    -> WarningYellow
        "resmi_kurum"      -> DangerRed
        else               -> PurplePrimary
    }

    val categoryLabel = when (scenario.category) {
        "banka_kart_hesap" -> "Banka"
        "kargo_gumruk"     -> "Kargo"
        "finans_kripto"    -> "Kripto"
        "resmi_kurum"      -> "Resmi Kurum"
        else               -> scenario.category
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) PurplePrimary else CardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(80.dp)
                .background(
                    color = categoryColor,
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = scenario.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(
                            categoryColor.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = categoryLabel,
                        color = categoryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = scenario.preview,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 34.dp)
        )
    }
}
