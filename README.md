# SE Shield - Real-Time Scam Detection

A mobile application that detects social engineering and scam calls in real time using speech recognition and NLP.

## Architecture

```
Android (Kotlin / Jetpack Compose)
    └── REST API --> FastAPI Backend
                        ├── Whisper       (speech to text)
                        ├── BERTurk v6   (scam classification)
                        ├── EWMA Manager (trend analysis)
                        └── MongoDB Atlas (storage)
```

## Features

- Live microphone analysis with chunk-based processing
- Audio file analysis (MP4, MP3, WAV, M4A)
- EWMA trend tracking with ensemble scoring
- Scam simulation scenarios
- Analysis history with detailed reports

---

## Setup

### Prerequisites

- Python 3.10+
- Android Studio (Hedgehog or later)
- Android device or emulator (API 26+)
- MongoDB Atlas account

---

### Backend

**1. Clone the repository**

```bash
git clone https://github.com/fundaaaclk/ScamDetectionRealTime.git
cd ScamDetectionRealTime/scam_backend
```

**2. Create a virtual environment and install dependencies**

```bash
python -m venv venv
source venv/bin/activate       # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

**3. Create the .env file**

Create `scam_backend/.env` with the following content (get credentials from the project owner):

```
MONGODB_URI=mongodb+srv://<user>:<password>@<cluster>.mongodb.net/
MONGODB_DB_NAME=scam_guard_db
```

**4. Run the backend**

```bash
python run.py
```

The BERTurk model will be downloaded automatically from Google Drive on first run.
To download manually, place `berturk_scam_v6_final.zip` inside `scam_backend/models/`.

Verify the backend is running:

```bash
curl http://localhost:8000/health
```

---

### Android

**1. Open the project in Android Studio**

Open `AndroidStudioProjects/ScamDetect/` as an existing project.

**2. Set the backend URL**

Edit `ApiService.kt`:

```kotlin
// Android emulator
const val BASE_URL = "http://10.0.2.2:8000"

// Physical device (use your machine's local IP)
const val BASE_URL = "http://192.168.x.x:8000"
```

Make sure the device and the machine running the backend are on the same network.

**3. Build and install**

Via Android Studio: click Run, or from terminal:

```bash
cd AndroidStudioProjects/ScamDetect
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.scamdetect/.MainActivity
```

---

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /health | Health check |
| POST | /analyze | Analyze text |
| POST | /transcribe-file | Upload and analyze audio file |
| POST | /transcribe-chunk | Analyze live microphone chunk |
| POST | /microphone-session-end | Save completed session |
| GET | /analyses | List analysis history |
| GET | /analyses/stats | Summary statistics |
| GET | /scenarios | List simulation scenarios |
