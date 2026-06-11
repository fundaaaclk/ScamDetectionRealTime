"""
ewma_manager.py
───────────────
Session bazlı EWMA hesaplayıcı + BERTurk ensemble final_score.
"""

import threading
from dataclasses import dataclass, field
from typing import Dict, List, Optional

# ─────────────────────────────────────────────────────────────────────────────
EWMA_BETA        = 0.6
ALARM_THRESHOLD  = 50
TREND_WINDOW     = 3
MIN_CHUNKS       = 2

# Ensemble
BERT_WEIGHT          = 0.65
EWMA_WEIGHT          = 0.35
DANGEROUS_THRESHOLD  = 70
SUSPICIOUS_THRESHOLD = 40


@dataclass
class SessionState:
    session_id:   str
    scores:       List[float] = field(default_factory=list)
    ewma_values:  List[float] = field(default_factory=list)
    ewma_current: float = 0.0
    alarm:        bool  = False


class EWMAManager:

    def __init__(self) -> None:
        self._sessions: Dict[str, SessionState] = {}
        self._lock = threading.Lock()

    def add_chunk(self, session_id: str, chunk_index: int, scam_score: int) -> dict:
        with self._lock:
            if session_id not in self._sessions:
                self._sessions[session_id] = SessionState(session_id=session_id)

            state = self._sessions[session_id]
            score_normalized = scam_score / 100.0

            state.scores.append(score_normalized)

            if len(state.ewma_values) == 0:
                ewma = score_normalized
            else:
                ewma = EWMA_BETA * state.ewma_current + (1 - EWMA_BETA) * score_normalized

            state.ewma_current = ewma
            state.ewma_values.append(ewma)

            ewma_score_100 = round(ewma * 100)
            if ewma_score_100 >= ALARM_THRESHOLD:
                state.alarm = True

            trend = self._calculate_trend(state.ewma_values)

            # Ensemble final score
            final_score = round(BERT_WEIGHT * scam_score + EWMA_WEIGHT * ewma_score_100)
            if final_score >= DANGEROUS_THRESHOLD:
                final_label = "dangerous"
            elif final_score >= SUSPICIOUS_THRESHOLD:
                final_label = "suspicious"
            else:
                final_label = "safe"

            return {
                "ewma_score":   ewma_score_100,
                "trend":        trend,
                "alarm":        state.alarm,
                "chunk_count":  len(state.scores),
                "final_score":  final_score,
                "final_label":  final_label,
                "raw_scores":   [round(s * 100) for s in state.scores],
                "ewma_values":  [round(v * 100) for v in state.ewma_values],
            }

    def _calculate_trend(self, ewma_values: List[float]) -> str:
        if len(ewma_values) < MIN_CHUNKS:
            return "insufficient_data"
        recent = ewma_values[-TREND_WINDOW:]
        if len(recent) < 2:
            return "stable"
        delta = recent[-1] - recent[0]
        if delta > 0.10:
            return "rising"
        elif delta < -0.10:
            return "falling"
        else:
            return "stable"

    def get_session(self, session_id: str) -> Optional[SessionState]:
        with self._lock:
            return self._sessions.get(session_id)

    def get_summary(self, session_id: str) -> dict:
        with self._lock:
            state = self._sessions.get(session_id)
            if not state or not state.scores:
                return {
                    "session_id":      session_id,
                    "chunk_count":     0,
                    "max_ewma":        0,
                    "final_ewma":      0,
                    "alarm_triggered": False,
                    "trend":           "insufficient_data",
                    "peak_chunk":      -1,
                }
            ewma_100   = [round(v * 100) for v in state.ewma_values]
            max_ewma   = max(ewma_100)
            peak_chunk = ewma_100.index(max_ewma)
            return {
                "session_id":      session_id,
                "chunk_count":     len(state.scores),
                "max_ewma":        max_ewma,
                "final_ewma":      ewma_100[-1],
                "alarm_triggered": state.alarm,
                "trend":           self._calculate_trend(state.ewma_values),
                "peak_chunk":      peak_chunk,
                "ewma_series":     ewma_100,
                "score_series":    [round(s * 100) for s in state.scores],
            }

    def clear_session(self, session_id: str) -> None:
        with self._lock:
            self._sessions.pop(session_id, None)

    def clear_all(self) -> None:
        with self._lock:
            self._sessions.clear()


ewma_manager = EWMAManager()