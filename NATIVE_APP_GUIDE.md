# Saját Pénzügyek - 100% Natív Android Alkalmazás

## 📱 Elérhető APK Fájlok (Telepítésre kész)

Az alkalmazás teljes natív (Kotlin + Jetpack Compose + Room + CameraX + ML Kit) verziója sikeresen lefordult és aláírásra került. A telepítő fájlok közvetlenül elérhetők a projekt gyökerében:

1. **Optimalizált verzió (Ajánlott):**
   - **Útvonal:** `G:\penzugyi-dashboard\penzugyek-native.apk`
   - **Méret:** ~53.3 MB (teljes offline ML Kit neurális hálót, CameraX motort és SQLite adatbázist tartalmaz)
2. **Debug verzió:**
   - **Útvonal:** `G:\penzugyi-dashboard\penzugyek-native-debug.apk`
   - **Méret:** ~59.4 MB

---

## 🚀 Telepítés a telefonra lépésről lépésre

1. **Fájl átvitele a telefonra:**
   - Csatlakoztasd a telefonodat USB-kábellel a géphez, és másold át a `penzugyek-native.apk` fájlt a *Letöltések (Downloads)* mappába.
   - **VAGY** küldd át magadnak Telegramon, Google Drive-on, vagy helyi hálózaton keresztül.
2. **Telepítés engedélyezése:**
   - A telefonodon nyisd meg a *Fájlkezelőt (File Manager)*, koppints a `penzugyek-native.apk`-ra.
   - Ha a telefon rákérdez, engedélyezd az *„Ismeretlen forrásból származó alkalmazások telepítése”* (Install unknown apps) opciót a fájlkezelő számára.
3. **Megnyitás:**
   - Koppints a **Telepítés** gombra, majd nyisd meg az alkalmazást (**Saját Pénzügyek** néven találod meg az app fiókban).

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
