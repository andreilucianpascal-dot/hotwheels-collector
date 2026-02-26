# 📸 Ghid: Data Augmentation pentru TFLite

## 🎯 Obiectiv

Înmulțirea dataset-ului de la **48 imagini → 480 imagini** prin aplicarea automată de transformări (rotații, flip, brightness, contrast, etc.).

---

## 📋 Pași de urmat

### Pasul 1: Organizare Date

Asigură-te că ai structura:

```
training_48/
  ├── images/        (48 imagini .jpg)
  └── masks/         (48 masti .png)
```

**Verificare**:
```powershell
cd C:\Users\Andrei\Desktop
dir training_48\images
dir training_48\masks
```

Ar trebui să vezi **48 fișiere** în fiecare director.

---

### Pasul 2: Instalare Librării

Augmentation-ul folosește biblioteca `albumentations` (specializată pentru segmentare).

**Comandă**:
```powershell
py -m pip install albumentations opencv-python
```

**Timp estimat**: 30-60 secunde

---

### Pasul 3: Copiere Script Augmentation

Copiază scriptul în desktop:

```powershell
cd C:\Users\Andrei\Desktop
# Scriptul augment_dataset.py ar trebui sa fie deja aici
```

**Verificare**:
```powershell
dir augment_dataset.py
```

---

### Pasul 4: Rulare Augmentation

**Comandă**:
```powershell
py augment_dataset.py
```

**Ce se întâmplă**:
1. Citește cele 48 imagini + măști din `training_48/`
2. Generează **10 variante** pentru fiecare (1 originală + 9 augmentate)
3. Salvează în `training_480/images/` și `training_480/masks/`

**Output așteptat**:
```
========================================
DATA AUGMENTATION
========================================
Imagini originale: 48
Variante per imagine: 10
Total imagini dupa augmentation: 480
========================================

[1/48] Procesare: 0.jpg
  -> Generat 10 variante (1 orig + 9 aug)
[2/48] Procesare: 11.jpg
  -> Generat 10 variante (1 orig + 9 aug)
...
[48/48] Procesare: 47.jpg
  -> Generat 10 variante (1 orig + 9 aug)

========================================
AUGMENTATION COMPLETAT!
========================================
Total imagini generate: 480
  - Imagini: training_480/images
  - Masti: training_480/masks
========================================

Verificare dataset:
  - Imagini gasite: 480
  - Masti gasite: 480
  Verificare OK: Toate imaginile au masti!

GATA! Acum poti antrena modelul cu:
  py train_tflite_480_masks.py
```

**Timp estimat**: **2-3 minute**

---

### Pasul 5: Antrenare Model cu 480 Imagini

**Comandă**:
```powershell
py train_tflite_480_masks.py
```

**Ce se întâmplă**:
1. Încarcă cele 480 imagini + măști
2. Split: 80% antrenare (384 imagini), 20% validare (96 imagini)
3. Antrenează modelul UNet (100 epochs, early stopping dacă nu îmbunătățește)
4. Salvează `card_segmentation_480.tflite`

**Timp estimat**: **30-40 minute** (pe RTX 5070)

**Output așteptat**:
```
=== INCARCARE DATASET ===
Imagini: training_480/images
Masti: training_480/masks
Dataset incarcat:
  - Imagini: (480, 256, 256, 3)
  - Masti: (480, 256, 256, 1)
  - Imagini valide: 480

=== SPLIT DATASET ===
Antrenare: 384 imagini
Validare: 96 imagini

=== CREARE MODEL UNET ===
Model creat: ...

=== ANTRENARE MODEL ===
Epochs: 100
Batch size: 16
Learning rate: 0.001

INFO: Pe RTX 5070, antrenarea ar trebui sa dureze ~30-40 minute
Poti monitoriza progresul in timp real...

Epoch 1/100
24/24 ━━━━━━━━━━━━━━━━━━━━ 12s 485ms/step - loss: 0.4521 - dice_coefficient: 0.5479 - val_loss: 0.3212 - val_dice_coefficient: 0.6788
Epoch 2/100
24/24 ━━━━━━━━━━━━━━━━━━━━ 10s 420ms/step - loss: 0.2987 - dice_coefficient: 0.7013 - val_loss: 0.2456 - val_dice_coefficient: 0.7544
...
Epoch 45/100
24/24 ━━━━━━━━━━━━━━━━━━━━ 10s 425ms/step - loss: 0.0523 - dice_coefficient: 0.9477 - val_loss: 0.0487 - val_dice_coefficient: 0.9513

=== EVALUARE FINALA ===
Validation Loss: 0.0487
Validation Dice Coefficient: 0.9513
Validation Accuracy: 0.9856

=== CONVERSIE LA TFLITE ===
Model TFLite salvat: card_segmentation_480.tflite
Marime: 3245.67 KB

========================================
ANTRENARE COMPLETATA CU SUCCES!
========================================
Rezultate:
  - Dice Coefficient: 0.9513
  - Accuracy: 0.9856

Urmatori pasi:
1. Copiaza 'card_segmentation_480.tflite' in:
   app/src/main/assets/models/card_segmentation.tflite
2. Rebuildeaza aplicatia Android
3. Testeaza cu poze noi!
========================================
```

---

### Pasul 6: Copiere Model în Aplicație

**Comandă**:
```powershell
copy C:\Users\Andrei\Desktop\card_segmentation_480.tflite C:\Users\Andrei\StudioProjects\hotwheels-collector\app\src\main\assets\models\card_segmentation.tflite
```

---

### Pasul 7: Rebuild Aplicație

În **Android Studio**:
1. Click **Build** → **Rebuild Project**
2. Așteaptă compilarea (2-3 minute)
3. Run pe telefon

---

## 📊 Rezultate Așteptate

| Metric | Cu 48 Imagini | Cu 480 Imagini (Augmentate) |
|--------|---------------|------------------------------|
| Dice Coefficient | 0.988 (antrenare) | **0.95+** (validare) |
| Acuratețe Reală | ~30% | **80-95%** |
| Pixels Kept | 30% | **75-90%** |

---

## ⚠️ Probleme Posibile

### Eroare: `ModuleNotFoundError: No module named 'albumentations'`

**Soluție**:
```powershell
py -m pip install albumentations opencv-python
```

---

### Eroare: `training_48/images nu exista`

**Soluție**:
Verifică că ai structura corectă:
```powershell
cd C:\Users\Andrei\Desktop
dir training_48\images
dir training_48\masks
```

---

### Antrenarea durează prea mult (>1 oră)

**Cauză**: GPU-ul nu este utilizat.

**Verificare**:
```powershell
py -c "import tensorflow as tf; print('GPU:', tf.config.list_physical_devices('GPU'))"
```

**Output așteptat**:
```
GPU: [PhysicalDevice(name='/physical_device:GPU:0', device_type='GPU')]
```

Dacă vezi lista goală, TensorFlow folosește CPU-ul.

**Soluție**:
```powershell
py -m pip install tensorflow-gpu
```

---

## 🎉 Succes!

După finalizarea pașilor, vei avea un model TFLite mult mai precis, antrenat pe 480 imagini augmentate!

**Acuratețea așteptată**: **80-95%** (comparativ cu 30% anterior)














