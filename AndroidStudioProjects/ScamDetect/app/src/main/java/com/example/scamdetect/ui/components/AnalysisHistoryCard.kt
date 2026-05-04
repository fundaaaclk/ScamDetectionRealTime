package com.example.scamdetect.ui.components

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetect.ui.theme.CardBackground
import com.example.scamdetect.ui.theme.DangerRed
import com.example.scamdetect.ui.theme.SafeGreen
import com.example.scamdetect.ui.theme.TextPrimary
import com.example.scamdetect.ui.theme.TextSecondary
import com.example.scamdetect.ui.theme.WarningYellow

@Composable
fun AnalysisHistoryCard(
    scenarioName: String,
    date: String,
    duration: String,
    riskLabel: String,
    riskScore: Int,
    isDangerous: Boolean,
    onClick: () -> Unit = {}
) {
    val badgeColor = when {
        riskScore >= 75 -> DangerRed
        riskScore >= 40 -> WarningYellow
        else -> SafeGreen
    }

    val badgeBg = when {
        riskScore >= 75 -> DangerRed.copy(alpha = 0.15f)
        riskScore >= 40 -> WarningYellow.copy(alpha = 0.15f)
        else -> SafeGreen.copy(alpha = 0.15f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = scenarioName,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "$date · $duration",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "● $riskLabel — %$riskScore risk",
            color = badgeColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(badgeBg, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
