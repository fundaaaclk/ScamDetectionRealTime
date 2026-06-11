import json
import shutil
import zipfile
from pathlib import Path
from typing import Any, Dict

import torch
from transformers import AutoModelForSequenceClassification, AutoTokenizer

BASE_DIR   = Path(__file__).resolve().parent.parent
MODEL_ROOT = BASE_DIR / "models"
MODEL_ZIP  = MODEL_ROOT / "berturk_scam_v6_final.zip"
MODEL_DIR  = MODEL_ROOT / "berturk_scam_v6_final"

LEGIT_LABEL = "legit"

SUGGESTIONS: Dict[str, str] = {
    "banka_kart_hesap": "Bankanızı yalnızca kartın arkasındaki resmi numaradan arayın. Şifre, CVV veya OTP bilgilerinizi asla kimseyle paylaşmayın.",
    "diger_scam":       "Bu mesaj dolandırıcılık belirtisi taşıyor. Herhangi bir ödeme yapmayın veya kişisel bilgi paylaşmayın.",
    "finans_kripto":    "Garanti kazanç vaat eden yatırım teklifleri her zaman dolandırıcılıktır. Tanımadığınız platformlara para göndermeyin.",
    "kargo_gumruk":     "Kargo firmasını resmi web sitesinden veya müşteri hizmetleri numarasından arayarak teyit edin. Link veya QR ile ödeme yapmayın.",
    "oltalama":         "Şüpheli linklere tıklamayın. Kişisel veya finansal bilgilerinizi bu tür formlara girmeyin.",
    "pazar_yeri_ilan":  "Tanımadığınız satıcılara peşin ödeme yapmayın. Platform dışı ödeme talep edenlere güvenmeyin.",
    "resmi_kurum":      "Resmi kurumlar telefon veya SMS ile şifre/kart bilgisi istemez. Kurumu doğrudan resmi numarasından arayın.",
    "sosyal_medya":     "Sosyal medyada tanımadığınız kişilerin para veya hediye tekliflerine inanmayın. Hesabı platform üzerinden şikayet edin.",
    "tuketici_iade":    "İade işlemleri için yalnızca resmi müşteri hizmetlerini kullanın. Link veya QR ile ödeme talep edilirse reddedin.",
    LEGIT_LABEL:        "",
}


class ScamModel:
    def __init__(self) -> None:
        self.tokenizer = None
        self.model     = None
        self.id2label: Dict[int, str] = {}
        self.legit_idx: int = -1
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    def _prepare_model_folder(self) -> None:
        if (MODEL_DIR / "model.safetensors").exists():
            return

        if not MODEL_ZIP.exists():
            raise FileNotFoundError(
                f"Model bulunamadı. {MODEL_ZIP.name} dosyasını scam_backend/models/ içine koy."
            )

        tmp_dir = MODEL_ROOT / "_tmp_extract"
        if tmp_dir.exists():
            shutil.rmtree(tmp_dir)

        with zipfile.ZipFile(MODEL_ZIP, "r") as zf:
            zf.extractall(tmp_dir)

        # Colab zip yapısı: content/berturk_scam_v6_final/
        candidates = [
            tmp_dir / "content" / "berturk_scam_v6_final",
            tmp_dir / "berturk_scam_v6_final",
            tmp_dir,
        ]
        extracted = next((p for p in candidates if (p / "model.safetensors").exists()), None)
        if extracted is None:
            raise FileNotFoundError("Zip içinde model.safetensors bulunamadı.")

        if MODEL_DIR.exists():
            shutil.rmtree(MODEL_DIR)
        shutil.move(str(extracted), str(MODEL_DIR))
        shutil.rmtree(tmp_dir, ignore_errors=True)

    def load(self) -> None:
        self._prepare_model_folder()

        label_map_path = MODEL_DIR / "label_map.json"
        if label_map_path.exists():
            with open(label_map_path, "r", encoding="utf-8") as f:
                raw = json.load(f)
            id2label_raw = raw.get("id2label", raw)
            self.id2label = {int(k): v for k, v in id2label_raw.items()}

        self.legit_idx = next(
            (k for k, v in self.id2label.items() if v == LEGIT_LABEL), -1
        )
        self.tokenizer = AutoTokenizer.from_pretrained(
            str(MODEL_DIR), local_files_only=True
        )
        self.model = AutoModelForSequenceClassification.from_pretrained(
            str(MODEL_DIR), local_files_only=True
        )
        self.model.to(self.device)
        self.model.eval()

    def predict(self, text: str) -> Dict[str, Any]:
        if self.model is None or self.tokenizer is None:
            raise RuntimeError("Model yüklenmedi.")

        inputs = self.tokenizer(
            text, return_tensors="pt",
            truncation=True, padding=True, max_length=512,
        )
        inputs = {k: v.to(self.device) for k, v in inputs.items()}

        with torch.no_grad():
            outputs       = self.model(**inputs)
            probabilities = torch.softmax(outputs.logits, dim=-1)[0]

        pred_id = int(torch.argmax(probabilities).item())
        label   = self.id2label.get(pred_id, str(pred_id))
        is_scam = label != LEGIT_LABEL

        safe_prob  = float(probabilities[self.legit_idx].item()) if self.legit_idx >= 0 else 0.0
        scam_prob  = round(1.0 - safe_prob, 6)
        score      = round(float(probabilities[pred_id].item()), 6)
        scam_score = round(scam_prob * 100)

        return {
            "label":            label,
            "is_scam":          is_scam,
            "scam_type":        label if is_scam else "",
            "scam_score":       scam_score,
            "score":            score,
            "scam_probability": scam_prob,
            "safe_probability": round(safe_prob, 6),
            "suggestion":       SUGGESTIONS.get(label, ""),
        }


scam_model = ScamModel()