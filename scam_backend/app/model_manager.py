import json
import shutil
import zipfile
from pathlib import Path
from typing import Any, Dict

import torch
from transformers import AutoModelForSequenceClassification, AutoTokenizer

BASE_DIR = Path(__file__).resolve().parent.parent
MODEL_ROOT = BASE_DIR / "models"
MODEL_DIR = MODEL_ROOT / "best_model_v2"
MODEL_ZIP = MODEL_ROOT / "best_model_v2.zip"

class ScamModel:
    def __init__(self) -> None:
        self.tokenizer = None
        self.model = None
        self.id2label = {0: "safe", 1: "scam"}
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    def _prepare_model_folder(self) -> None:
        if (MODEL_DIR / "model.safetensors").exists():
            return

        if not MODEL_ZIP.exists() and not (MODEL_DIR / "model.safetensors").exists():
            import logging
            logger = logging.getLogger(__name__)
            logger.info("Model klasörü bulunamadı. Google Drive'dan indiriliyor (Bu işlem birkaç dakika sürebilir)...")
            try:
                import gdown
                MODEL_ROOT.mkdir(parents=True, exist_ok=True)
                # Kendi eğittiğimiz BERT modelini (zip formatında) Drive'dan indiriyoruz
                gdown.download(id="1XFE5K8pMtarOLjPLxaxn-o7gAvTKciAt", output=str(MODEL_ZIP), quiet=False)
            except ImportError:
                logger.warning("gdown kütüphanesi eksik. Model indirilemiyor. 'pip install gdown' çalıştırın.")
            except Exception as e:
                logger.error(f"Google Drive'dan model indirilemedi: {e}")

        if (MODEL_DIR / "model.safetensors").exists():
            return

        if not MODEL_ZIP.exists():
            raise FileNotFoundError(
                "Model bulunamadı. best_model_v2.zip dosyasını scam_backend/models/ içine koy."
            )
        tmp_dir = MODEL_ROOT / "_tmp_extract"
        if tmp_dir.exists():
            shutil.rmtree(tmp_dir)
        with zipfile.ZipFile(MODEL_ZIP, "r") as zip_ref:
            zip_ref.extractall(tmp_dir)
        extracted = tmp_dir / "content" / "best_model_v2"
        if not extracted.exists():
            raise FileNotFoundError("Zip içinde content/best_model_v2 klasörü bulunamadı.")
        if MODEL_DIR.exists():
            shutil.rmtree(MODEL_DIR)
        shutil.move(str(extracted), str(MODEL_DIR))
        shutil.rmtree(tmp_dir, ignore_errors=True)

    def load(self) -> None:
        self._prepare_model_folder()
        label_map_path = MODEL_DIR / "label_map.json"
        if label_map_path.exists():
            with open(label_map_path, "r", encoding="utf-8") as f:
                self.id2label = {int(k): v for k, v in json.load(f).items()}
        self.tokenizer = AutoTokenizer.from_pretrained(str(MODEL_DIR), local_files_only=True)
        self.model = AutoModelForSequenceClassification.from_pretrained(str(MODEL_DIR), local_files_only=True)
        self.model.to(self.device)
        self.model.eval()

    def predict(self, text: str) -> Dict[str, Any]:
        if self.model is None or self.tokenizer is None:
            raise RuntimeError("Model yüklenmedi.")
        inputs = self.tokenizer(text, return_tensors="pt", truncation=True, padding=True, max_length=512)
        inputs = {key: value.to(self.device) for key, value in inputs.items()}
        with torch.no_grad():
            outputs = self.model(**inputs)
            probabilities = torch.softmax(outputs.logits, dim=-1)[0]
        pred_id = int(torch.argmax(probabilities).item())
        label = self.id2label.get(pred_id, str(pred_id))
        safe_probability = float(probabilities[0].item())
        scam_probability = float(probabilities[1].item())
        score = scam_probability if label == "scam" else safe_probability
        return {
            "label": label,
            "score": round(score, 6),
            "is_scam": label == "scam",
            "scam_probability": round(scam_probability, 6),
            "safe_probability": round(safe_probability, 6),
        }

scam_model = ScamModel()
