import base64
import logging
import os
import tempfile

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from app.database import get_stats, get_all_analyses, get_analysis_by_id, save_analysis
from app.model_manager import scam_model
from app.whisper_manager import whisper_manager
from app.schemas import (
    AnalyzeRequest,
    AnalyzeResponse,
    AnalysisListResponse,
    AnalysisRecord,
    ReportRequest,
    ReportResponse,
    Scenario,
    StatsResponse,
    TranscribeChunkRequest,
    TranscribeChunkResponse,
    TranscribeFileResponse,
)

logger = logging.getLogger(__name__)

app = FastAPI(title="Scam Guard AI Backend", version="2.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Başlangıç ─────────────────────────────────────────────────────────────────

@app.on_event("startup")
def startup_event():
    scam_model.load()
    whisper_manager.load()

# ── Temel ─────────────────────────────────────────────────────────────────────

@app.get("/")
def root():
    return {"message": "Scam Guard AI Backend v2.0 çalışıyor"}

@app.get("/health")
def health():
    return {
        "status": "ok",
        "model_loaded": scam_model.model is not None,
        "whisper_loaded": whisper_manager.is_loaded,
    }

# ── Metin analizi ─────────────────────────────────────────────────────────────

@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze(request: AnalyzeRequest):
    try:
        prediction = scam_model.predict(request.text)
        # Metin analizini de kaydet
        await save_analysis(
            source="text",
            transcript=request.text,
            **prediction,
        )
        return prediction
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))

# ── Senaryo listesi ───────────────────────────────────────────────────────────

@app.get("/scenarios", response_model=list[Scenario])
def get_scenarios():
    return [
        {"id": 1, "title": "Banka Şifre Dolandırıcılığı", "category": "Banka",  "message": "Bankanızdan arıyoruz, güvenlik için şifrenizi söylemeniz gerekiyor."},
        {"id": 2, "title": "Kargo Adres Güncelleme",       "category": "Kargo",  "message": "Kargonuz teslim edilemedi. Adresinizi güncellemek için linke tıklayın."},
        {"id": 3, "title": "Kart İşlem İptali",             "category": "Finans", "message": "Kartınızdan 45.000 TL çekildi. İptal etmek için hemen linke tıklayın."},
        {"id": 4, "title": "Güvenli Günlük Mesaj",          "category": "Normal", "message": "Yarın saat 3'te toplantımız var, uygun musun?"},
    ]

# ── Rapor (legacy) ────────────────────────────────────────────────────────────

@app.post("/reports", response_model=ReportResponse)
def create_report(request: ReportRequest):
    return {"status": "success", "message": "Rapor alındı."}

# ── Ses dosyası yükle → transcribe → analiz → kaydet ─────────────────────────

ALLOWED_AUDIO_EXTENSIONS = {".mp3", ".wav", ".mp4", ".m4a", ".ogg", ".flac", ".webm"}

@app.post("/transcribe-file", response_model=TranscribeFileResponse)
async def transcribe_file(file: UploadFile = File(...)):
    """
    Ses/video dosyası yükle, Whisper ile transcript al, model ile analiz et,
    MongoDB'ye kaydet.
    """
    ext = os.path.splitext(file.filename or "")[1].lower()
    if ext not in ALLOWED_AUDIO_EXTENSIONS:
        raise HTTPException(
            status_code=415,
            detail=f"Desteklenmeyen dosya formatı '{ext}'. Kabul edilenler: {sorted(ALLOWED_AUDIO_EXTENSIONS)}",
        )

    try:
        contents = await file.read()
        with tempfile.NamedTemporaryFile(suffix=ext, delete=False) as tmp:
            tmp.write(contents)
            tmp_path = tmp.name
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Dosya okunamadı: {exc}")

    try:
        transcript, duration = whisper_manager.transcribe_file(tmp_path)

        if not transcript:
            analysis_id = await save_analysis(
                source="file",
                transcript="",
                label="safe",
                score=0.0,
                is_scam=False,
                scam_probability=0.0,
                safe_probability=1.0,
                duration_seconds=duration,
                file_name=file.filename,
            )
            return TranscribeFileResponse(
                analysis_id=analysis_id,
                transcript="",
                label="safe",
                score=0.0,
                is_scam=False,
                scam_probability=0.0,
                safe_probability=1.0,
                duration_seconds=duration,
            )

        prediction = scam_model.predict(transcript)

        # MongoDB'ye kaydet
        analysis_id = await save_analysis(
            source="file",
            transcript=transcript,
            duration_seconds=duration,
            file_name=file.filename,
            **prediction,
        )

        return TranscribeFileResponse(
            analysis_id=analysis_id,
            transcript=transcript,
            duration_seconds=duration,
            **prediction,
        )

    except Exception as exc:
        logger.exception("transcribe-file hatası")
        raise HTTPException(status_code=500, detail=str(exc))

    finally:
        try:
            os.unlink(tmp_path)
        except OSError:
            pass

