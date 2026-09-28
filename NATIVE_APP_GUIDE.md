# Finances - 100% Native Android Application

## 📱 Available APK File (Ready to Install)

The fully native (Kotlin + Jetpack Compose + Room + CameraX + ML Kit) Android application has been compiled and is available directly in the project root:

- **Path:** `G:\penzugyi-dashboard\finances-native.apk`
- **Size:** ~58.8 MB (includes offline ML Kit text recognition engine, CameraX, and local SQLite/Room database)

---

## 🚀 Installation Guide

1. **Transfer to your phone:**
   - Connect your phone to your PC via USB cable and copy `finances-native.apk` to your *Downloads* folder.
   - **OR** transfer via Google Drive, Telegram, or local network.
2. **Install:**
   - On your phone, open *File Manager*, tap `finances-native.apk`.
   - If prompted, allow *Install unknown apps* for the file manager.
3. **Launch:**
   - Tap **Install / Update**, then open **Finances** from your app drawer.

---

## 🏦 Fő Funkciók és Használati Útmutató

### 1. Banki Értesítés Figyelő és Automatikus Könyvelés
- **Támogatott bankok:** Revolut, OTP Bank (SmartBank & Új OTP), Erste George, MBH Bank, Wise, valamint általános magyar banki formátumok.
- **Hogyan aktiváld:**
  1. Az appban menj az **Automatizáció (Értesítések)** fülre (alsó navigáció vagy menü).
  2. Koppints az **Értesítési hozzáférés megadása** gombra.
  3. A rendszer közvetlenül a speciális Android beállításokhoz visz. Keresd meg a **Saját Pénzügyek** appot, és engedélyezd.
  4. **Xiaomi / Redmi / POCO (HyperOS / MIUI) felhasználóknak:**
     - Az appban található útmutató segít: nyisd meg a *Beállítások -> Alkalmazások -> Saját Pénzügyek -> Akkumulátorkímélő* pontot, és állítsd **„Nincs korlátozás” (No restrictions)** módra, valamint engedélyezd az **Automatikus indítást (Autostart)**, hogy a háttérben futó banki értesítésfigyelő folyamatosan aktív maradjon.
  5. **Tesztelés:** Az appban található *„OTP Teszt”* és *„Revolut Teszt”* gombokkal szimulálhatsz beérkező értesítéseket valós banki tranzakció nélkül is!

### 2. Nyugta és Számla OCR Beolvasás (Kamera + ML Kit)
- **Hogyan működik:**
  - Menj a **Nyugta beolvasása** menüpontra (alsó menüsor vagy a Dashboard „Nyugta fotózása” gombja).
  - Add meg a kamera engedélyt.
  - Az élő keresőben látható egy célzókeret és a vaku (torch) kapcsoló gomb.
  - Fotózd le a blokkot (Lidl, Aldi, Spar, Penny, stb.) vagy válassz egy fotót a galériából.
  - Az eszközön futó Google ML Kit motor másodpercek alatt kinyeri a bolt nevét, a fizetendő végösszeget, a fizetési módot (Készpénz / Kártya), az ÁFA-t és a dátumot.
  - A felugró ellenőrző kártyán egy koppintással jóváhagyhatod és elmentheted.

### 3. Tranzakciók, Szűrés és Keresés
- Intelligens diakritika-mentes kereső (pl. az „etterem” megtalálja az „Étterem” tételeket is).
- Időszak szerinti szűrés: Ez a hónap, Múlt hónap, 3 hónap, 6 hónap, Összes.
- Kategória szerinti szűrés és Ismétlődő tételek gyorsszűrője.
- Manuális tranzakció rögzítése a jobb alsó **+** gombbal (Kiadás, Bevétel, Átvezetés).

### 4. CSV Import
- Importálhatsz OTP, Revolut, Erste vagy egyéni banki CSV kivonatokat.
- Automatikus karakterkódolás (UTF-8, UTF-8 BOM, ISO-8859-2), pontos magyar/európai tizedesvesszők (`1 450,50 Ft`) és idézőjeles mezők kezelése.
- Előnézeti táblázat, oszlop-hozzárendelés és duplikáció-szűrés importálás előtt.

### 5. Pénzügyi Elemzések & Számlák
- Bevételek, Kiadások, Egyenleg és Megtakarítási ráta (%) számítás.
- Kategória szerinti eloszlás grafikon és havi trendek.
- Számlák kezelése: Készpénz, OTP Folyószámla, Revolut Számla, Megtakarítások.

---

## 🛠 Technológiai Összegzés & Architektúra

| Réteg | Megvalósítás |
| :--- | :--- |
| **Felhasználói felület (UI)** | 100% Jetpack Compose, Material 3, Navigation Compose |
| **Adatbázis & Perzisztencia** | SQLite / Room 2.6.1 ORM, DataStore Preferences |
| **Kamera & Számítógépes látás** | CameraX 1.3.2 + Google ML Kit Text Recognition 16.0.0 |
| **Háttérszolgáltatás** | `NotificationListenerService` (Coroutine IO Dispatcher) |
| **Pénzügyi matematika** | Pontos egész filléres (Long minor units) aritmetika, 0 kerekítési hiba |
| **Lokalizáció** | Kétnyelvű (Magyar / Angol) Android `strings.xml` erőforrásokon keresztül |
| **Kódminőség & Tesztek** | Unit tesztek: `FinancialMathTest`, `CategorizationAndDedupTest`, `ParsersTest` (Mind sikeresen lefutott) |
| **Eszköz- és Build-környezet** | Strictly Isolated a `G:` meghajtón (`G:\tools\jdk-17`, `G:\tools\android-sdk`, `G:\tools\gradle-8.5`) |
