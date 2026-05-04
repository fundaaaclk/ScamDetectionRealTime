package com.example.scamdetect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.scamdetect.ui.theme.DangerRed
import com.example.scamdetect.ui.theme.SafeGreen
import androidx.compose.ui.graphics.Color

@Composable
fun AttackTag(
    text: String,
    isSafe: Boolean = false
) {
    Text(
        text = text,
        color = if (isSafe) SafeGreen else DangerRed,
        modifier = Modifier
            .background(
                color = if (isSafe) Color(0xFF0B3D2A) else Color(0xFF3A1111),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}