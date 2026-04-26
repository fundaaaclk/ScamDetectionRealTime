package com.example.scamdetect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetect.ui.theme.DangerRed

@Composable
fun RiskAlertCard(
    title: String,
    descriptions: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DangerRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .background(
                DangerRed.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row {
            Text("🚨", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = DangerRed,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        descriptions.forEach { desc ->
            Text(
                text = desc,
                color = DangerRed.copy(alpha = 0.85f),
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 24.dp, bottom = 2.dp)
            )
        }
    }
}
