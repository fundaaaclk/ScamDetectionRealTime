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
from app.ewma_manager import ewma_manager
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

app = FastAPI(title="Scam Guard AI Backend", version="3.0.0")

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
    return {"message": "Scam Guard AI Backend v3.0 çalışıyor"}

@app.get("/health")
def health():
    return {
        "status":         "ok",
        "model_loaded":   scam_model.model is not None,
        "whisper_loaded": whisper_manager.is_loaded,
    }

# ── Metin analizi ─────────────────────────────────────────────────────────────

@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze(request: AnalyzeRequest):
    try:
        prediction = scam_model.predict(request.text)
        await save_analysis(
            source="text",
            transcript=request.text,
            **prediction,
        )
        return prediction
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))

# ── EWMA Test endpoint'i (ses olmadan metin ile test) ─────────────────────────

class EWMATestRequest(BaseModel):
    text: str
    chunk_index: int = 0
    session_id: str = "test"

class EWMAResetRequest(BaseModel):
    session_id: str = "test"

@app.post("/test-ewma")
async def test_ewma(request: EWMATestRequest):
    """
    Ses olmadan metin ile EWMA test endpoint'i.
    Konuşma simülasyonu için kullanılır.
    """
    prediction = scam_model.predict(request.text)
    ewma_result = ewma_manager.add_chunk(
        session_id=request.session_id,
        chunk_index=request.chunk_index,
        scam_score=prediction["scam_score"],
    )
    return {
        "text":        request.text,
        "scam_score":  prediction["scam_score"],
        "scam_type":   prediction["scam_type"],
        "is_scam":     prediction["is_scam"],
        "ewma_score":  ewma_result["ewma_score"],
        "trend":       ewma_result["trend"],
        "alarm":       ewma_result["alarm"],
        "chunk_count": ewma_result["chunk_count"],
        "ewma_series": ewma_result["ewma_values"],
        "score_series":ewma_result["raw_scores"],
    }

@app.post("/test-ewma/reset")
async def test_ewma_reset(request: EWMAResetRequest):
    """EWMA session'ını sıfırla."""
    ewma_manager.clear_session(request.session_id)
    return {"status": "ok", "session_id": request.session_id}

@app.get("/test-ewma/summary/{session_id}")
async def test_ewma_summary(session_id: str):
    """Session özeti."""
    return ewma_manager.get_summary(session_id)

# ── Senaryo listesi ───────────────────────────────────────────────────────────

