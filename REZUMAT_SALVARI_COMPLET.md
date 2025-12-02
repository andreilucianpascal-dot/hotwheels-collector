# 📸 REZUMAT COMPLET: Cum sunt salvate pozele și datele în aplicație

## 🔄 FLUXUL COMPLET DE SALVARE

### 1️⃣ **PROCESARE POZE (CameraManager.processCarPhotos)**

**Input:**
- 1 poza front (originală)
- 1 poza back (pentru barcode)

**Procesare:**
1. ✅ Extrage barcode din poza back (ML Kit)
2. ✅ **ȘTERGE poza back** (după extragere barcode)
3. ✅ Generează **thumbnail** din poza front (300 KB)
4. ✅ Generează **full photo** din poza front (500 KB)

**Output:**
- ✅ **2 fișiere finale**: `thumbnail.jpg` (300KB) + `full.jpg` (500KB)
- ✅ Barcode string

**⚠️ IMPORTANT:** Poza back NU este salvată niciodată - se șterge imediat după extragere barcode!

---

## 2️⃣ **SALVARE LOCALĂ (Room Database + Storage)**

### 📁 **Fișiere Fizice pe Dispozitiv**

**Locație:** `/data/data/com.example.hotwheelscollectors/files/photos/{userId}/{carId}/`

**Fișiere salvate:**
```
📁 photos/
  └── 📁 {userId}/
      └── 📁 {carId}/
          ├── 📄 thumbnail.jpg  (300 KB)
          └── 📄 full.jpg        (500 KB)
```

**Total:** **2 fișiere fizice** per mașină

---

### 🗄️ **Room Database - CarEntity Table**

**Tabel:** `cars`

**Câmpuri relevante pentru poze:**
```kotlin
CarEntity {
    id: String                          // UUID unic
    userId: String
    photoUrl: String                    // = permanentFull (path la full.jpg)
    frontPhotoPath: String              // = permanentFull (path la full.jpg)
    combinedPhotoPath: String          // = permanentThumbnail (path la thumbnail.jpg)
    barcode: String
    // ... alte câmpuri
}
```

**Ce salvează:**
- ✅ `photoUrl` = `/data/data/.../photos/{userId}/{carId}/full.jpg`
- ✅ `frontPhotoPath` = `/data/data/.../photos/{userId}/{carId}/full.jpg`
- ✅ `combinedPhotoPath` = `/data/data/.../photos/{userId}/{carId}/thumbnail.jpg`

**Total în Room:** **1 înregistrare CarEntity** per mașină

---

### 🗄️ **Room Database - PhotoEntity Table**

**Tabel:** `photos`

**Câmpuri relevante:**
```kotlin
PhotoEntity {
    id: String                          // UUID unic
    carId: String                       // FK la cars.id
    localPath: String                   // = permanentThumbnail (thumbnail.jpg)
    thumbnailPath: String?              // = permanentThumbnail (thumbnail.jpg)
    fullSizePath: String?               // = permanentFull (full.jpg)
    cloudPath: String                   // = "" (se populează după upload)
    type: PhotoType                      // = FRONT
    syncStatus: SyncStatus              // = PENDING_UPLOAD
    barcode: String?                    // Barcode extras
    // ... alte câmpuri
}
```

**Ce salvează:**
- ✅ `localPath` = `/data/data/.../photos/{userId}/{carId}/thumbnail.jpg`
- ✅ `thumbnailPath` = `/data/data/.../photos/{userId}/{carId}/thumbnail.jpg`
- ✅ `fullSizePath` = `/data/data/.../photos/{userId}/{carId}/full.jpg`

**Total în Room:** **1 înregistrare PhotoEntity** per mașină

---

## 3️⃣ **FIREBASE STORAGE (Upload în Cloud)**

### 📤 **Upload Proces**

**Funcție:** `CarSyncRepository.uploadPhotoToFirestore()`

**Upload-ează 2 poze:**
1. **Thumbnail** (300 KB)
   - Local path: `car.combinedPhotoPath` (din Room)
   - Storage path: `mainline/{carId}/thumbnail/{UUID}.jpg`
   - Returnează: `https://firebasestorage.googleapis.com/.../thumbnail/xxx.jpg`

