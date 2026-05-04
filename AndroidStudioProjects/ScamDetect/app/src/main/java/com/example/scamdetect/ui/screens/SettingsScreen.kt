package com.example.scamdetect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetect.ui.theme.CardBackground
import com.example.scamdetect.ui.theme.CardBorder
import com.example.scamdetect.ui.theme.PurplePrimary
import com.example.scamdetect.ui.theme.SafeGreen
import com.example.scamdetect.ui.theme.TextPrimary
import com.example.scamdetect.ui.theme.TextSecondary
import com.example.scamdetect.ui.theme.TextTertiary

@Composable
fun SettingsScreen() {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var highRiskAlerts by remember { mutableStateOf(true) }
    var autoAnalysis by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = "Ayarlar",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Text(
            text = "Uygulama tercihleri",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // GENEL section
        Text(
            text = "GENEL",
            color = TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            SettingsToggleItem(
                icon = "🔔",
                title = "Bildirimler",
                subtitle = "Analiz sonuçlarını bildir",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsToggleItem(
                icon = "🚨",
                title = "Yüksek risk uyarıları",
                subtitle = "Risk %70 üstünde anlık uyarı",
                checked = highRiskAlerts,
                onCheckedChange = { highRiskAlerts = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsToggleItem(
                icon = "⚡",
                title = "Otomatik analiz",
                subtitle = "Arama sonrası otomatik başlat",
                checked = autoAnalysis,
                onCheckedChange = { autoAnalysis = it }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ANALİZ section
        Text(
            text = "ANALİZ",
            color = TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            SettingsInfoItem(
                icon = "🧠",
                title = "Model versiyonu",
                value = "v2.1.0"
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoItem(
                icon = "📊",
                title = "Eşik değeri",
                value = "%70"
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoItem(
                icon = "⏱",
                title = "Chunk süresi",
                value = "30 sn"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // HAKKINDA section
        Text(
            text = "HAKKINDA",
            color = TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            SettingsInfoItem(
                icon = "📱",
                title = "Uygulama",
                value = "SE Shield v1.0"
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoItem(
                icon = "🎓",
                title = "Proje",
                value = "Bitirme Projesi"
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsInfoItem(
                icon = "💻",
                title = "Platform",
                value = "Android"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Version footer
        Text(
            text = "SE Shield — Sosyal Mühendislik Tespit Sistemi",
            color = TextTertiary,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "© 2026 Bitirme Projesi",
            color = TextTertiary,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsToggleItem(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = PurplePrimary,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = CardBorder
            )
        )
    }
}

@Composable
private fun SettingsInfoItem(
    icon: String,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        Text(
            text = value,
            color = TextSecondary,
            fontSize = 14.sp
        )
    }
}
