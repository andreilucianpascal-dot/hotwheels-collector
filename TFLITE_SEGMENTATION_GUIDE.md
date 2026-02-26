# TensorFlow Lite - Ghid Complet pentru Segmentarea Cartonașelor Hot Wheels

## 📋 Cuprins
1. [Prezentare Generală](#prezentare-generală)
2. [Arhitectura Modelului](#arhitectura-modelului)
3. [Crearea Dataset-ului](#crearea-dataset-ului)
4. [Anotarea Imaginilor](#anotarea-imaginilor)
5. [Antrenarea Modelului](#antrenarea-modelului)
6. [Conversia la TFLite](#conversia-la-tflite)
7. [Integrarea în Aplicație](#integrarea-în-aplicație)
8. [Resurse și Tools](#resurse-și-tools)

---

## 🎯 Prezentare Generală

### Ce este TensorFlow Lite?
TensorFlow Lite este o versiune optimizată a TensorFlow pentru dispozitive mobile și embedded. Este perfect pentru aplicații Android care necesită inferență ML în timp real.

### De ce TFLite pentru segmentarea cartonașelor?
- ✅ **Performanță**: Rulează direct pe device (nu necesită server)
- ✅ **Viteză**: Inferență rapidă (< 1 secundă)
- ✅ **Mărime**: Model optimizat (1-8 MB în APK)
- ✅ **Precizie**: Poate detecta forme complexe (nu doar dreptunghiuri)
- ✅ **Robust**: Funcționează cu diferite unghiuri, distanțe, iluminări

### Diferența față de Template Matching
- **Template Matching** (actual): Caută o formă fixă în imagine → limitat la forme simple
- **TFLite Segmentation**: Învață să recunoască cartonașul în orice poziție/ipostază → mult mai robust

---

## 🏗️ Arhitectura Modelului

### Opțiuni de Modele

#### 1. **UNet** (Recomandat pentru început)
- **Avantaje**: Simplu, eficient, bun pentru segmentare precisă
- **Mărime**: ~2-4 MB
- **Viteză**: ~200-500ms pe device
- **Precizie**: Foarte bună pentru obiecte cu forme complexe

#### 2. **DeepLabv3-MobileNet** (Alternativă)
- **Avantaje**: Pre-antrenat, mai rapid
- **Mărime**: ~1-2 MB
- **Viteză**: ~100-300ms
- **Precizie**: Bună, dar poate fi mai puțin precisă la margini

#### 3. **DeepLabv3+ MobileNet** (Cel mai rapid)
- **Avantaje**: Cel mai optimizat pentru mobile
- **Mărime**: ~1-2 MB
- **Viteză**: ~50-200ms
- **Precizie**: Acceptabilă pentru majoritatea cazurilor

### Recomandare Finală
**Începe cu UNet** - este cel mai simplu de antrenat și oferă cea mai bună precizie pentru forme complexe precum cartonașele Hot Wheels.

---

## 📸 Crearea Dataset-ului

### Câte Imagini Ai Nevoie?

#### Minimum Viable:
- **200-300 imagini** pentru un model funcțional
- **500+ imagini** pentru un model robust și precis
- **1000+ imagini** pentru producție (opțional)

### Distribuția pe Tipuri de Cartonașe

Ai menționat că ai **7-8 tipuri de cartonașe**:
- Cartonaș scurt (108x108)
- Cartonaș lung (108x165)
- Premium
- Mare/Large
- Și încă 2-3 tipuri

**Recomandare**: **50-70 imagini per tip** = **350-560 imagini total**

### Varietatea Imaginilor

Fiecare tip de cartonaș trebuie fotografiat în:
- ✅ **Diferite distanțe**: aproape, departe, mediu
- ✅ **Diferite unghiuri**: frontal, ușor înclinat stânga/dreapta, sus/jos
- ✅ **Diferite iluminări**: naturală, artificială, umbră parțială
- ✅ **Diferite fundaluri**: alb, colorat, texturat
- ✅ **Diferite poziții în cadru**: centru, margini, parțial în afara cadrului

### Structura Dataset-ului

```
dataset/
├── images/
│   ├── 1.jpg
│   ├── 2.jpg
│   ├── 3.jpg
│   └── ...
├── masks/
│   ├── 1.png
│   ├── 2.png
│   ├── 3.png
│   └── ...
└── annotations.json (opțional - pentru metadata)
```

**Convenție de nume**: 
- Imagine: `1.jpg`, `2.jpg`, etc.
- Mască: `1.png`, `2.png`, etc. (același număr!)

---

## 🎨 Anotarea Imaginilor

### Ce este o Mască?

O **mască** este o imagine PNG cu:
- **Fundal negru (0,0,0)** = zonele care NU sunt cartonaș
- **Cartonaș alb (255,255,255)** = zona care ESTE cartonaș
- **Opțional**: Zone gri pentru margini fuzzy (anti-aliasing)

### Cum Să Faci Anotările?

#### Opțiunea 1: **LabelMe** (Recomandat - Gratuit)
- **Download**: https://github.com/wkentaro/labelme
- **Instalare**: `pip install labelme`
- **Utilizare**:
  1. Deschizi imaginea
  2. Desenezi conturul cartonașului cu poligon
  3. Salvezi ca JSON
  4. Export ca PNG mask

**Avantaje**: 
- Gratuit și open-source
- Suportă forme complexe (poligoane, nu doar dreptunghiuri)
- Export direct la PNG masks
- Batch processing

#### Opțiunea 2: **CVAT** (Pentru echipe)
- **Website**: https://cvat.org/
- **Avantaje**: Colaborare, workflow profesional
- **Dezavantaje**: Mai complex, necesită server

#### Opțiunea 3: **Photoshop/GIMP** (Manual)
- Deschizi imaginea
- Selectezi cartonașul cu Pen Tool (pentru forme complexe)
- Creezi mască (Select → Save Selection)
- Export ca PNG (alb pe negru)

### Detalii Importante pentru Anotări

#### Forma Cartonașului
Cartonașele Hot Wheels **NU sunt dreptunghiuri perfecte**:
- Au colțuri tăiate
- Au decupaje în formă de "umeras"
- Margini rotunjite
- Forme complexe

**Soluție**: Folosește **poligoane** în LabelMe pentru a urmări exact conturul!

#### Precizia Anotărilor
- ✅ **Foarte important**: Anotările trebuie să fie **precise**
- ✅ Urmează exact marginea cartonașului (nu mai mult, nu mai puțin)
- ✅ Pentru margini fuzzy, folosește gri (anti-aliasing)
- ❌ **Evită**: Anotări aproximative sau "aproape bune"

#### Batch Anotare
După ce ai 50-100 imagini anotate, poți folosi:
- **Data augmentation** (rotații, flip-uri, brightness) pentru a multiplica dataset-ul
- **Semi-supervised learning** (opțional, avansat)

---

## 🚀 Antrenarea Modelului

### Setup Inițial

#### 1. Instalează TensorFlow

**Pentru RTX 5070 (CUDA 12.x):**

**Opțiunea 1: pip (recomandat)**
```bash
# Verifică mai întâi GPU-ul:
nvidia-smi

# Instalează TensorFlow cu CUDA 12.x support:
pip install tensorflow[and-cuda]

# SAU versiunea specifică:
pip install tensorflow==2.15.0
```

**Opțiunea 2: conda (mai simplu pentru CUDA/cuDNN)**
```bash
conda create -n tf-gpu python=3.10
conda activate tf-gpu
conda install -c conda-forge tensorflow-gpu cudatoolkit=12.2 cudnn=8.9
```

**Verifică instalarea:**
```python
import tensorflow as tf
print("TensorFlow version:", tf.__version__)
print("GPU Available:", len(tf.config.list_physical_devices('GPU')) > 0)
if tf.config.list_physical_devices('GPU'):
    print("GPU Name:", tf.config.list_physical_devices('GPU')[0].name)
    # Ar trebui să vezi: /physical_device:GPU:0
```

**Dacă NU vezi GPU-ul:**
1. Verifică că ai instalat CUDA 12.x
2. Verifică că ai instalat cuDNN 8.9.x
3. Reinstalează TensorFlow: `pip uninstall tensorflow tensorflow-gpu && pip install tensorflow[and-cuda]`

#### 2. Instalează Librării Suplimentare
```bash
pip install numpy
pip install opencv-python
pip install matplotlib
pip install pillow
pip install scikit-learn
```

### Structura Codului de Antrenare

#### Pasul 1: Încarcă Dataset-ul
```python
import os
import cv2
import numpy as np
from sklearn.model_selection import train_test_split

def load_dataset(images_dir, masks_dir):
    images = []
    masks = []
    
    # Asigură-te că numele fișierelor se potrivesc
    for filename in os.listdir(images_dir):
        if filename.endswith('.jpg'):
            img_path = os.path.join(images_dir, filename)
            mask_path = os.path.join(masks_dir, filename.replace('.jpg', '.png'))
            
            if os.path.exists(mask_path):
                img = cv2.imread(img_path)
                mask = cv2.imread(mask_path, cv2.IMREAD_GRAYSCALE)
                
                # Redimensionează la dimensiune fixă (ex: 256x256 sau 512x512)
                img = cv2.resize(img, (256, 256))
                mask = cv2.resize(mask, (256, 256))
                
                # Normalizează
                img = img.astype(np.float32) / 255.0
                mask = (mask > 127).astype(np.float32)  # Binarizează masca
                
                images.append(img)
                masks.append(mask)
    
    return np.array(images), np.array(masks)

# Încarcă datele
images, masks = load_dataset('dataset/images', 'dataset/masks')
```

#### Pasul 2: Split Train/Validation
```python
X_train, X_val, y_train, y_val = train_test_split(
    images, masks, test_size=0.2, random_state=42
)
```

#### Pasul 3: Data Augmentation
```python
from tensorflow.keras.preprocessing.image import ImageDataGenerator

# Augmentare pentru imagini
img_datagen = ImageDataGenerator(
    rotation_range=15,
    width_shift_range=0.1,
    height_shift_range=0.1,
    zoom_range=0.1,
    horizontal_flip=True,
    brightness_range=[0.8, 1.2]
)

# Augmentare pentru măști (doar transformări geometrice, NU brightness!)
mask_datagen = ImageDataGenerator(
    rotation_range=15,
    width_shift_range=0.1,
    height_shift_range=0.1,
    zoom_range=0.1,
    horizontal_flip=True
)
```

#### Pasul 4: Construiește Modelul UNet
```python
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Input, Conv2D, MaxPooling2D, UpSampling2D, Concatenate

def build_unet(input_size=(256, 256, 3)):
    inputs = Input(input_size)
    
    # Encoder (Downsampling)
    c1 = Conv2D(64, (3, 3), activation='relu', padding='same')(inputs)
    c1 = Conv2D(64, (3, 3), activation='relu', padding='same')(c1)
    p1 = MaxPooling2D((2, 2))(c1)
    
    c2 = Conv2D(128, (3, 3), activation='relu', padding='same')(p1)
    c2 = Conv2D(128, (3, 3), activation='relu', padding='same')(c2)
    p2 = MaxPooling2D((2, 2))(c2)
    
    c3 = Conv2D(256, (3, 3), activation='relu', padding='same')(p2)
    c3 = Conv2D(256, (3, 3), activation='relu', padding='same')(c3)
    p3 = MaxPooling2D((2, 2))(c3)
    
    # Bottleneck
    c4 = Conv2D(512, (3, 3), activation='relu', padding='same')(p3)
    c4 = Conv2D(512, (3, 3), activation='relu', padding='same')(c4)
    
    # Decoder (Upsampling)
    u5 = UpSampling2D((2, 2))(c4)
    u5 = Concatenate()([u5, c3])
    c5 = Conv2D(256, (3, 3), activation='relu', padding='same')(u5)
    c5 = Conv2D(256, (3, 3), activation='relu', padding='same')(c5)
    
    u6 = UpSampling2D((2, 2))(c5)
    u6 = Concatenate()([u6, c2])
    c6 = Conv2D(128, (3, 3), activation='relu', padding='same')(u6)
    c6 = Conv2D(128, (3, 3), activation='relu', padding='same')(c6)
    
    u7 = UpSampling2D((2, 2))(c6)
    u7 = Concatenate()([u7, c1])
    c7 = Conv2D(64, (3, 3), activation='relu', padding='same')(u7)
    c7 = Conv2D(64, (3, 3), activation='relu', padding='same')(c7)
    
    # Output layer
    outputs = Conv2D(1, (1, 1), activation='sigmoid')(c7)
    
    model = Model(inputs=inputs, outputs=outputs)
    return model

model = build_unet()
model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])
```

#### Pasul 5: Antrenare

**Optimizări pentru RTX 5070 + 32GB RAM:**

```python
from tensorflow.keras.callbacks import ModelCheckpoint, EarlyStopping, ReduceLROnPlateau
import tensorflow as tf

# Configurează GPU memory growth (pentru RTX 5070)
gpus = tf.config.experimental.list_physical_devices('GPU')
if gpus:
    try:
        for gpu in gpus:
            tf.config.experimental.set_memory_growth(gpu, True)
    except RuntimeError as e:
        print(e)

# Callbacks
callbacks = [
    ModelCheckpoint('best_model.h5', save_best_only=True, monitor='val_loss', verbose=1),
    EarlyStopping(monitor='val_loss', patience=15, restore_best_weights=True, verbose=1),
    ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=5, min_lr=1e-7, verbose=1)
]

# Antrenare cu batch_size optimizat pentru RTX 5070
history = model.fit(
    X_train, y_train,
    batch_size=16,  # ✅ Cu 32GB RAM și RTX 5070, poți folosi 16-32
    epochs=50,      # ✅ Cu RTX 5070: ~30-60 min pentru 500 imagini
    validation_data=(X_val, y_val),
    callbacks=callbacks,
    verbose=1
)

# Dacă ai erori OOM (Out of Memory), reduce batch_size la 8 sau 4
```

**Tips pentru RTX 5070:**
- ✅ Poți folosi `batch_size=16` sau chiar `32` (cu 32GB RAM)
- ✅ Mixed precision training (opțional, pentru viteză suplimentară):
```python
# Adaugă la începutul scriptului
from tensorflow.keras.mixed_precision import set_global_policy
set_global_policy('mixed_float16')  # Accelerare ~1.5-2x
```

### Timp de Antrenare

#### Pentru RTX 5070 + Ryzen 7800X3D + 32GB RAM:
- **500 imagini, UNet, batch_size=16**: **30-60 minute** ⚡
- **1000 imagini, UNet, batch_size=16**: **1-2 ore**
- **Cu mixed precision**: **20-40 minute** pentru 500 imagini

#### Comparație:
- **CPU (chiar și Ryzen 7800X3D)**: 4-6 ore pentru 500 imagini
- **RTX 5070**: **30-60 minute** pentru 500 imagini (10-12x mai rapid!)
- **Google Colab (GPU T4)**: 1-2 ore pentru 500 imagini

**Recomandare**: 
- ✅ **Cu RTX 5070** → Antrenează **local** (mult mai rapid și convenabil!)
- ❌ Nu mai ai nevoie de Colab - calculatorul tău e mai rapid!

### Setup pentru Calculator Rapid (cu GPU)

#### Configurație Recomandată: RTX 5070 + Ryzen 7800X3D + 32GB RAM

Această configurație este **excelentă** pentru antrenare! Vei putea antrena local foarte rapid.

#### Pasul 1: Instalează CUDA și cuDNN

**Pentru RTX 5070 (Ada Lovelace architecture):**
- **CUDA 12.x** (recomandat: CUDA 12.2 sau mai nou)
- **cuDNN 8.9.x** sau mai nou

**Download:**
1. CUDA: https://developer.nvidia.com/cuda-downloads
2. cuDNN: https://developer.nvidia.com/cudnn (necesită cont NVIDIA gratuit)

**Sau folosește conda (mai simplu):**
```bash
conda install -c conda-forge cudatoolkit=12.2 cudnn=8.9
```

#### Pasul 2: Instalează TensorFlow cu GPU Support

```bash
# Pentru RTX 5070 (CUDA 12.x):
pip install tensorflow[and-cuda]

# SAU dacă folosești conda:
conda install -c conda-forge tensorflow-gpu
```

#### Pasul 3: Verifică Setup-ul

```bash
# Verifică GPU-ul
nvidia-smi
```

Ar trebui să vezi RTX 5070 listat.

```python
# Verifică în Python
import tensorflow as tf
print("TensorFlow version:", tf.__version__)
print("GPU Available:", len(tf.config.list_physical_devices('GPU')) > 0)
if tf.config.list_physical_devices('GPU'):
    print("GPU Name:", tf.config.list_physical_devices('GPU')[0].name)
    # Configurează memory growth pentru a evita OOM
    gpus = tf.config.experimental.list_physical_devices('GPU')
    if gpus:
        try:
            for gpu in gpus:
                tf.config.experimental.set_memory_growth(gpu, True)
            print("✅ GPU memory growth enabled")
        except RuntimeError as e:
            print(e)
```

#### Estimări de Timp pentru RTX 5070:

- **500 imagini, UNet, batch_size=16**: **30-60 minute** (foarte rapid!)
- **1000 imagini, UNet, batch_size=16**: **1-2 ore**
- **Cu data augmentation**: +20-30% timp

**Avantaje:**
- ✅ Antrenare locală (nu depinzi de Colab)
- ✅ Control complet asupra procesului
- ✅ Poți rula multiple experimente rapid
- ✅ 32GB RAM = poți folosi batch_size mai mare (16-32)

---

## 📦 Conversia la TFLite

### Pasul 1: Salvează Modelul
```python
model.save('unet_model.h5')
```

### Pasul 2: Conversie la TFLite
```python
import tensorflow as tf

# Încarcă modelul
model = tf.keras.models.load_model('best_model.h5')

# Convertește la TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# Optimizări (opțional, dar recomandat)
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# Quantizare (reduce mărimea modelului)
converter.target_spec.supported_types = [tf.float16]  # sau tf.int8 pentru mai mic

# Convertește
tflite_model = converter.convert()

# Salvează
with open('card_segmentation.tflite', 'wb') as f:
    f.write(tflite_model)
```

### Mărime Model Final
- **Float32**: ~8-12 MB
- **Float16**: ~4-6 MB (recomandat)
- **Int8**: ~2-3 MB (poate pierde precizie)

---

## 📱 Integrarea în Aplicație Android

### Pasul 1: Adaugă TFLite în Proiect

#### `build.gradle` (Module: app)
```gradle
dependencies {
    // TensorFlow Lite
    implementation 'org.tensorflow:tensorflow-lite:2.14.0'
    implementation 'org.tensorflow:tensorflow-lite-gpu:2.14.0'  // Opțional - pentru GPU
    
    // Support pentru imagini
    implementation 'org.tensorflow:tensorflow-lite-support:0.4.4'
}
```

#### Copiază Modelul
Copiază `card_segmentation.tflite` în:
```
app/src/main/assets/models/card_segmentation.tflite
```

### Pasul 2: Creează TFLite Manager

#### `TFLiteSegmentationManager.kt`
```kotlin
package com.example.hotwheelscollectors.domain.manager

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteSegmentationManager(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private val INPUT_SIZE = 256  // Dimensiunea la care ai antrenat modelul
    private val PIXEL_SIZE = 3    // RGB
    
    init {
        loadModel()
    }
    
    private fun loadModel() {
        try {
            val modelFile = loadModelFile("card_segmentation.tflite")
            interpreter = Interpreter(modelFile)
            Timber.d("✅ TFLite model loaded successfully")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to load TFLite model")
        }
    }
    
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Procesează o imagine și returnează masca segmentată
     */
    fun segmentCard(source: Bitmap): Bitmap? {
        val interpreter = this.interpreter ?: return null
        
        // 1. Redimensionează imaginea la INPUT_SIZE
        val resizedBitmap = Bitmap.createScaledBitmap(
            source,
            INPUT_SIZE,
            INPUT_SIZE,
            true
        )
        
        // 2. Convertește Bitmap la ByteBuffer
        val inputBuffer = bitmapToByteBuffer(resizedBitmap)
        
        // 3. Pregătește output buffer
        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputBuffer = ByteBuffer.allocateDirect(
            outputShape[1] * outputShape[2] * outputShape[3] * 4  // Float32 = 4 bytes
        ).order(ByteOrder.nativeOrder())
        
        // 4. Rulează inferența
        interpreter.run(inputBuffer, outputBuffer)
        
        // 5. Convertește output la Bitmap (mască)
        val maskBitmap = outputBufferToBitmap(outputBuffer, outputShape)
        
        // 6. Redimensionează masca la dimensiunea originală
        return Bitmap.createScaledBitmap(
            maskBitmap,
            source.width,
            source.height,
            true
        )
    }
    
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(
            INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE * 4  // Float32
        ).order(ByteOrder.nativeOrder())
        
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val pixelValue = pixels[pixel++]
                
                // Normalizează la [0, 1]
                byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)  // R
                byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)   // G
                byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)            // B
            }
        }
        
        return byteBuffer
    }
    
    private fun outputBufferToBitmap(
        buffer: ByteBuffer,
        shape: IntArray
    ): Bitmap {
        buffer.rewind()
        val width = shape[1]
        val height = shape[2]
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        for (i in 0 until height) {
            for (j in 0 until width) {
                val value = buffer.float
                // Convertește probabilitatea [0,1] la [0,255]
                val grayValue = (value * 255).toInt().coerceIn(0, 255)
                pixels[i * width + j] = android.graphics.Color.rgb(
                    grayValue, grayValue, grayValue
                )
            }
        }
        maskBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return maskBitmap
    }
    
    /**
     * Aplică masca pe imaginea originală și extrage cartonașul
     */
    fun extractCardWithMask(source: Bitmap, mask: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(
            source.width,
            source.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(result)
        
        // Fundal alb
        canvas.drawColor(android.graphics.Color.WHITE)
        
        // Desenează doar zonele din mască
        val paint = android.graphics.Paint()
        val srcRect = android.graphics.Rect(0, 0, source.width, source.height)
        val dstRect = android.graphics.Rect(0, 0, source.width, source.height)
        
        // Creează un BitmapShader pentru a aplica masca
        val shader = android.graphics.BitmapShader(
            source,
            android.graphics.Shader.TileMode.CLAMP,
            android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = shader
        
        // Aplică masca ca alpha mask
        val maskPaint = android.graphics.Paint()
        maskPaint.xfermode = android.graphics.PorterDuffXfermode(
            android.graphics.PorterDuff.Mode.DST_IN
        )
        
        canvas.drawBitmap(source, srcRect, dstRect, paint)
        canvas.drawBitmap(mask, srcRect, dstRect, maskPaint)
        
        return result
    }
    
    fun release() {
        interpreter?.close()
        interpreter = null
    }
}
```

### Pasul 3: Integrează în CameraManager

#### Modifică `CameraManager.kt`
```kotlin
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tfliteManager = TFLiteSegmentationManager(context)
    
    /**
     * Procesează poza folosind TFLite în loc de template matching
     */
    suspend fun processPhotoWithTFLite(photoUri: Uri): Bitmap? = withContext(Dispatchers.Default) {
        try {
            // 1. Încarcă imaginea
            val sourceBitmap = loadBitmapFromUri(photoUri) ?: return@withContext null
            
            // 2. Obține masca din TFLite
            val mask = tfliteManager.segmentCard(sourceBitmap)
                ?: return@withContext null
            
            // 3. Extrage cartonașul folosind masca
            val extractedCard = tfliteManager.extractCardWithMask(sourceBitmap, mask)
            
            // 4. Returnează rezultatul
            extractedCard
        } catch (e: Exception) {
            Timber.e(e, "Failed to process photo with TFLite")
            null
        }
    }
}
```

---

## 🛠️ Resurse și Tools

### Tools pentru Anotare
1. **LabelMe**: https://github.com/wkentaro/labelme
2. **CVAT**: https://cvat.org/
3. **Roboflow**: https://roboflow.com/ (cloud-based, opțional)

### Resurse de Învățare
1. **TensorFlow Lite Tutorial**: https://www.tensorflow.org/lite
2. **UNet Paper**: https://arxiv.org/abs/1505.04597
3. **Image Segmentation Guide**: https://www.tensorflow.org/tutorials/images/segmentation

### Google Colab Templates
- Caută "UNet TensorFlow Colab" pentru template-uri gata de folosit
- Sau folosește acest template: https://github.com/zhixuhao/unet

### Dataset-uri Publice (pentru referință)
- **COCO Dataset**: https://cocodataset.org/ (pentru a vedea cum arată anotările profesionale)
- **Cityscapes**: https://www.cityscapes-dataset.com/ (pentru segmentare urbană)

---

## ⏱️ Timeline Estimativ

### Faza 1: Dataset (1-2 săptămâni)
- Fotografiere: 2-3 zile (350-560 imagini)
- Anotare: 5-10 zile (50-100 imagini/zi cu LabelMe)

### Faza 2: Antrenare (1 zi - cu RTX 5070!)
- Setup CUDA/TensorFlow: 2-3 ore
- Antrenare: **30-60 minute** pentru 500 imagini (foarte rapid cu RTX 5070!)
- Testare și optimizare: 2-3 ore

### Faza 3: Integrare (2-3 zile)
- Conversie TFLite: 0.5 zi
- Integrare în app: 1-2 zile
- Testare și optimizare: 1 zi

**Total**: ~2-3 săptămâni pentru un model funcțional

---

## ✅ Checklist Final

### Înainte de a Începe
- [ ] Instalează LabelMe
- [ ] Pregătește camera pentru fotografiere
- [ ] Creează structura de foldere pentru dataset

### În Timpul Anotării
- [ ] Asigură-te că numele fișierelor se potrivesc (1.jpg → 1.png)
- [ ] Verifică calitatea anotărilor (precizie la margini)
- [ ] Testează pe 10-20 imagini înainte de a continua

### După Antrenare
- [ ] Testează modelul pe imagini noi (nu din dataset)
- [ ] Verifică precizia (măsură cu IoU - Intersection over Union)
- [ ] Optimizează mărimea modelului (quantizare)

### În Aplicație
- [ ] Testează pe device-uri reale (nu doar emulator)
- [ ] Măsoară timpul de inferență
- [ ] Compară cu template matching (vechi) pentru a vedea îmbunătățirea

---

## 🎯 Rezumat Executiv

1. **Fotografiază 350-560 imagini** (50-70 per tip de cartonaș)
2. **Anotează cu LabelMe** (poligoane precise pe conturul cartonașului)
3. **Antrenează UNet în Python** (local pe calculator rapid sau Google Colab)
4. **Convertește la TFLite** (Float16 pentru balanță mărime/precizie)
5. **Integrează în app cu Kotlin** (folosește `TFLiteSegmentationManager`)
6. **Testează și optimizează** (măsoară precizia și viteza)

### ⚠️ Important: Python vs Kotlin

- **Python** = Pentru **antrenare** (obligatoriu, TensorFlow/Keras rulează în Python)
- **Kotlin** = Pentru **inferență în app** (TensorFlow Lite rulează în Kotlin/Android)

Nu poți antrena modelul în Kotlin - doar în Python. Dar rularea modelului (inferența) se face în Kotlin în aplicația ta Android.

**Rezultat Final**: Un model care detectează cartonașele Hot Wheels în orice poziție, unghi sau distanță, mult mai robust decât template matching!

---

## 📞 Suport

Dacă întâmpini probleme:
1. Verifică că anotările sunt corecte (măști PNG albe pe negru)
2. Verifică că dimensiunile imaginilor sunt consistente
3. Verifică că modelul se încarcă corect în app (check logs)
4. Testează modelul separat (înainte de integrare) cu imagini de test

**Succes! 🚀**

