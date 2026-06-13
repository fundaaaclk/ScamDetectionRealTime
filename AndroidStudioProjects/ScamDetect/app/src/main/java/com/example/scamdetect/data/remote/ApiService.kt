package com.example.scamdetect.data.remote

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.scamdetect.data.model.AnalysisRecord
import com.example.scamdetect.data.model.AnalysisStats
import com.example.scamdetect.data.model.ChunkResult
import com.example.scamdetect.data.model.TranscribeResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {
    // Emülatörde backend'e ulaşmak için 10.0.2.2 kullanılır.
    // Gerçek cihazda Mac'in yerel IP adresini gir (örn. "192.168.1.X").
    const val BASE_URL = "http://172.20.10.3:8000"

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)   // Whisper uzun sürebilir
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
}

object ApiService {

    // ── Metin analizi → /analyze ──────────────────────────────────────────────

    suspend fun analyzeText(text: String, chunkIndex: Int = 0, sessionId: String = "default"): ChunkResult {
        val json = JSONObject().apply {
            put("text", text)
            put("chunk_index", chunkIndex)
            put("session_id", sessionId)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/test-ewma")
            .post(body)
            .build()
        val response = ApiClient.http.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw RuntimeException("Boş yanıt")
        if (!response.isSuccessful) throw RuntimeException("Sunucu hatası ${response.code}: $responseBody")
        val result = JSONObject(responseBody)
        val scamScore = result.optInt("scam_score", 0)
        return ChunkResult(
            chunkIndex      = chunkIndex,
            transcript      = text,
            label           = result.optString("scam_type", "legit").ifBlank { "legit" },
            isScam          = result.optBoolean("is_scam", false),
            scamType        = result.optString("scam_type", ""),
            scamScore       = scamScore,
            score           = scamScore / 100f,
            scamProbability = scamScore / 100f,
            safeProbability = 1f - scamScore / 100f,
            suggestion      = result.optString("suggestion", ""),
            ewmaScore       = result.optInt("ewma_score", 0),
            trend           = result.optString("trend", "insufficient_data"),
            alarm           = result.optBoolean("alarm", false),
            finalScore      = result.optInt("ewma_score", 0),
            finalLabel      = when {
                result.optInt("ewma_score", 0) >= 70 -> "dangerous"
                result.optInt("ewma_score", 0) >= 40 -> "suspicious"
                else -> "safe"
            }
        )
    }

    // ── Ses dosyası → /transcribe-file ───────────────────────────────────────

    suspend fun transcribeFile(uri: Uri, context: Context): TranscribeResult {
        val contentResolver = context.contentResolver

        val mimeType = contentResolver.getType(uri) ?: "audio/mpeg"
        val ext = when {
            mimeType.contains("mp4")  -> "mp4"
            mimeType.contains("wav")  -> "wav"
            mimeType.contains("ogg")  -> "ogg"
            mimeType.contains("flac") -> "flac"
            else                      -> "mp3"
        }

        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Dosya açılamadı")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name     = "file",
                filename = "upload.$ext",
                body     = bytes.toRequestBody(mimeType.toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/transcribe-file")
            .post(requestBody)
            .build()

        val response = ApiClient.http.newCall(request).execute()
        val body = response.body?.string() ?: throw RuntimeException("Boş yanıt")

        if (!response.isSuccessful) throw RuntimeException("Sunucu hatası ${response.code}: $body")

        return parseTranscribeResult(JSONObject(body))
    }

    // ── Mikrofon chunk → /transcribe-chunk ───────────────────────────────────

    suspend fun transcribeChunk(
        audioBytes: ByteArray,
        sampleRate: Int,
        chunkIndex: Int,
        sessionId: String
    ): ChunkResult {
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

        val json = JSONObject().apply {
            put("audio_base64", base64Audio)
            put("sample_rate", sampleRate)
            put("chunk_index", chunkIndex)
            put("session_id", sessionId)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/transcribe-chunk")
            .post(body)
            .build()

        val response = ApiClient.http.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw RuntimeException("Boş yanıt")

        if (!response.isSuccessful) throw RuntimeException("Sunucu hatası ${response.code}: $responseBody")

        return parseChunkResult(JSONObject(responseBody))
    }

    // ── Mikrofon oturumu bitti → /microphone-session-end ───────────────────

    suspend fun endMicrophoneSession(
        chunks: List<ChunkResult>,
        durationSeconds: Float
    ): String {
        val chunksArray = org.json.JSONArray()
        chunks.forEach { c ->
            val obj = JSONObject().apply {
                put("chunk_index", c.chunkIndex)
                put("transcript", c.transcript)
                put("label", c.label)
                put("score", c.score.toDouble())
                put("is_scam", c.isScam)
                put("scam_probability", c.scamProbability.toDouble())
                put("safe_probability", c.safeProbability.toDouble())
            }
            chunksArray.put(obj)
        }

        val overallTranscript = chunks.joinToString(" ") { it.transcript }
        val overallScamProb = chunks.maxOfOrNull { it.scamProbability } ?: 0.0f
        val overallSafeProb = 1.0f - overallScamProb

        val json = JSONObject().apply {
            put("chunks", chunksArray)
            put("overall_transcript", overallTranscript)
            put("overall_scam_probability", overallScamProb.toDouble())
            put("overall_safe_probability", overallSafeProb.toDouble())
            put("duration_seconds", durationSeconds.toDouble())
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/microphone-session-end")
            .post(body)
            .build()

        val response = ApiClient.http.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw RuntimeException("Boş yanıt")

        if (!response.isSuccessful) throw RuntimeException("Sunucu hatası ${response.code}: $responseBody")

        val root = JSONObject(responseBody)
        return root.optString("analysis_id", "")
    }

    // ── İstatistikler → /analyses/stats ──────────────────────────────────────

    suspend fun getStats(): AnalysisStats {
        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/analyses/stats")
            .get()
            .build()

        val response = ApiClient.http.newCall(request).execute()
        val body = response.body?.string() ?: throw RuntimeException("Boş yanıt")

        if (!response.isSuccessful) throw RuntimeException("Sunucu hatası ${response.code}: $body")

        return parseStats(JSONObject(body))
    }

    // ── Geçmiş listesi → /analyses ────────────────────────────────────────────

    suspend fun getAnalyses(limit: Int = 50): List<AnalysisRecord> {
        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/analyses?limit=$limit")
            .get()
            .build()

        val response = ApiClient.http.newCall(request).execute()
        val body = response.body?.string() ?: throw RuntimeException("Boş yanıt")

        if (!response.isSuccessful) throw RuntimeException("Sunucu hatası ${response.code}: $body")

        val root = JSONObject(body)
        val arr = root.getJSONArray("analyses")
        return (0 until arr.length()).map { parseAnalysisRecord(arr.getJSONObject(it)) }
    }

    // ── Tek analiz detayı → /analyses/{id} ───────────────────────────────────

    suspend fun getAnalysisById(id: String): AnalysisRecord {
        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/analyses/$id")
            .get()
            .build()

        val response = ApiClient.http.newCall(request).execute()
        val body = response.body?.string() ?: throw RuntimeException("Boş yanıt")

        if (!response.isSuccessful) throw RuntimeException("Sunucu hatası ${response.code}: $body")

        return parseAnalysisRecord(JSONObject(body))
    }

    // ── JSON → Model ──────────────────────────────────────────────────────────

    private fun parseTranscribeResult(json: JSONObject) = TranscribeResult(
        analysisId      = json.optString("analysis_id").takeIf { it.isNotBlank() },
        transcript      = json.optString("transcript", ""),
        label           = json.optString("label", "legit"),
        isScam          = json.optBoolean("is_scam", false),
        scamType        = json.optString("scam_type", ""),
        scamScore       = json.optInt("scam_score", 0),
        score           = json.optDouble("score", 0.0).toFloat(),
        scamProbability = json.optDouble("scam_probability", 0.0).toFloat(),
        safeProbability = json.optDouble("safe_probability", 1.0).toFloat(),
        suggestion      = json.optString("suggestion", ""),
        durationSeconds = if (json.isNull("duration_seconds")) null
                          else json.optDouble("duration_seconds").toFloat()
    )

    private fun parseChunkResult(json: JSONObject) = ChunkResult(
        chunkIndex      = json.optInt("chunk_index", 0),
        transcript      = json.optString("transcript", ""),
        label           = json.optString("label", "legit"),
        isScam          = json.optBoolean("is_scam", false),
        scamType        = json.optString("scam_type", ""),
        scamScore       = json.optInt("scam_score", 0),
        score           = json.optDouble("score", 0.0).toFloat(),
        scamProbability = json.optDouble("scam_probability", 0.0).toFloat(),
        safeProbability = json.optDouble("safe_probability", 1.0).toFloat(),
        suggestion      = json.optString("suggestion", ""),
        ewmaScore       = json.optInt("ewma_score", 0),
        trend           = json.optString("trend", "insufficient_data"),
        alarm           = json.optBoolean("alarm", false),
        finalScore      = json.optInt("final_score", 0),
        finalLabel      = json.optString("final_label", "safe")
    )

    private fun parseStats(json: JSONObject) = AnalysisStats(
        total     = json.optInt("total", 0),
        dangerous = json.optInt("dangerous", 0),
        suspicious = json.optInt("suspicious", 0),
        safe      = json.optInt("safe", 0),
        avgRisk   = json.optDouble("avg_risk", 0.0).toFloat()
    )

    private fun parseAnalysisRecord(json: JSONObject) = AnalysisRecord(
        id              = json.optString("id", ""),
        source          = json.optString("source", "file"),
        transcript      = json.optString("transcript", ""),
        label           = json.optString("label", "legit"),
        isScam          = json.optBoolean("is_scam", false),
        scamType        = json.optString("scam_type", ""),
        scamScore       = json.optInt("scam_score", 0),
        suggestion      = json.optString("suggestion", ""),
        score           = json.optDouble("score", 0.0).toFloat(),
        scamProbability = json.optDouble("scam_probability", 0.0).toFloat(),
        safeProbability = json.optDouble("safe_probability", 1.0).toFloat(),
        riskPercent     = json.optInt("risk_percent", 0),
        riskLevel       = json.optString("risk_level", "safe"),
        durationSeconds = if (json.isNull("duration_seconds")) null
                          else json.optDouble("duration_seconds").toFloat(),
        fileName        = if (json.isNull("file_name")) null else json.optString("file_name").takeIf { it.isNotBlank() },
        createdAt       = json.optString("created_at", "")
    )
}
