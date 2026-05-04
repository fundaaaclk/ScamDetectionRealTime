package com.example.scamdetect.data.model

data class TranscribeResult(
    val analysisId: String?,        // MongoDB'deki kayıt id'si
    val transcript: String,
    val label: String,
    val score: Float,
    val isScam: Boolean,
    val scamProbability: Float,
    val safeProbability: Float,
    val durationSeconds: Float?
)

data class ChunkResult(
    val chunkIndex: Int,
    val transcript: String,
    val label: String,
    val score: Float,
    val isScam: Boolean,
    val scamProbability: Float,
    val safeProbability: Float
)

