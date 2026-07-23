<div align="center">
  <img src="app/src/main/res/mipmap-hdpi/ic_launcher.png" width="128" height="128" alt="BIST Logo" />
  <h1>🚀 BİST - Borsa, Döviz & Kripto Takip</h1>
  <p><strong>Borsa İstanbul (BİST), Makro Göstergeler ve Kripto Paralar için Modern Android Takip Uygulaması</strong></p>

  <p>
    <a href="#-özellikler">Özellikler</a> •
    <a href="#-ekran-görüntüleri">Ekran Görüntüleri</a> •
    <a href="#-teknolojiler-ve-mimari">Teknolojiler</a> •
    <a href="#-gereksinimler--kurulum">Kurulum</a> •
    <a href="#-lisans">Lisans</a>
  </p>
</div>

---

## 📌 Hakkında

**BİST Tracker**, Borsa İstanbul'da işlem gören tüm hisse senetlerini, Döviz/Altın makro göstergelerini ve popüler Kripto para birimlerini canlı olarak takip etmenizi sağlayan yüksek performanslı bir Android uygulamasıdır. 

Üçüncü taraf ara sunuculara veya Google Apps Script gibi aracılara ihtiyaç duymadan **doğrudan Yahoo Finance API (v8)** üzerinden verileri eşzamanlı olarak çeker.

---

## ✨ Özellikler

### 📊 Borsa İstanbul & Makro Piyasalar
- **600+ BİST Hisse Senedi:** Borsa İstanbul'da işlem gören tüm şirketlerin (A1CAP'tan ZRGYO'ya) canlı fiyatı, günlük değişim oranı ve işlem hacimleri.
- **Endeksler & Makro Göstergeler:** BİST 30, BİST 50, BİST 100, Dolar/TL, Euro/TL, Ons Altın ($), **Gram Altın (TL)** ve Darphane Altın Sertifikası (`ALTINS1`).

### 🪙 Kripto Para Takip Tablosu
- 27+ Popüler Kripto Para (Bitcoin, Ethereum, Solana, XRP, Dogecoin, PEPE vb.) canlı Dolar ($) piyasa fiyatları.

### 📈 İnteraktif Fiyat Grafikleri
- İstediğiniz varlığa tıklayarak **Günlük (5dk), Haftalık (15dk), 1 Aylık, 3 Aylık ve 1 Yıllık** mum/çizgi trend grafiklerini görüntüleme.

### 🔔 Fiyat Alarmları & Arka Plan Bildirimleri
- Hedef fiyat alarmları kurabilme. Uygulama kapalı olsa bile `WorkManager` arka planda fiyatları kontrol eder ve hedefe ulaşıldığında bildirim gönderir.

### 📱 Android Ana Ekran Widget'ı
- Uygulamayı açmaya gerek kalmadan favori hisse ve göstergelerinizi canlı takip edebileceğiniz dinamik Widget desteği.

### 🎨 Modern UI & Göz Yormayan Karanlık Tema
- Jetpack Compose ile oluşturulmuş, yüksek performanslı, akıcı animasyonlu ve estetik karanlık (Dark) tasarım.

---

## 🛠 Teknolojiler ve Mimari

Proje, modern Android geliştirme standartlarına (MAD - Modern Android Development) uygun olarak yazılmıştır:

- **Dil:** [Kotlin](https://kotlinlang.org/) (100%)
- **Kullanıcı Arayüzü:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3 & Motion Animations)
- **Mimari:** MVVM (Model-View-ViewModel) + Clean Repository Pattern
- **Eşzamanlılık (Concurrency):** Kotlin Coroutines (`async / awaitAll`) & `StateFlow`
- **Ağ İstemcisi:** [OkHttp 4](https://square.github.io/okhttp/) & JSON Parsing (doğrudan API istemci mimarisi)
- **Arka Plan Servisleri:** [Android WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Veri Kaynağı:** Yahoo Finance v8 API

---

## 💻 Gereksinimler & Kurulum

### Geliştirme Gereksinimleri
- **Android Studio:** Ladybug (2024.2.1) veya üzeri
- **JDK:** Java 17+
- **Minimum Android Sürümü:** Android 7.0 (API 24 / Nougat)
- **Hedef Android Sürümü:** Android 14.0 (API 34)

### Kurulum Adımları
1. Repoyu bilgisayarınıza klonlayın:
   ```bash
   git clone https://github.com/yusufberkayacar/Bist-Crypto-Tracker.git
   ```
2. Android Studio'yu açın ve **`File -> Open`** diyerek proje klasörünü seçin.
3. Otomatik Gradle senkronizasyonunun (`Gradle Sync`) tamamlanmasını bekleyin.
4. Bir cihaz veya emülatör seçip **Run (`Shift + F10`)** tuşuna basarak uygulamayı çalıştırın.

---

## 📄 Lisans

Bu proje açık kaynaklıdır ve [MIT Lisansı](LICENSE) kapsamında sunulmaktadır.
