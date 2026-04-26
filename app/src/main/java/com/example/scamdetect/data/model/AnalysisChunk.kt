package com.example.scamdetect.data.model

data class AnalysisChunk(
    val time: String,
    val text: String,
    val riskScore: Int,
    val label: String
)