package com.example.scamdetect.data.model

data class AnalysisStats(
    val total: Int,
    val dangerous: Int,
    val suspicious: Int,
    val safe: Int,
    val avgRisk: Float
)

data class AnalysisRecord(
    val id: String,
    val source: String,          // "file" | "microphone" | "text"
    val transcript: String,
    val label: String,
    val score: Float,
    val isScam: Boolean,
    val scamProbability: Float,
    val safeProbability: Float,
    val riskPercent: Int,
    val riskLevel: String,       // "safe" | "suspicious" | "dangerous"
    val durationSeconds: Float?,
    val fileName: String?,
    val createdAt: String        // ISO 8601 string — UI'da formatlanır
)
