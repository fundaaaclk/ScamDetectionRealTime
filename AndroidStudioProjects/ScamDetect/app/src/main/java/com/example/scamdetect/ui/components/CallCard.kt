package com.example.scamdetect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetect.ui.theme.CardBackground
import com.example.scamdetect.ui.theme.CardBorder
import com.example.scamdetect.ui.theme.DangerRed
import com.example.scamdetect.ui.theme.SafeGreen
import com.example.scamdetect.ui.theme.TextPrimary
import com.example.scamdetect.ui.theme.TextSecondary
import com.example.scamdetect.ui.theme.WarningYellow

@Composable
fun CallCard(
    callerNumber: String,
    status: String,
    recordingTime: String,
    riskScore: Int,
    riskLabel: String
) {
    val riskColor = when {
        riskScore >= 75 -> DangerRed
        riskScore >= 50 -> WarningYellow
        else -> SafeGreen
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Phone icon + Caller info + Recording indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Phone icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CardBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Telefon",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Caller info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = callerNumber,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = status,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            // Recording indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(DangerRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = recordingTime,
                    color = DangerRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Risk score label + value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Risk skoru",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Text(
                text = riskLabel,
                color = riskColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { riskScore / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = riskColor,
            trackColor = CardBorder
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Risk label
        Text(
            text = riskLabel,
            color = riskColor,
            fontSize = 12.sp
        )
    }
}