# ── Mikrofon chunk → transcribe → analiz ─────────────────────────────────────
# Not: Chunk'lar ayrı ayrı kaydedilmez; mikrofon oturumu bitince
# /microphone-session-end endpoint'i ile toplu kaydedilir.

@app.post("/transcribe-chunk", response_model=TranscribeChunkResponse)
async def transcribe_chunk(request: TranscribeChunkRequest):
    """
    Android mikrofon chunk'ı (base64 WAV/PCM) transcribe et ve analiz et.
    """
    try:
        audio_bytes = base64.b64decode(request.audio_base64)
    except Exception:
        raise HTTPException(status_code=400, detail="Geçersiz base64 verisi.")

    if len(audio_bytes) == 0:
        raise HTTPException(status_code=400, detail="Ses verisi boş.")

    try:
        transcript = whisper_manager.transcribe_bytes(
            audio_bytes, sample_rate=request.sample_rate
        )

        if not transcript:
            return TranscribeChunkResponse(
                chunk_index=request.chunk_index,
                transcript="",
                label="safe",
                score=0.0,
                is_scam=False,
                scam_probability=0.0,
                safe_probability=1.0,
            )

        prediction = scam_model.predict(transcript)
        return TranscribeChunkResponse(
            chunk_index=request.chunk_index,
            transcript=transcript,
            **prediction,
        )

    except Exception as exc:
        logger.exception("transcribe-chunk hatası (chunk_index=%d)", request.chunk_index)
        raise HTTPException(status_code=500, detail=str(exc))


# ── Mikrofon oturumu bitti → toplu kaydet ─────────────────────────────────────

class MicrophoneSessionRequest(BaseModel):
    chunks: list[dict]
    overall_transcript: str = ""
    overall_scam_probability: float = 0.0
    overall_safe_probability: float = 1.0
    duration_seconds: float = 0.0

class MicrophoneSessionResponse(BaseModel):
    analysis_id: str
    risk_percent: int
    risk_level: str

@app.post("/microphone-session-end", response_model=MicrophoneSessionResponse)
async def microphone_session_end(request: MicrophoneSessionRequest):
    """
    Mikrofon kaydı bittiğinde tüm chunk'ları tek belge olarak MongoDB'ye kaydeder.
    """
    try:
        scam_prob = request.overall_scam_probability
        safe_prob = request.overall_safe_probability
        label = "scam" if scam_prob >= 0.5 else "safe"
        is_scam = scam_prob >= 0.5

        analysis_id = await save_analysis(
            source="microphone",
            transcript=request.overall_transcript,
            label=label,
            score=scam_prob,
            is_scam=is_scam,
            scam_probability=scam_prob,
            safe_probability=safe_prob,
            duration_seconds=request.duration_seconds,
            chunks=request.chunks,
        )

        risk_percent = round(scam_prob * 100)
        risk_level = (
            "dangerous" if risk_percent >= 75
            else "suspicious" if risk_percent >= 50
            else "safe"
        )

        return MicrophoneSessionResponse(
            analysis_id=analysis_id,
            risk_percent=risk_percent,
            risk_level=risk_level,
        )
    except Exception as exc:
        logger.exception("microphone-session-end hatası")
        raise HTTPException(status_code=500, detail=str(exc))

# ── MongoDB Geçmiş Endpoint'leri ──────────────────────────────────────────────

@app.get("/analyses/stats", response_model=StatsResponse)
async def analyses_stats():
    """HomeScreen için özet istatistikler."""
    try:
        return await get_stats()
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@app.get("/analyses", response_model=AnalysisListResponse)
async def analyses_list(limit: int = 50):
    """ReportsListScreen için geçmiş analiz listesi."""
    try:
        docs = await get_all_analyses(limit=limit)
        return AnalysisListResponse(total=len(docs), analyses=docs)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@app.get("/analyses/{analysis_id}", response_model=AnalysisRecord)
async def analyses_detail(analysis_id: str):
    """Tek bir analizin detayı (ReportScreen için)."""
    doc = await get_analysis_by_id(analysis_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Analiz bulunamadı.")
    return doc
