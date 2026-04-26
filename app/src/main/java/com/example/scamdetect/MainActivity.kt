package com.example.scamdetect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.scamdetect.navigation.AppNavGraph
import com.example.scamdetect.ui.theme.ScamDetectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScamDetectTheme {
                AppNavGraph()
            }
        }
    }
}