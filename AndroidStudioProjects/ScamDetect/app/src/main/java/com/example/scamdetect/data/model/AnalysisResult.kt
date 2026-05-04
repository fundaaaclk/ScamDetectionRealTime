package com.example.scamdetect.data.model

data class AnalysisResult(
    val title: String,
    val scenarioName: String,
    val totalAnalyses: Int,
    val dangerousCount: Int,
    val safeCount: Int,
    val riskScore: Int,
    val dangerousChunks: Int,
    val totalChunks: Int,
    val attackTypes: List<String>,
    val safeSignals: List<String>,
    val chunks: List<AnalysisChunk>
)