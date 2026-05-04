# Scam Guard AI Backend

Bu backend Colab'da eğittiğin `best_model_v2.zip` modelini kullanır.

## Modeli yerleştir

`best_model_v2.zip` dosyasını şu klasöre koy:

```text
scam_backend/models/best_model_v2.zip
```

İlk çalıştırmada zip otomatik açılır.

## Çalıştırma

```bash
cd scam_backend
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
python run.py
```

Swagger test ekranı:

```text
http://127.0.0.1:8000/docs
```

## Endpointler

- `GET /health`
- `POST /analyze`
- `GET /scenarios`
- `POST /reports`

## Analyze örneği

```bash
curl -X POST "http://127.0.0.1:8000/analyze" \
  -H "Content-Type: application/json" \
  -d '{"text":"Kartınızdan 45.000 TL çekildi, iptal etmek için linke tıklayın"}'
```
