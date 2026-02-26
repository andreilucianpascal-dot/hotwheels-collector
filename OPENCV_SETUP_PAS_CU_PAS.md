# 🎯 OpenCV Android SDK - Setup Pas cu Pas

## 📥 PASUL 1: Descărcare OpenCV Android SDK

1. **Deschide browser-ul** și mergi la: https://opencv.org/releases/
2. **Găsește secțiunea "OpenCV 4.12.0"** (ultima versiune - recomandat)
3. **Click pe "Android"** (sau "OpenCV – 4.12.0 Android pack")
4. **Descarcă arhiva** (ex: `opencv-4.12.0-android-sdk.zip`)
5. **Extrage arhiva** într-un folder temporar (ex: `C:\Users\Andrei\Downloads\OpenCV-android-sdk`)

**Notă:** OpenCV 4.12.0 este ultima versiune și include cele mai recente îmbunătățiri. Dacă întâmpini probleme pe dispozitive foarte noi (cu pagină de memorie 16KB), poți încerca versiunea 4.9.0 ca alternativă.

**Structura după extragere:**
```
OpenCV-android-sdk/
├── sdk/              ← ACESTA este modulul pe care îl vom folosi
│   ├── build.gradle
│   ├── src/
│   └── ...
├── samples/
└── README.android
```

---

## 📁 PASUL 2: Copiere Modul în Proiect

1. **Deschide File Explorer** și navighează la:
   ```
   C:\Users\Andrei\StudioProjects\hotwheels-collector
   ```

2. **Copiază folderul `sdk`** din `OpenCV-android-sdk` în root-ul proiectului

3. **Redenumește folderul** din `sdk` în `opencv` (sau păstrează `sdk` - ambele funcționează)

**Structura finală ar trebui să fie:**
```
hotwheels-collector/
├── app/
│   ├── build.gradle.kts
│   └── ...
├── opencv/              ← NOUL MODUL (copiat din OpenCV SDK)
│   ├── build.gradle
│   ├── src/
│   └── ...
├── build.gradle.kts
├── settings.gradle.kts
└── ...
```

---

## ⚙️ PASUL 3: Actualizare settings.gradle.kts

1. **Deschide** `settings.gradle.kts` în Android Studio
2. **Găsește linia** `include(":app")`
3. **Adaugă** linia pentru modulul OpenCV:

```kotlin
rootProject.name = "HotWheelsCollectors"
include(":app")
include(":opencv")  // ← ADAUGĂ ACEASTĂ LINIE
```

**Fișierul complet ar trebui să arate așa:**
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://repo.maven.apache.org/maven2/") }
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
    }
}

rootProject.name = "HotWheelsCollectors"
include(":app")
include(":opencv")  // ← NOUA LINIE
```

---

## 🔧 PASUL 4: Actualizare app/build.gradle.kts

1. **Deschide** `app/build.gradle.kts` în Android Studio
2. **Găsește secțiunea** cu dependența OpenCV (în jurul liniei 339-341)
3. **Înlocuiește** dependența Maven cu modulul local:

**ÎNAINTE:**
```kotlin
// ---------- OpenCV (Post-processing pentru TFLite masks) ----------
// OpenCV pentru Android - folosim dependența de la jitpack (NU FUNCȚIONEAZĂ pentru Android)
implementation("com.github.opencv:opencv:4.8.0")  // ← Aceasta este pentru desktop, nu Android!
```

**DUPĂ:**
```kotlin
// ---------- OpenCV (Post-processing pentru TFLite masks) ----------
// OpenCV Android SDK - modul oficial importat
implementation(project(":opencv"))
```

---

## ✅ PASUL 5: Verificare build.gradle al Modulului OpenCV

1. **Deschide** `opencv/build.gradle` (sau `sdk/build.gradle` dacă ai păstrat numele)
2. **Verifică** că există și este configurat corect

**Dacă nu există `build.gradle` în modulul opencv**, trebuie creat unul. Dar de obicei OpenCV SDK vine cu unul deja configurat.

**Exemplu de `opencv/build.gradle` (dacă trebuie creat):**
```gradle
apply plugin: 'com.android.library'

android {
    compileSdkVersion 34

    defaultConfig {
        minSdkVersion 24
        targetSdkVersion 34
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.txt'
        }
    }

    sourceSets {
        main {
            jniLibs.srcDirs = ['src/main/jniLibs']
            java.srcDirs = ['src/main/java']
        }
    }
}

dependencies {
    // Dependențe necesare pentru OpenCV
}
```

---

## 🔄 PASUL 6: Sync și Rebuild

1. **Click pe** "Sync Now" în Android Studio (sau File → Sync Project with Gradle Files)
2. **Așteaptă** ca sync-ul să se termine
3. **Dacă apar erori**, verifică:
   - Modulul `opencv` este în root-ul proiectului
   - `settings.gradle.kts` include `:opencv`
   - `app/build.gradle.kts` folosește `implementation(project(":opencv"))`

4. **După sync reușit**, fă **Build → Rebuild Project**

---

## 🧪 PASUL 7: Testare

1. **Rulează aplicația** pe dispozitiv/emulator
2. **Verifică logcat** pentru mesajul:
   ```
   ✅ OpenCV initialized: 4.12.0 (sau versiunea ta)
   ```

3. **Testează procesarea unei fotografii** - ar trebui să funcționeze OpenCV post-processing

---

## ❌ Rezolvare Probleme Comune

### Problema 1: "Module 'opencv' not found"
**Soluție:** Verifică că:
- Folderul `opencv` este în root-ul proiectului (același nivel cu `app`)
- `settings.gradle.kts` include `:opencv`
- Fă Sync Project

### Problema 2: "Could not find method apply()"
**Soluție:** Modulul `opencv` trebuie să aibă un `build.gradle` (nu `.kts`). Dacă OpenCV SDK vine cu `.kts`, convertește-l sau creează unul nou.

### Problema 3: "Native libraries not found"
**Soluție:** Verifică că folderul `opencv/src/main/jniLibs` conține bibliotecile native (`.so` files) pentru arhitecturile tale (arm64-v8a, armeabi-v7a, etc.)

### Problema 4: "OpenCVLoader.initDebug() returns false"
**Soluție:** 
- Verifică că bibliotecile native sunt incluse în APK
- Verifică că `OpenCVLoader.initDebug()` este apelat în `Application.onCreate()`
- Verifică logcat pentru erori specifice

---

## 📝 Checklist Final

- [ ] OpenCV Android SDK descărcat și extras
- [ ] Folderul `sdk` copiat în proiect ca `opencv`
- [ ] `settings.gradle.kts` actualizat cu `include(":opencv")`
- [ ] `app/build.gradle.kts` actualizat cu `implementation(project(":opencv"))`
- [ ] Sync Project reușit
- [ ] Rebuild Project reușit
- [ ] Aplicația rulează fără erori
- [ ] OpenCV se inițializează corect (verifică logcat)

---

## 🎉 Gata!

După ce ai completat toți pașii, OpenCV Android SDK oficial este integrat în proiect și poate fi folosit pentru post-procesarea măștilor TFLite!

