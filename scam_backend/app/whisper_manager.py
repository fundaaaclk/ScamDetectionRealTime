"""
whisper_manager.py
──────────────────
faster-whisper tabanlı ses → metin dönüştürücü.

- transcribe_file(path)   : Dosya yolundan transcript üretir (mp3/wav/mp4).
- transcribe_bytes(bytes) : Ham WAV byte'larından transcript üretir.
"""

import struct
import tempfile
import logging
from pathlib import Path
from typing import Optional, Tuple

logger = logging.getLogger(__name__)

# Lazy import — yüklenmemişse hata anlaşılır olsun
try:
    from faster_whisper import WhisperModel
except ImportError as e:  # pragma: no cover
    raise ImportError(
        "faster-whisper kurulu değil. "
        "`pip install faster-whisper` komutunu çalıştır."
    ) from e

# ─────────────────────────────────────────────────────────────────────────────
# Sabitler
# ─────────────────────────────────────────────────────────────────────────────
WHISPER_MODEL_SIZE = "medium"
# Mac'te MPS henüz kararsız, cpu + int8 en stabil seçenek
COMPUTE_DEVICE = "cpu"
COMPUTE_TYPE = "int8"
LANGUAGE = "tr"          # Türkçe zorla; None yapılırsa otomatik algılar


class WhisperManager:
    """faster-whisper singleton wrapper."""

    def __init__(self) -> None:
        self._model: Optional[WhisperModel] = None

    # ── Yükleme ───────────────────────────────────────────────────────────────

    def load(self) -> None:
        """Startup'ta bir kez çağrılır. Modeli indirir / cache'den yükler."""
        logger.info(
            "Whisper '%s' modeli yükleniyor (%s / %s)…",
            WHISPER_MODEL_SIZE,
            COMPUTE_DEVICE,
            COMPUTE_TYPE,
        )
        self._model = WhisperModel(
            WHISPER_MODEL_SIZE,
            device=COMPUTE_DEVICE,
            compute_type=COMPUTE_TYPE,
        )
        logger.info("Whisper modeli hazır.")

    @property
    def is_loaded(self) -> bool:
        return self._model is not None

    # ── Dosya yolu ile transcribe ──────────────────────────────────────────────

    def transcribe_file(self, file_path: str) -> Tuple[str, Optional[float]]:
        """
        Ses / video dosyasını transcribe eder.

        Parameters
        ----------
        file_path : str  — mp3, wav, mp4 vb. dosya yolu

        Returns
        -------
        (transcript_text, duration_seconds)
        """
        self._ensure_loaded()
        segments, info = self._model.transcribe(
            file_path,
            language=LANGUAGE,
            beam_size=5,
            vad_filter=True,          # sessiz kısımları atla
            vad_parameters={"min_silence_duration_ms": 500},
        )
        text_parts = [seg.text.strip() for seg in segments]
        transcript = " ".join(text_parts).strip()
        duration = getattr(info, "duration", None)
        logger.info("Transcribe tamamlandı. Süre: %.1fs | Karakter: %d", duration or 0, len(transcript))
        return transcript, duration

    # ── Ham byte ile transcribe ────────────────────────────────────────────────

    def transcribe_bytes(
        self,
        audio_bytes: bytes,
        sample_rate: int = 16000,
    ) -> str:
        """
        Ham PCM ya da tam WAV byte dizisini transcribe eder.

        - Eğer bytes zaten WAV header içeriyorsa doğrudan yazar.
        - Değilse, 16-bit mono WAV header ekleyerek geçici dosya oluşturur.

        Parameters
        ----------
        audio_bytes : bytes  — WAV veya ham PCM
        sample_rate : int    — PCM örnekleme hızı (varsayılan 16000)

        Returns
        -------
        transcript_text : str
        """
        self._ensure_loaded()

        with tempfile.NamedTemporaryFile(suffix=".wav", delete=True) as tmp:
            if self._is_wav(audio_bytes):
                tmp.write(audio_bytes)
            else:
                tmp.write(self._build_wav_header(len(audio_bytes), sample_rate))
                tmp.write(audio_bytes)
            tmp.flush()

            segments, _ = self._model.transcribe(
                tmp.name,
                language=LANGUAGE,
                beam_size=5,
                vad_filter=True,
                vad_parameters={"min_silence_duration_ms": 300},
            )
            text_parts = [seg.text.strip() for seg in segments]

        transcript = " ".join(text_parts).strip()
        logger.debug("Chunk transcript: '%s'", transcript[:80])
        return transcript

    # ── Yardımcı metotlar ─────────────────────────────────────────────────────

    def _ensure_loaded(self) -> None:
        if self._model is None:
            raise RuntimeError(
                "Whisper modeli yüklenmedi. startup_event çalışmamış olabilir."
            )

    @staticmethod
    def _is_wav(data: bytes) -> bool:
        """RIFF header kontrolü."""
        return len(data) >= 4 and data[:4] == b"RIFF"

    @staticmethod
    def _build_wav_header(pcm_size: int, sample_rate: int) -> bytes:
        """16-bit mono PCM için minimal WAV header üretir."""
        num_channels = 1
        bits_per_sample = 16
        byte_rate = sample_rate * num_channels * bits_per_sample // 8
        block_align = num_channels * bits_per_sample // 8
        data_chunk_size = pcm_size
        riff_chunk_size = 36 + data_chunk_size

        header = struct.pack(
            "<4sI4s4sIHHIIHH4sI",
            b"RIFF",
            riff_chunk_size,
            b"WAVE",
            b"fmt ",
            16,               # PCM fmt chunk boyutu
            1,                # PCM format
            num_channels,
            sample_rate,
            byte_rate,
            block_align,
            bits_per_sample,
            b"data",
            data_chunk_size,
        )
        return header


# ── Singleton ─────────────────────────────────────────────────────────────────
whisper_manager = WhisperManager()
