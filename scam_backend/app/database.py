"""
database.py
───────────
MongoDB Atlas bağlantısı ve analiz geçmişi CRUD işlemleri.
Motor (async) kullanır — FastAPI ile tam uyumlu.
"""

import os
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from bson import ObjectId
from dotenv import load_dotenv
from motor.motor_asyncio import AsyncIOMotorClient

load_dotenv()

MONGODB_URI     = os.getenv("MONGODB_URI", "")
MONGODB_DB_NAME = os.getenv("MONGODB_DB_NAME", "scam_guard_db")
COLLECTION_NAME = "analyses"

# ── Client singleton ──────────────────────────────────────────────────────────

_client: Optional[AsyncIOMotorClient] = None


def get_client() -> AsyncIOMotorClient:
    global _client
    if _client is None:
        _client = AsyncIOMotorClient(MONGODB_URI)
    return _client


def get_collection():
    return get_client()[MONGODB_DB_NAME][COLLECTION_NAME]


# ── ObjectId yardımcısı ───────────────────────────────────────────────────────

def _serialize(doc: Dict) -> Dict:
    """MongoDB'nin ObjectId'sini string'e çevirir."""
    if doc and "_id" in doc:
        doc["id"] = str(doc.pop("_id"))
    return doc


# ── CRUD ──────────────────────────────────────────────────────────────────────

async def save_analysis(
    source: str,
    transcript: str,
    label: str,
    score: float,
    is_scam: bool,
    scam_probability: float,
    safe_probability: float,
    scam_type: str = "",
    scam_score: int = 0,
    suggestion: str = "",
    duration_seconds: Optional[float] = None,
    file_name: Optional[str] = None,
    chunks: Optional[List[Dict]] = None,
) -> str:
    """
    Analiz sonucunu MongoDB'ye kaydeder.
    Returns: oluşturulan belgenin string id'si
    """
    col = get_collection()

    # Risk yüzdesi (0-100 tamsayı)
    risk_percent = round(scam_probability * 100)
    risk_level = (
        "dangerous" if risk_percent >= 75
        else "suspicious" if risk_percent >= 50
        else "safe"
    )

    doc = {
        "source":           source,
        "transcript":       transcript,
        "label":            label,
        "score":            score,
        "is_scam":          is_scam,
        "scam_type":        scam_type,
        "scam_score":       scam_score,
        "suggestion":       suggestion,
        "scam_probability": scam_probability,
        "safe_probability": safe_probability,
        "risk_percent":     risk_percent,
        "risk_level":       risk_level,
        "duration_seconds": duration_seconds,
        "file_name":        file_name,
        "chunks":           chunks or [],
        "created_at":       datetime.now(timezone.utc),
    }

    result = await col.insert_one(doc)
    return str(result.inserted_id)


async def get_all_analyses(limit: int = 50) -> List[Dict]:
    """Son yapılan analizleri yeniden eskiye sıralı döndürür."""
    col = get_collection()
    cursor = col.find().sort("created_at", -1).limit(limit)
    docs = await cursor.to_list(length=limit)
    return [_serialize(d) for d in docs]


async def get_analysis_by_id(analysis_id: str) -> Optional[Dict]:
    """Tek bir analizi id ile getirir."""
    col = get_collection()
    try:
        doc = await col.find_one({"_id": ObjectId(analysis_id)})
    except Exception:
        return None
    return _serialize(doc) if doc else None


async def get_stats() -> Dict[str, Any]:
    """HomeScreen için özet istatistikler: toplam, tehlikeli, güvenli, ortalama risk."""
    col = get_collection()
    total     = await col.count_documents({})
    dangerous = await col.count_documents({"risk_level": "dangerous"})
    suspicious = await col.count_documents({"risk_level": "suspicious"})
    safe      = await col.count_documents({"risk_level": "safe"})

    # Ortalama risk (son 50 analiz)
    pipeline = [
        {"$sort": {"created_at": -1}},
        {"$limit": 50},
        {"$group": {"_id": None, "avg_risk": {"$avg": "$risk_percent"}}}
    ]
    avg_cursor = await col.aggregate(pipeline).to_list(length=1)
    avg_risk = round(avg_cursor[0]["avg_risk"], 1) if avg_cursor else 0.0

    return {
        "total":      total,
        "dangerous":  dangerous,
        "suspicious": suspicious,
        "safe":       safe,
        "avg_risk":   avg_risk,
    }
