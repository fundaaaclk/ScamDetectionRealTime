package com.example.scamdetect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.scamdetect.data.model.AnalysisChunk
import com.example.scamdetect.ui.theme.*

@Composable
fun ChunkItem(chunk: AnalysisChunk) {
    val riskColor = when {
        chunk.riskScore >= 75 -> DangerRed
        chunk.riskScore >= 50 -> WarningYellow
        else -> SafeGreen
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chunk.time,
                color = TextSecondary,
                modifier = Modifier.width(48.dp)
            )

            Text(
                text = chunk.text,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "%${chunk.riskScore}",
                color = riskColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { chunk.riskScore / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = riskColor,
            trackColor = CardBorder
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = chunk.label,
            color = riskColor
        )
    }
}