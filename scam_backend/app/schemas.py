from pydantic import BaseModel, Field
from typing import Any, Dict, List, Optional
from datetime import datetime

class AnalyzeRequest(BaseModel):
    text: str = Field(..., min_length=1)

class AnalyzeResponse(BaseModel):
    label: str
    is_scam: bool
    scam_type: str
    scam_score: int
    score: float
    scam_probability: float
    safe_probability: float
    suggestion: str

class ScenarioLine(BaseModel):
    speaker: str
    text: str
    delay_ms: int

class Scenario(BaseModel):
    id: int
    title: str
    category: str
    preview: str
    lines: List[ScenarioLine]

class ReportRequest(BaseModel):
    text: str
    predicted_label: Optional[str] = None
    user_note: Optional[str] = None

class ReportResponse(BaseModel):
    status: str
    message: str

# ── Ses tabanlı endpoint şemaları ─────────────────────────────────────────────

class TranscribeFileResponse(BaseModel):
    analysis_id: Optional[str] = None
    transcript: str
    label: str
    is_scam: bool
    scam_type: str
    scam_score: int
    score: float
    scam_probability: float
    safe_probability: float
    suggestion: str
    duration_seconds: Optional[float] = None

class TranscribeChunkRequest(BaseModel):
    audio_base64: str = Field(..., description="Base64 encode edilmiş WAV/PCM chunk")
    sample_rate:  int = Field(16000, description="Örnekleme hızı (Hz)")
    chunk_index:  int = Field(0, description="Chunk sıra numarası")
    session_id:   str = Field("default", description="Mikrofon oturum kimliği")

class TranscribeChunkResponse(BaseModel):
    chunk_index:      int
    transcript:       str
    # BERTurk
    label:            str   = "legit"
    is_scam:          bool  = False
    scam_type:        str   = ""
    scam_score:       int   = 0
    score:            float = 0.0
    scam_probability: float = 0.0
    safe_probability: float = 1.0
    suggestion:       str   = ""
    # EWMA
    ewma_score:       int   = 0
    trend:            str   = "insufficient_data"
    alarm:            bool  = False
    chunk_count:      int   = 0
    # Ensemble
    final_score:      int   = 0       # BERTurk + EWMA birleşik skor (0-100)
    final_label:      str   = "safe"  # safe | suspicious | dangerous

# ── MongoDB geçmiş ve istatistik şemaları ─────────────────────────────────────

class AnalysisRecord(BaseModel):
    id: str
    source: str
    transcript: str
    label: str
    is_scam: bool
    scam_type: str = ""
    scam_score: int = 0
    suggestion: str = ""
    score: float
    scam_probability: float
    safe_probability: float
    risk_percent: int
    risk_level: str
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