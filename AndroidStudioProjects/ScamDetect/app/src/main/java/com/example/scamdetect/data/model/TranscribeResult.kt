package com.example.scamdetect.data.model

data class TranscribeResult(
    val analysisId: String?,
    val transcript: String,
    val label: String,
    val isScam: Boolean,
    val scamType: String,
    val scamScore: Int,
    val score: Float,
    val scamProbability: Float,
    val safeProbability: Float,
    val suggestion: String,
    val durationSeconds: Float?
)

data class ChunkResult(
    val chunkIndex: Int,
    val transcript: String,
    val label: String,
    val isScam: Boolean,
    val scamType: String,
    val scamScore: Int,
    val score: Float,
    val scamProbability: Float,
    val safeProbability: Float,
    val suggestion: String,
    // EWMA + Ensemble
    val ewmaScore: Int = 0,
    val trend: String = "insufficient_data",
    val alarm: Boolean = false,
    val finalScore: Int = 0,
    val finalLabel: String = "safe"
)