@app.get("/scenarios", response_model=list[Scenario])
def get_scenarios():
    return [
        {
            "id": 1,
            "title": "Banka Kartı Donduruldu",
            "category": "banka_kart_hesap",
            "preview": "Yapı Kredi güvenlik birimi arıyor — hesabınızda şüpheli işlem var.",
            "lines": [
                {"speaker": "caller", "text": "Merhaba, Yapı Kredi Bankası güvenlik biriminden arıyorum.", "delay_ms": 0},
                {"speaker": "caller", "text": "Hesabınızda şüpheli bir işlem tespit ettik ve kartınızı geçici olarak dondurduk.", "delay_ms": 3500},
                {"speaker": "caller", "text": "Hesabınızı yeniden aktif etmek için kimliğinizi doğrulamamız gerekiyor.", "delay_ms": 3500},
                {"speaker": "caller", "text": "Lütfen kart şifrenizi ve son 3 hane CVV numaranızı bana söyler misiniz?", "delay_ms": 3500},
                {"speaker": "caller", "text": "İşlemi hızlıca tamamlamazsak hesabınız 24 saat bloke kalacak.", "delay_ms": 4000},
                {"speaker": "caller", "text": "Ayrıca size gelen SMS doğrulama kodunu da paylaşmanız gerekiyor.", "delay_ms": 3500},
            ],
        },
        {
            "id": 2,
            "title": "Kargo Gümrük Ödemesi",
            "category": "kargo_gumruk",
            "preview": "Uluslararası kargonuz gümrükte bekliyor, ödeme yapılması gerekiyor.",
            "lines": [
                {"speaker": "caller", "text": "İyi günler, PTT kargo müşteri hizmetlerinden arıyorum.", "delay_ms": 0},
                {"speaker": "caller", "text": "Yurt dışından size gelen bir paketiniz gümrükte bekliyor.", "delay_ms": 3500},
                {"speaker": "caller", "text": "Paketi serbest bırakmak için 187 TL gümrük vergisi ödemeniz gerekiyor.", "delay_ms": 3500},
                {"speaker": "caller", "text": "Ödemeyi şimdi telefonda kart bilgilerinizle halledebiliriz.", "delay_ms": 3500},
                {"speaker": "caller", "text": "Bugün ödeme yapmazsanız paket iade edilecek ve tekrar gönderim ücreti ödersiniz.", "delay_ms": 4000},
            ],
        },
        {
            "id": 3,
            "title": "Kripto Yatırım Teklifi",
            "category": "finans_kripto",
            "preview": "Günde %30 garantili kripto kazancı için acil fırsat sunuluyor.",
            "lines": [
                {"speaker": "caller", "text": "Merhaba, sizi bir yatırım fırsatı için arıyorum.", "delay_ms": 0},
                {"speaker": "caller", "text": "Platformumuz yapay zeka ile kripto ticareti yapıyor ve günlük %30 getiri garantisi sunuyoruz.", "delay_ms": 3500},
                {"speaker": "caller", "text": "Şu an sadece 12 kontenjanımız kaldı, bugün kaydolursanız ilk ay komisyon almıyoruz.", "delay_ms": 4000},
                {"speaker": "caller", "text": "Minimum 500 dolar ile başlayabilirsiniz, USDT veya banka transferi kabul ediyoruz.", "delay_ms": 3500},
                {"speaker": "caller", "text": "Hemen IBAN'ınızı veya kripto cüzdan adresinizi verir misiniz?", "delay_ms": 3500},
            ],
        },
        {
            "id": 4,
            "title": "SGK Prim Borç Uyarısı",
            "category": "resmi_kurum",
            "preview": "SGK müdürlüğünden aranıyorsunuz — prim borcu nedeniyle dosyanız kapanacak.",
            "lines": [
                {"speaker": "caller", "text": "İyi günler, Sosyal Güvenlik Kurumu İstanbul müdürlüğünden arıyorum.", "delay_ms": 0},
                {"speaker": "caller", "text": "Kayıtlarımıza göre 2023 yılına ait 3.840 TL prim borcunuz bulunmaktadır.", "delay_ms": 3500},
                {"speaker": "caller", "text": "Bu borç bugün ödenmediği takdirde dosyanız kapatılacak ve sağlık güvenceniz askıya alınacak.", "delay_ms": 4000},
                {"speaker": "caller", "text": "Ödemeyi şimdi kredi kartıyla telefonda yapabilirsiniz, size özel indirimli ödeme planı sunabiliriz.", "delay_ms": 4000},
                {"speaker": "caller", "text": "TC kimlik numaranızı ve kart bilgilerinizi alabilir miyim?", "delay_ms": 3500},
            ],
        },
    ]

# ── Rapor ─────────────────────────────────────────────────────────────────────

@app.post("/reports", response_model=ReportResponse)
def create_report(request: ReportRequest):
    return {"status": "success", "message": "Rapor alındı."}

# ── Ses dosyası yükle → transcribe → analiz → kaydet ─────────────────────────

ALLOWED_AUDIO_EXTENSIONS = {".mp3", ".wav", ".mp4", ".m4a", ".ogg", ".flac", ".webm"}


# ── EWMA Test endpoint'i (ses olmadan metin ile test) ─────────────────────────

class EWMATestRequest(BaseModel):
    text: str
    chunk_index: int = 0
    session_id: str = "test"

class EWMAResetRequest(BaseModel):
    session_id: str = "test"

@app.post("/test-ewma")
async def test_ewma(request: EWMATestRequest):
    """Ses olmadan metin ile EWMA test endpoint'i."""
    prediction = scam_model.predict(request.text)
    ewma_result = ewma_manager.add_chunk(
        session_id=request.session_id,
        chunk_index=request.chunk_index,
        scam_score=prediction["scam_score"],
    )
    return {
        "text":        request.text,
        "scam_score":  prediction["scam_score"],
        "scam_type":   prediction["scam_type"],
        "is_scam":     prediction["is_scam"],
        "ewma_score":  ewma_result["ewma_score"],
        "trend":       ewma_result["trend"],
        "alarm":       ewma_result["alarm"],
        "chunk_count": ewma_result["chunk_count"],
        "final_score": ewma_result["final_score"],
        "final_label": ewma_result["final_label"],
        "ewma_series": ewma_result["ewma_values"],
        "score_series":ewma_result["raw_scores"],
    }

@app.post("/test-ewma/reset")
async def test_ewma_reset(request: EWMAResetRequest):
    ewma_manager.clear_session(request.session_id)
    return {"status": "ok", "session_id": request.session_id}