2. **Full Photo** (500 KB)
   - Local path: `car.photoUrl` (din Room)
   - Storage path: `mainline/{carId}/full/{UUID}.jpg`
   - Returnează: `https://firebasestorage.googleapis.com/.../full/xxx.jpg`

**Total în Firebase Storage:** **2 fișiere** per mașină

**Structură Storage:**
```
📁 firebase-storage/
  └── 📁 mainline/           (sau premium/, treasure_hunt/, etc.)
      └── 📁 {carId}/
          ├── 📄 thumbnail/
          │   └── {UUID}.jpg
          └── 📄 full/
              └── {UUID}.jpg
```

---

## 4️⃣ **FIRESTORE DATABASE (Documente Cloud)**

### 📄 **Collection: `globalCars`**

**Path:** `globalCars/{carId}` (carId este UUID unic per utilizator)

**Document structure:**
```javascript
{
  carId: "uuid-unique-per-user",
  barcode: "1234567890",
  carName: "Ferrari F40",
  brand: "Ferrari",
  series: "Mainline",
  year: 2024,
  color: "Red",
  frontPhotoUrl: "https://firebasestorage.googleapis.com/.../thumbnail/xxx.jpg",  // ✅ THUMBNAIL URL
  backPhotoUrl: "https://firebasestorage.googleapis.com/.../full/xxx.jpg",       // ✅ FULL URL
  croppedBarcodeUrl: "",  // Nu se folosește în prezent
  category: "Mainline",
  subcategory: "Rally",
  contributorUserId: "user-id",
  verificationCount: 1,
  createdAt: Timestamp,
  // ... alte câmpuri
}
```

**Ce salvează:**
- ✅ `frontPhotoUrl` = **Download URL pentru thumbnail** (din Firebase Storage)
- ✅ `backPhotoUrl` = **Download URL pentru full photo** (din Firebase Storage)
- ✅ Toate datele mașinii (nume, brand, serie, an, culoare, etc.)

**Total în Firestore:** **1 document** per mașină în `globalCars`

---

### 📄 **Collection: `globalBarcodes`**

**Path:** `globalBarcodes/{barcode}` (barcode este cheia documentului)

**Document structure:**
```javascript
{
  barcode: "1234567890",
  carName: "Ferrari F40",
  brand: "Ferrari",
  series: "Mainline",
  year: 2024,
  color: "Red",
  frontPhotoUrl: "https://firebasestorage.googleapis.com/.../thumbnail/xxx.jpg",  // ✅ THUMBNAIL URL
  backPhotoUrl: "https://firebasestorage.googleapis.com/.../full/xxx.jpg",       // ✅ FULL URL
  contributorUserId: "user-id",
  verificationCount: 1,
  lastVerified: Timestamp,
  createdAt: Timestamp,
  // ... alte câmpuri
}
```

**Ce salvează:**
- ✅ **DOAR dacă barcode-ul NU există deja** (prima verificare)
- ✅ `frontPhotoUrl` = **Download URL pentru thumbnail**
- ✅ `backPhotoUrl` = **Download URL pentru full photo**
- ✅ Date mașină (prima mașină cu acel barcode)

**Total în Firestore:** **1 document** per barcode unic în `globalBarcodes`

**⚠️ IMPORTANT:** 
- Dacă 100 de mașini au același barcode dar descriere diferită:
  - ✅ Toate cele 100 se salvează în `globalCars` (fiecare cu carId unic)
  - ✅ Doar prima se salvează în `globalBarcodes` (barcode este unic)
  - ✅ Toate au propriile thumbnail + full URL-uri în Storage

---

## 5️⃣ **CE VEDE UTILIZATORUL ÎN APLICAȚIE**

### 📱 **LOCAL (Colecția Mea)**

**Ecran:** Tab "My Collection" → Mainline / Premium / etc.

