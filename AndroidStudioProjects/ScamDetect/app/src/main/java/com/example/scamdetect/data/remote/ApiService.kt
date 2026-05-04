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
    const val BASE_URL = "http://10.0.2.2:8000"

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)   // Whisper uzun sürebilir
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
}

object ApiService {

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
        chunkIndex: Int
    ): ChunkResult {
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

        val json = JSONObject().apply {
            put("audio_base64", base64Audio)
            put("sample_rate", sampleRate)
            put("chunk_index", chunkIndex)
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
                put("score", c.score)
                put("is_scam", c.isScam)
                put("scam_probability", c.scamProbability)
                put("safe_probability", c.safeProbability)
            }
            chunksArray.put(obj)
        }

        val overallTranscript = chunks.joinToString(" ") { it.transcript }
        val overallScamProb = chunks.maxOfOrNull { it.scamProbability } ?: 0.0f
        val overallSafeProb = 1.0f - overallScamProb

        val json = JSONObject().apply {
            put("chunks", chunksArray)
            put("overall_transcript", overallTranscript)
            put("overall_scam_probability", overallScamProb)
            put("overall_safe_probability", overallSafeProb)
            put("duration_seconds", durationSeconds)
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
        label           = json.optString("label", "safe"),
        score           = json.optDouble("score", 0.0).toFloat(),
        isScam          = json.optBoolean("is_scam", false),
        scamProbability = json.optDouble("scam_probability", 0.0).toFloat(),
        safeProbability = json.optDouble("safe_probability", 1.0).toFloat(),
        durationSeconds = if (json.isNull("duration_seconds")) null
                          else json.optDouble("duration_seconds").toFloat()
    )

    private fun parseChunkResult(json: JSONObject) = ChunkResult(
        chunkIndex      = json.optInt("chunk_index", 0),
        transcript      = json.optString("transcript", ""),
        label           = json.optString("label", "safe"),
        score           = json.optDouble("score", 0.0).toFloat(),
        isScam          = json.optBoolean("is_scam", false),
        scamProbability = json.optDouble("scam_probability", 0.0).toFloat(),
        safeProbability = json.optDouble("safe_probability", 1.0).toFloat()
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
        label           = json.optString("label", "safe"),
        score           = json.optDouble("score", 0.0).toFloat(),
        isScam          = json.optBoolean("is_scam", false),
        scamProbability = json.optDouble("scam_probability", 0.0).toFloat(),
        safeProbability = json.optDouble("safe_probability", 1.0).toFloat(),
        riskPercent     = json.optInt("risk_percent", 0),
        riskLevel       = json.optString("risk_level", "safe"),
        durationSeconds = if (json.isNull("duration_seconds")) null
                          else json.optDouble("duration_seconds").toFloat(),
        fileName        = json.optString("file_name").takeIf { it.isNotBlank() },
        createdAt       = json.optString("created_at", "")
    )
}
