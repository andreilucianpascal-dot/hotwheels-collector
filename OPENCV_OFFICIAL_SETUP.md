# Setup OpenCV Android SDK - Metoda Oficială

## Pas 1: Descărcare OpenCV Android SDK

1. Mergi la: https://opencv.org/releases/
2. Descarcă **"OpenCV – 4.x.x Android pack"** (recomandat: 4.8.0 sau 4.9.0)
3. Extrage arhiva într-un folder temporar (ex: `C:\Users\Andrei\Downloads\OpenCV-android-sdk`)

## Pas 2: Copiere Modul în Proiect

1. Copiază folderul `sdk` din `OpenCV-android-sdk` în root-ul proiectului:
   ```
   C:\Users\Andrei\StudioProjects\hotwheels-collector\opencv
   ```

2. Structura ar trebui să fie:
   ```
   hotwheels-collector/
   ├── app/
   ├── opencv/          ← Modulul OpenCV
   │   ├── build.gradle
   │   ├── src/
   │   └── ...
   ├── build.gradle.kts
   └── settings.gradle.kts
   ```

## Pas 3: Actualizare settings.gradle.kts

Adaugă modulul OpenCV:

```kotlin
rootProject.name = "HotWheelsCollectors"
include(":app")
include(":opencv")  // ← Adaugă această linie
```

## Pas 4: Actualizare app/build.gradle.kts

Înlocuiește dependența Maven cu:

```kotlin
// ---------- OpenCV (Post-processing pentru TFLite masks) ----------
// OpenCV Android SDK - modul oficial
implementation(project(":opencv"))
```

## Pas 5: Verificare build.gradle al modulului opencv

Modulul `opencv` ar trebui să aibă deja un `build.gradle` configurat corect.
Dacă nu există, trebuie creat unul care să exporte bibliotecile native.

## Avantaje Metodă Oficială:

✅ Control complet asupra versiunii  
✅ Posibilitate de customizare build  
✅ Versiune oficială de la OpenCV  
✅ Include toate bibliotecile native (`.so`)  
✅ Compatibil cu toate versiunile Android  

## Dezavantaje:

❌ Mai complex de setat inițial  
❌ Trebuie să descarci și să copiezi manual  
❌ Mărește dimensiunea repository-ului  

## Alternativă Rapidă (Maven):

Dacă vrei o soluție rapidă, folosește:
```kotlin
implementation("com.quickbirdstudios:opencv:4.5.3.0")
```

Aceasta este o versiune prebuilt care funcționează imediat, dar nu oferă același control.














