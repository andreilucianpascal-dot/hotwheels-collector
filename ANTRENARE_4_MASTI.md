# 🚀 Antrenare TFLite cu Cele 4 Măști

Ghid pas cu pas pentru antrenarea modelului TFLite folosind cele 4 măști existente (0.png, 11.png, 24.png, 33.png).

## 📋 Ce Vei Avea Nevoie

1. ✅ **4 imagini originale** (0.jpg, 11.jpg, 24.jpg, 33.jpg)
2. ✅ **4 măști PNG** (0.png, 11.png, 24.png, 33.png) - deja le ai!
3. ✅ **Python 3.8+** instalat
4. ✅ **TensorFlow** instalat

## 📁 Pasul 1: Organizează Datele

Creează structura de foldere:

```
C:\Users\Andrei\Desktop\train_tflite\
├── dataset/
│   ├── images/
│   │   ├── 0.jpg
│   │   ├── 11.jpg
│   │   ├── 24.jpg
│   │   └── 33.jpg
│   └── masks/
│       ├── 0.png
│       ├── 11.png
│       ├── 24.png
│       └── 33.png
└── train_tflite_4_masks.py
```

### Cum să copiezi fișierele:

1. **Imaginile originale** (0.jpg, 11.jpg, 24.jpg, 33.jpg):
   - Le găsești în folderul unde le-ai salvat când le-ai făcut pozele
   - Copiază-le în `dataset/images/`

2. **Măștile PNG** (0.png, 11.png, 24.png, 33.png):
   - Le găsești în: `app/src/main/assets/mask/`
   - Copiază-le în `dataset/masks/`

3. **Scriptul de antrenare**:
   - `train_tflite_4_masks.py` (deja creat în proiect)

## 🔧 Pasul 2: Instalează Dependențele

Deschide PowerShell sau Command Prompt și rulează:

```powershell
# Navighează la folderul de antrenare
cd C:\Users\Andrei\Desktop\train_tflite

# Instalează TensorFlow (dacă nu e instalat)
py -m pip install tensorflow

# Instalează celelalte dependențe
py -m pip install numpy pillow scikit-learn
```

**Notă**: Dacă ai RTX 5070 și vrei să folosești GPU:
```powershell
# Instalează TensorFlow cu suport GPU
py -m pip install tensorflow[and-cuda]
```

## 🎯 Pasul 3: Rulează Antrenarea

```powershell
# Asigură-te că ești în folderul corect
cd C:\Users\Andrei\Desktop\train_tflite

# Rulează scriptul
py train_tflite_4_masks.py
```

### Ce va face scriptul:

1. ✅ Verifică că toate fișierele există
2. ✅ Încarcă imaginile și măștile
3. ✅ Aplică data augmentation (crește de la 4 la ~16 imagini)
4. ✅ Antrenează modelul UNet (50 epoci)
5. ✅ Convertește la TFLite
6. ✅ Salvează `card_segmentation.tflite`

### Timp estimat:

- **Cu CPU**: ~10-15 minute
- **Cu GPU (RTX 5070)**: ~2-5 minute ⚡

## 📦 Pasul 4: Copiază Modelul în Aplicație

După ce antrenarea e gata:

1. **Găsește modelul**: `card_segmentation.tflite` (în folderul de antrenare)

2. **Copiază-l în aplicație**:
   ```
   app/src/main/assets/models/card_segmentation.tflite
   ```

3. **Rebuild aplicația** în Android Studio

4. **Testează** - TFLite va fi folosit automat! 🎉

## ⚠️ Limitări cu 4 Măști

Cu doar 4 imagini, modelul:
- ✅ Va funcționa și va genera măști
- ⚠️ Nu va fi perfect pentru poze noi (overfitting)
- ✅ E suficient pentru testare și validare a procesului

**Recomandare**: După ce testezi, adaugă mai multe măști (10-20+) pentru un model mai robust.

## 🐛 Rezolvare Probleme

### Eroare: "No module named 'tensorflow'"
```powershell
py -m pip install tensorflow
```

### Eroare: "No module named 'sklearn'"
```powershell
py -m pip install scikit-learn
```

### Eroare: "Folderul dataset nu există"
- Verifică că ai creat structura de foldere corect
- Verifică că ești în folderul corect când rulezi scriptul

### Modelul e prea mare (>10MB)
- Scriptul folosește deja optimizări (quantization)
- Modelul ar trebui să fie ~2-5MB

## ✅ Verificare Finală

După antrenare, verifică:

1. ✅ `card_segmentation.tflite` există
2. ✅ Mărimea: ~2-5MB
3. ✅ Copiat în `app/src/main/assets/models/`
4. ✅ Aplicația rebuild-uită
5. ✅ Logcat arată: "✅ TFLite model loaded successfully"

## 🎉 Gata!

Acum ai un model TFLite funcțional care:
- ✅ Generează măști pentru orice poză
- ✅ Funcționează în aplicație
- ✅ Va fi îmbunătățit când adaugi mai multe măști














