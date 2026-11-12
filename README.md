# Bundle Viewer

**[English](#english) | [Türkçe](#türkçe)**

---

## English

An Android app that lists the current bundles on [Humble Bundle](https://www.humblebundle.com/bundles), with their prices and contents.

### Features

- **Bundle list:** cover image with logo, name, included items, starting price, item count, total value and time left for every active bundle.
- **Category filters:** All / Games / Books / Software.
- **Sorting:** by deadline (default), lowest price, highest value or most items. Bundles with less than 2 days left are highlighted.
- **Refresh:** tap the refresh button in the top bar or pull down on the list.
- **Bundle details:** tap a bundle to see every price tier and the items it unlocks, with image, type, platforms and individual price.
- **Open in browser:** jump to the bundle's page on Humble Bundle to buy it.

### How it works

humblebundle.com is a Next.js app. Each page streams its data as a React Server Components payload inside `self.__next_f.push(...)` scripts. Instead of scraping the rendered HTML, the app downloads the page, joins that payload and reads the JSON objects in it:

- `/bundles` → every `algolia-hit` object is one bundle card
- each bundle page → the `bundlePage` object holds the tiers and items

Prices are shown in US dollars, as the website serves them. If Humble Bundle changes its page structure, the parser in `BundleScraper.kt` may need updating.

### Tech stack

- Kotlin, Jetpack Compose, Material 3
- OkHttp for networking, `org.json` for parsing
- Coil for image loading
- `minSdk 26` (Android 8.0), `targetSdk 36`

### Project structure

```
app/src/main/java/com/ahmet/bundleviewer/
├── MainActivity.kt      # Entry point, switches between list and detail
├── BundleViewModel.kt   # UI state, refresh and detail loading
├── BundleScraper.kt     # Downloads pages and parses the embedded JSON
├── Models.kt            # Data classes
├── ListScreen.kt        # Bundle list, filters, refresh
├── DetailScreen.kt      # Price tiers and items
├── Format.kt            # Money, date and label formatting
└── Theme.kt             # Dark color scheme
```

### Building

Requirements: JDK 17 and the Android SDK (platform 36).

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To install it on a connected device:

```bash
./gradlew installDebug
```

### Disclaimer

This is an unofficial, personal project and is not affiliated with or endorsed by Humble Bundle. All bundle data, names and images belong to their respective owners.

---

## Türkçe

[Humble Bundle](https://www.humblebundle.com/bundles) sitesindeki güncel bundle'ları fiyatları ve içerikleriyle listeleyen bir Android uygulaması.

### Özellikler

- **Bundle listesi:** Her aktif bundle için logolu kapak görseli, isim, içindeki ürünler, başlangıç fiyatı, ürün sayısı, toplam değer ve kalan süre.
- **Kategori filtreleri:** Tümü / Oyun / Kitap / Yazılım.
- **Sıralama:** Bitişi en yakın (varsayılan), en ucuz, en yüksek değer veya en çok ürün. 2 günden az kalanlar vurgulanır.
- **Yenileme:** Üst çubuktaki yenile butonuna bas ya da listeyi aşağı çek.
- **Bundle detayı:** Bir bundle'a dokununca tüm fiyat kademelerini ve her kademede açılan ürünleri görsel, tür, platform ve tek başına fiyatıyla görürsün.
- **Tarayıcıda aç:** Satın almak için bundle'ın Humble Bundle sayfasına git.

### Nasıl çalışıyor

humblebundle.com bir Next.js uygulaması. Her sayfa verisini `self.__next_f.push(...)` betikleri içinde React Server Components akışı olarak gönderiyor. Uygulama HTML kazımak yerine sayfayı indirip bu akışı birleştiriyor ve içindeki JSON nesnelerini okuyor:

- `/bundles` → her `algolia-hit` nesnesi bir bundle kartı
- her bundle sayfası → `bundlePage` nesnesi kademeleri ve ürünleri içeriyor

Fiyatlar sitenin gösterdiği gibi ABD doları olarak gösterilir. Humble Bundle sayfa yapısını değiştirirse `BundleScraper.kt` içindeki ayrıştırıcının güncellenmesi gerekebilir.

### Kullanılan teknolojiler

- Kotlin, Jetpack Compose, Material 3
- Ağ istekleri için OkHttp, ayrıştırma için `org.json`
- Görsel yükleme için Coil
- `minSdk 26` (Android 8.0), `targetSdk 36`

### Proje yapısı

```
app/src/main/java/com/ahmet/bundleviewer/
├── MainActivity.kt      # Giriş noktası, liste ve detay arasında geçiş
├── BundleViewModel.kt   # Arayüz durumu, yenileme ve detay yükleme
├── BundleScraper.kt     # Sayfaları indirir, gömülü JSON'u ayrıştırır
├── Models.kt            # Veri sınıfları
├── ListScreen.kt        # Bundle listesi, filtreler, yenileme
├── DetailScreen.kt      # Fiyat kademeleri ve ürünler
├── Format.kt            # Para, tarih ve etiket biçimlendirme
└── Theme.kt             # Koyu renk teması
```

### Derleme

Gereksinimler: JDK 17 ve Android SDK (platform 36).

```bash
./gradlew assembleDebug
```

APK `app/build/outputs/apk/debug/app-debug.apk` konumuna yazılır. Bağlı bir cihaza yüklemek için:

```bash
./gradlew installDebug
```

### Uyarı

Bu resmi olmayan, kişisel bir projedir; Humble Bundle ile bir bağlantısı yoktur ve Humble Bundle tarafından onaylanmamıştır. Tüm bundle verileri, isimleri ve görselleri ilgili sahiplerine aittir.
