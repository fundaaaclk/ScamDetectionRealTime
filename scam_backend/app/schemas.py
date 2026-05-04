from pydantic import BaseModel, Field
from typing import Any, Dict, List, Optional
from datetime import datetime

class AnalyzeRequest(BaseModel):
    text: str = Field(..., min_length=1)

class AnalyzeResponse(BaseModel):
    label: str
    score: float
    is_scam: bool
    scam_probability: float
    safe_probability: float

class Scenario(BaseModel):
    id: int
    title: str
    category: str
    message: str

class ReportRequest(BaseModel):
    text: str
    predicted_label: Optional[str] = None
    user_note: Optional[str] = None

class ReportResponse(BaseModel):
    status: str
    message: str

# ── Ses tabanlı endpoint şemaları ──────────────────────────────────────────

class TranscribeFileResponse(BaseModel):
    analysis_id: Optional[str] = None   # MongoDB'deki kayıt id'si
    transcript: str
    label: str
    score: float
    is_scam: bool
    scam_probability: float
    safe_probability: float
    duration_seconds: Optional[float] = None

class TranscribeChunkRequest(BaseModel):
    audio_base64: str = Field(..., description="Base64 encode edilmiş WAV/PCM chunk")
    sample_rate: int = Field(16000, description="Örnekleme hızı (Hz)")
    chunk_index: int = Field(0, description="Chunk sıra numarası")

class TranscribeChunkResponse(BaseModel):
    chunk_index: int
    transcript: str
    label: str
    score: float
    is_scam: bool
    scam_probability: float
    safe_probability: float

# ── MongoDB geçmiş ve istatistik şemaları ─────────────────────────────────

class AnalysisRecord(BaseModel):
    id: str
    source: str                         # "file" | "microphone" | "text"
    transcript: str
    label: str
    score: float
    is_scam: bool
    scam_probability: float
    safe_probability: float
    risk_percent: int
    risk_level: str                     # "safe" | "suspicious" | "dangerous"
    duration_seconds: Optional[float] = None
    file_name: Optional[str] = None
    chunks: List[Dict[str, Any]] = []
    created_at: datetime

class AnalysisListResponse(BaseModel):
    total: int
    analyses: List[AnalysisRecord]

class StatsResponse(BaseModel):
    total: int
    dangerous: int
    suspicious: int
    safe: int
    avg_risk: float
