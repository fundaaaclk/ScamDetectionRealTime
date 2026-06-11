# SE Shield — Scam Detection App

Gerçek zamanlı dolandırıcılık tespiti: BERTurk + EWMA + Whisper

---

## Kurulum

### 1. Repoyu klonla

```bash
git clone https://github.com/fundaaaclk/ScamDetectionRealTime.git
cd ScamDetectionRealTime
```

---

### 2. Backend Kurulumu

```bash
cd scam_backend
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

#### .env dosyası oluştur

`scam_backend/.env` dosyası oluştur (Funda'dan al):

```
MONGODB_URI=mongodb+srv://...
MONGODB_DB_NAME=scam_guard_db
```

#### Modeli indir

Model otomatik olarak Google Drive'dan indirilir. İlk `python run.py` çalıştırıldığında `models/` klasörüne indirilir.

Manuel indirmek istersen: `scam_backend/models/` klasörüne `berturk_scam_v6_final.zip` koy.

#### Backend'i başlat

```bash
python run.py
```

Çalıştığını kontrol et:
```bash
curl http://localhost:8000/health
```

---

### 3. Android Kurulumu

1. Android Studio'yu aç
2. `AndroidStudioProjects/ScamDetect/` klasörünü aç
3. `ApiService.kt` içindeki IP adresini güncelle:

```kotlin
// Emülatör için:
const val BASE_URL = "http://10.0.2.2:8000"

// Gerçek cihaz için (Mac'in WiFi IP'si):
const val BASE_URL = "http://192.168.x.x:8000"
```

4. Run butonuna bas veya terminalden:

```bash
cd AndroidStudioProjects/ScamDetect
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Mimari

```
Android (Kotlin/Compose)
    └── ApiService → HTTP → FastAPI Backend
                                ├── Whisper (ses → metin)
                                ├── BERTurk v6 (scam sınıflandırma)
                                ├── EWMA Manager (trend analizi)
                                └── MongoDB Atlas (kayıt)
```

## Özellikler

- **Mikrofon analizi** — Canlı ses, 3 saniyelik chunk'larla analiz
- **Dosya analizi** — MP4, MP3, WAV, M4A desteği
- **Simülasyon** — Hazır scam senaryoları
- **Geçmiş** — Tüm analizler MongoDB'de saklanır
- **EWMA + Ensemble** — Trend takibi, alarm sistemi
