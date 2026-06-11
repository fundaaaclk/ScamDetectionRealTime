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
    val source: String,
    val transcript: String,
    val label: String,
    val isScam: Boolean,
    val scamType: String,
    val scamScore: Int,
    val suggestion: String,
    val score: Float,
    val scamProbability: Float,
    val safeProbability: Float,
    val riskPercent: Int,
    val riskLevel: String,
    val durationSeconds: Float?,
    val fileName: String?,
    val createdAt: String
)
