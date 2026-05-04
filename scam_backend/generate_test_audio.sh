#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# generate_test_audio.sh
# Mac "say" komutuyla Türkçe test ses dosyaları üretir → MP4 olarak kaydeder
# Kullanım: bash generate_test_audio.sh
# ─────────────────────────────────────────────────────────────────────────────

OUTPUT_DIR="/Users/fundacelik/Desktop/ses_kayıtları"
VOICE="Yelda"          # Türkçe ses: Yelda (varsa). Yoksa: say -v ? | grep tr

mkdir -p "$OUTPUT_DIR"

# ─── Senaryo listesi ──────────────────────────────────────────────────────────
# Format: "dosya_adı|metin"

declare -a SCENARIOS=(
  # ── SCAM senaryolar ──────────────────────────────────────────────────────────
  "scam_banka_sifre|Merhaba, Ziraat Bankası güvenlik departmanından arıyorum. Hesabınızda şüpheli bir işlem tespit edildi. Hesabınızı korumak için internet bankacılığı şifrenizi şimdi doğrulamamız gerekiyor. Lütfen şifrenizi söyler misiniz?"

  "scam_kart_iptali|Acil durum bildirimi! Kredi kartınızdan 12.500 TL tutarında yetkisiz bir işlem gerçekleşti. İşlemi iptal etmek için hemen kart numaranızı, son kullanma tarihinizi ve güvenlik kodunuzu söylemeniz gerekiyor. Aksi hâlde paranız gitmiş olacak."

  "scam_kargo_link|Kargo şirketinden bilgilendirme. Adresinize teslim edilemeyen paketiniz için gümrük vergisi ödemeniz gerekmektedir. Ödeme yapmak ve teslimat adresinizi güncellemek için şu linke tıklayın. Bugün ödeme yapılmazsa paketiniz iade edilecektir."

  "scam_sahte_polis|Emniyet Müdürlüğü'nden arıyorum. Adınıza sahte banka hesabı açılmış. Soruşturma kapsamında banka hesap bilgilerinizi ve para miktarınızı bizimle paylaşmanız zorunludur. Bu gizli bir operasyondur, kimseyle konuşmayınız."

  "scam_vergi_iadesi|Gelir İdaresi Başkanlığı'ndan arıyorum. Yıllık vergi iadeniz onaylandı. 3.200 TL iade almak için hesap numaranızı ve TC kimlik numaranızı doğrulamamız gerekiyor. Bilgileri şimdi paylaşırsanız para 24 saat içinde hesabınıza geçecek."

  "scam_teknik_destek|Microsoft teknik destek hattından arıyorum. Bilgisayarınızda kritik bir virüs tespit ettik. Bilgisayarınızı kurtarmak için size uzaktan erişim izni vermeniz ve kredi kartı bilgilerinizi paylaşmanız gerekmektedir."

  "scam_sahte_cekilme|Tebrikler! Düzenlediğimiz çekilişte 50.000 TL kazandınız. Ödülünüzü almak için önce 500 TL işlem ücreti yatırmanız gerekmektedir. Hesap numaramıza havale yaptıktan sonra ödülünüz anında hesabınıza geçecek."

  # ── GÜVENLİ senaryolar ───────────────────────────────────────────────────────
  "guvenli_komsu|Merhaba, ben alt kattan Ayşe Hanım. Yarın akşam küçük bir yemek toplantısı yapıyoruz, sizi de bekleriz. Gelirseniz çok seviniriz, lütfen haber verin."

  "guvenli_randevu|İyi günler, hastanenin randevu hattından arıyorum. Pazartesi günü saat 14:30'daki doktor randevunuzu hatırlatmak istedik. Herhangi bir değişiklik için lütfen bizi arayın."

  "guvenli_aile|Merhaba canım, ben annen. Bu akşam akşam yemeğine gelecek misin? Çorba yaptım, beyin fırtınası yapmadan önce birlikte oturursak çok güzel olur. Haber ver bekleyeyim."
)

echo ""
echo "🎙  Ses dosyaları üretiliyor → $OUTPUT_DIR"
echo "────────────────────────────────────────────"

TOTAL=${#SCENARIOS[@]}
COUNT=0

for entry in "${SCENARIOS[@]}"; do
  IFS="|" read -r NAME TEXT <<< "$entry"
  COUNT=$((COUNT + 1))
  AIFF_TMP="/tmp/${NAME}.aiff"
  MP4_OUT="${OUTPUT_DIR}/${NAME}.mp4"

  echo "[$COUNT/$TOTAL] 🔊 $NAME"

  # 1. say ile AIFF üret
  say -v "$VOICE" "$TEXT" -o "$AIFF_TMP"

  # 2. ffmpeg ile MP4/AAC'ye çevir (sessiz)
  ffmpeg -y -i "$AIFF_TMP" -c:a aac -b:a 128k "$MP4_OUT" 2>/dev/null

  # 3. Geçici AIFF sil
  rm -f "$AIFF_TMP"

  echo "    ✅ Kaydedildi: $MP4_OUT"
done

echo ""
echo "────────────────────────────────────────────"
echo "✅ Tüm dosyalar hazır: $OUTPUT_DIR"
echo ""
open "$OUTPUT_DIR"