**Ce afișează:**
- ✅ Date din **Room Database** (`CarEntity`)
- ✅ Thumbnail-ul afișat din: `carEntity.combinedPhotoPath` (fișier local)
- ✅ Full photo afișat din: `carEntity.photoUrl` (fișier local)

**Source:** Fișiere fizice de pe dispozitiv (`/photos/{userId}/{carId}/`)

---

### 🌐 **BROWSE (Baza de Date Globală)**

**Ecran:** Tab "Browse" → Mainline / Premium / etc.

**Ce afișează:**
- ✅ Date din **Firestore Database** (`globalCars` collection)
- ✅ Thumbnail-ul afișat din: `car.frontPhotoUrl` (Firebase Storage download URL)
- ✅ Text: `"{brand}-{category}({verificationCount}) verified by {verificationCount} user"`
- ✅ Button: "Add to My Collection"

**Source:** 
- Firestore: `firestoreRepository.getGlobalMainlineCars()`
- Firebase Storage: `AsyncImage(model = car.frontPhotoUrl)`

**⚠️ PROBLEMA ACTUALĂ:**
- Dacă `car.frontPhotoUrl` este gol (`""`), thumbnail-ul nu se afișează
- Cauza: Upload-ul în Firebase Storage eșuează (permisiuni / App Check)

---

## 📊 **REZUMAT: CÂTE EXEMPLARE SE SALVEAZĂ**

### Per Mașină Adăugată:

| Locație | Tip | Cantitate | Exemplu |
|---------|-----|-----------|---------|
| **Local Storage** | Fișiere fizice | **2 fișiere** | `thumbnail.jpg` + `full.jpg` |
| **Room Database** | `CarEntity` | **1 înregistrare** | 1 mașină |
| **Room Database** | `PhotoEntity` | **1 înregistrare** | 1 poza (cu 2 path-uri) |
| **Firebase Storage** | Fișiere în cloud | **2 fișiere** | `thumbnail/{UUID}.jpg` + `full/{UUID}.jpg` |
| **Firestore** | `globalCars` document | **1 document** | Cu 2 URL-uri (thumbnail + full) |
| **Firestore** | `globalBarcodes` document | **0-1 document** | Doar dacă barcode nou |

### Total pe Mașină:
- ✅ **2 fișiere locale** (thumbnail + full)
- ✅ **2 fișiere în Firebase Storage** (thumbnail + full)
- ✅ **1-2 documente în Firestore** (globalCars + eventual globalBarcodes)

---

## 🔍 **VERIFICĂRI NECESARE**

### ✅ Ce Funcționează:
1. ✅ Procesare poze (thumbnail 300KB + full 500KB)
2. ✅ Salvare locală (Room + fișiere fizice)
3. ✅ Logica de sync (CarSyncRepository)

### ⚠️ Ce Trebuie Verificat:
1. ⚠️ **Firebase Storage Upload** - Verifică dacă upload-ul reușește
2. ⚠️ **Firebase Storage Rules** - Verifică permisiunile
3. ⚠️ **Firebase App Check** - Verifică configurația
4. ⚠️ **Browse Thumbnail** - Verifică dacă `frontPhotoUrl` este populat în Firestore

### 🔧 **Debug Logs:**
- `StorageRepository`: "=== STARTING PHOTO UPLOAD ==="
- `CarSyncRepository`: "Firestore Storage URLs:" (thumbnail + full)
- `FirestoreRepository`: "Saved to globalCars collection"

---

## 📝 **NOTIȚE IMPORTANTE**

1. **Poza back se șterge** - Nu este salvată niciodată, doar pentru extragere barcode
2. **2 poze finale** - Thumbnail (300KB) + Full (500KB) din poza front
3. **Barcode comun** - Dacă 100 de mașini au același barcode, toate se salvează în `globalCars`, doar prima în `globalBarcodes`
4. **Browse folosește thumbnail** - `car.frontPhotoUrl` este thumbnail URL pentru afișare rapidă
5. **Full photo pentru detalii** - `car.backPhotoUrl` este full URL pentru ecranul de detalii

---

**Data analizei:** 2025-11-02
**Status:** Cod verificat și documentat ✅