@app.get("/test-ewma/summary/{session_id}")
async def test_ewma_summary(session_id: str):
    return ewma_manager.get_summary(session_id)

@app.post("/transcribe-file", response_model=TranscribeFileResponse)
async def transcribe_file(file: UploadFile = File(...)):
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
                source="file", transcript="", label="safe",
                score=0.0, is_scam=False,
                scam_probability=0.0, safe_probability=1.0,
                duration_seconds=duration, file_name=file.filename,
            )
            return TranscribeFileResponse(
                analysis_id=analysis_id, transcript="", label="safe",
                score=0.0, is_scam=False, scam_type="",
                scam_score=0, scam_probability=0.0, safe_probability=1.0,
                suggestion="", duration_seconds=duration,
            )
        prediction = scam_model.predict(transcript)
        analysis_id = await save_analysis(
            source="file", transcript=transcript,
            duration_seconds=duration, file_name=file.filename,
            **prediction,
        )
        return TranscribeFileResponse(
            analysis_id=analysis_id, transcript=transcript,
            duration_seconds=duration, **prediction,
        )
    except Exception as exc:
        logger.exception("transcribe-file hatası")
        raise HTTPException(status_code=500, detail=str(exc))
    finally:
        try:
            os.unlink(tmp_path)
        except OSError:
            pass

# ── Mikrofon chunk → transcribe → analiz → EWMA ──────────────────────────────

@app.post("/transcribe-chunk", response_model=TranscribeChunkResponse)
async def transcribe_chunk(request: TranscribeChunkRequest):
    """
    Android mikrofon chunk'ı transcribe et, BERTurk ile analiz et,
    EWMA trend hesapla.
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
            ewma_result = ewma_manager.add_chunk(
                session_id=request.session_id,
                chunk_index=request.chunk_index,
                scam_score=0,
            )
            return TranscribeChunkResponse(
                chunk_index=request.chunk_index,
                transcript="",
                ewma_score=ewma_result["ewma_score"],
                trend=ewma_result["trend"],
                alarm=ewma_result["alarm"],
                chunk_count=ewma_result["chunk_count"],
                final_score=ewma_result["final_score"],
                final_label=ewma_result["final_label"],
            )

        prediction = scam_model.predict(transcript)
        ewma_result = ewma_manager.add_chunk(
            session_id=request.session_id,
            chunk_index=request.chunk_index,
            scam_score=prediction["scam_score"],
        )

        return TranscribeChunkResponse(
            chunk_index=request.chunk_index,
            transcript=transcript,
            ewma_score=ewma_result["ewma_score"],
            trend=ewma_result["trend"],
            alarm=ewma_result["alarm"],
            chunk_count=ewma_result["chunk_count"],
            final_score=ewma_result["final_score"],
            final_label=ewma_result["final_label"],
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
    session_id: str = "default"

class MicrophoneSessionResponse(BaseModel):
    analysis_id: str
    risk_percent: int
    risk_level: str
    ewma_summary: dict = {}

@app.post("/microphone-session-end", response_model=MicrophoneSessionResponse)
async def microphone_session_end(request: MicrophoneSessionRequest):
    try:
        ewma_summary = ewma_manager.get_summary(request.session_id)
        scam_prob = request.overall_scam_probability
        safe_prob = request.overall_safe_probability
        label     = "scam" if scam_prob >= 0.5 else "safe"
        is_scam   = scam_prob >= 0.5

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
            "dangerous"  if risk_percent >= 75 else
            "suspicious" if risk_percent >= 50 else
            "safe"
        )

        ewma_manager.clear_session(request.session_id)

        return MicrophoneSessionResponse(
            analysis_id=analysis_id,
            risk_percent=risk_percent,
            risk_level=risk_level,
            ewma_summary=ewma_summary,
        )
    except Exception as exc:
        logger.exception("microphone-session-end hatası")
        raise HTTPException(status_code=500, detail=str(exc))

# ── MongoDB Geçmiş Endpoint'leri ──────────────────────────────────────────────

@app.get("/analyses/stats", response_model=StatsResponse)
async def analyses_stats():
    try:
        return await get_stats()
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))

@app.get("/analyses", response_model=AnalysisListResponse)
async def analyses_list(limit: int = 50):
    try:
        docs = await get_all_analyses(limit)
        return AnalysisListResponse(total=len(docs), analyses=docs)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))

@app.get("/analyses/{analysis_id}", response_model=AnalysisRecord)
async def analyses_detail(analysis_id: str):
    doc = await get_analysis_by_id(analysis_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Analiz bulunamadı.")
    return doc