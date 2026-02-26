"""
Script de antrenare TFLite pentru segmentarea cartonașelor Hot Wheels
Folosește cele 4 măști existente (0.png, 11.png, 24.png, 33.png)

REQUIREMENTS:
- Python 3.8+
- tensorflow>=2.14.0
- numpy
- pillow (PIL)

INSTALARE:
py -m pip install tensorflow numpy pillow

FOLOSIRE:
1. Organizează pozele și măștile:
   dataset/
     images/
       0.jpg
       11.jpg
       24.jpg
       33.jpg
     masks/
       0.png
       11.png
       24.png
       33.png

2. Rulează: py train_tflite_4_masks.py

3. Modelul va fi salvat ca: card_segmentation.tflite
"""

import os
import numpy as np
from PIL import Image
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
from tensorflow.keras.callbacks import ModelCheckpoint, EarlyStopping
import glob

print("=" * 60)
print("🚀 Antrenare TFLite pentru 4 măști")
print("=" * 60)

# ============================================================================
# CONFIGURARE
# ============================================================================
IMAGE_SIZE = 256  # Dimensiune input pentru model (256x256)
BATCH_SIZE = 2   # Batch size mic pentru 4 imagini
EPOCHS = 50       # Număr de epoci (poate fi ajustat)
VALIDATION_SPLIT = 0.2  # 20% pentru validare (1 imagine din 4)

# Path-uri - structura existentă după convert_coco_to_masks.py
CURRENT_DIR = os.getcwd()
IMAGES_DIR = os.path.join(CURRENT_DIR, "images")  # Pozele originale (0.jpg, 11.jpg, 24.jpg, 33.jpg)
MASKS_DIR = os.path.join(CURRENT_DIR, "masks")    # Măștile (0.png, 11.png, 24.png, 33.png)
OUTPUT_MODEL = "card_segmentation.tflite"

# ============================================================================
# VERIFICARE DATE
# ============================================================================
print("\n📁 Verificare dataset...")

# Verifică dacă există folderul cu măști
if not os.path.exists(MASKS_DIR):
    print(f"❌ Folderul {MASKS_DIR} nu există!")
    print("   Asigură-te că ai rulat convert_coco_to_masks.py și ai folderul 'masks'")
    print(f"   Folderul curent: {CURRENT_DIR}")
    exit(1)

# Găsește toate imaginile și măștile
image_files = sorted(glob.glob(os.path.join(IMAGES_DIR, "*.jpg")))
mask_files = sorted(glob.glob(os.path.join(MASKS_DIR, "*.png")))

print(f"   Imagini găsite: {len(image_files)}")
print(f"   Măști găsite: {len(mask_files)}")

if len(image_files) == 0 or len(mask_files) == 0:
    print("❌ Nu s-au găsit imagini sau măști!")
    exit(1)

# Verifică că fiecare imagine are o mască corespunzătoare
matched_pairs = []
for img_path in image_files:
    img_name = os.path.basename(img_path).replace(".jpg", "")
    mask_path = os.path.join(MASKS_DIR, f"{img_name}.png")
    
    if os.path.exists(mask_path):
        matched_pairs.append((img_path, mask_path))
        print(f"   ✅ {img_name}.jpg ↔ {img_name}.png")
    else:
        print(f"   ⚠️  {img_name}.jpg - mască lipsă!")

if len(matched_pairs) < 4:
    print(f"❌ Doar {len(matched_pairs)} perechi găsite. Necesităm minim 4!")
    exit(1)

print(f"\n✅ {len(matched_pairs)} perechi imagini-măști găsite")

# ============================================================================
# ÎNCĂRCARE DATE
# ============================================================================
print("\n📥 Încărcare date...")

def load_image(path, target_size=(IMAGE_SIZE, IMAGE_SIZE)):
    """Încarcă și redimensionează imaginea"""
    img = Image.open(path).convert('RGB')
    img = img.resize(target_size, Image.Resampling.LANCZOS)
    img_array = np.array(img, dtype=np.float32) / 255.0  # Normalizează [0, 1]
    return img_array

def load_mask(path, target_size=(IMAGE_SIZE, IMAGE_SIZE)):
    """Încarcă și redimensionează masca (binară)"""
    mask = Image.open(path).convert('L')  # Grayscale
    mask = mask.resize(target_size, Image.Resampling.NEAREST)
    mask_array = np.array(mask, dtype=np.float32)
    # Normalizează: > 128 = 1 (cartonaș), <= 128 = 0 (background)
    mask_array = (mask_array > 128).astype(np.float32)
    return mask_array

# Încarcă toate perechile
images = []
masks = []

for img_path, mask_path in matched_pairs:
    img = load_image(img_path)
    mask = load_mask(mask_path)
    images.append(img)
    masks.append(mask)
    print(f"   ✅ {os.path.basename(img_path)} ({img.shape}) + {os.path.basename(mask_path)} ({mask.shape})")

images = np.array(images)
masks = np.array(masks)
masks = np.expand_dims(masks, axis=-1)  # Adaugă dimensiune channel: (4, 256, 256, 1)

print(f"\n✅ Date încărcate:")
print(f"   Images shape: {images.shape}")
print(f"   Masks shape: {masks.shape}")

# ============================================================================
# DATA AUGMENTATION (pentru a crește dataset-ul)
# ============================================================================
print("\n🔄 Data augmentation (crește dataset-ul de la 4 la ~16 imagini)...")

from tensorflow.keras.preprocessing.image import ImageDataGenerator

# Augmentare pentru imagini
img_datagen = ImageDataGenerator(
    rotation_range=15,
    width_shift_range=0.1,
    height_shift_range=0.1,
    zoom_range=0.1,
    horizontal_flip=False,  # Nu flip-uim cartonașele
    fill_mode='constant',
    cval=0.0
)

# Augmentare pentru măști (aceleași transformări)
mask_datagen = ImageDataGenerator(
    rotation_range=15,
    width_shift_range=0.1,
    height_shift_range=0.1,
    zoom_range=0.1,
    horizontal_flip=False,
    fill_mode='constant',
    cval=0.0
)

# Aplică augmentarea
augmented_images = [images]
augmented_masks = [masks]

for i in range(3):  # 3x augmentare = 4 * 4 = 16 imagini total
    for img, mask in zip(images, masks):
        # Generează transformări identice pentru imagine și mască
        seed = np.random.randint(10000)
        
        img_aug = img_datagen.random_transform(img, seed=seed)
        mask_aug = mask_datagen.random_transform(mask.squeeze(), seed=seed)
        mask_aug = np.expand_dims(mask_aug, axis=-1)
        
        augmented_images.append(np.expand_dims(img_aug, axis=0))
        augmented_masks.append(np.expand_dims(mask_aug, axis=0))

augmented_images = np.vstack(augmented_images)
augmented_masks = np.vstack(augmented_masks)

print(f"   ✅ Dataset augmentat: {augmented_images.shape[0]} imagini")

# ============================================================================
# SPLIT TRAIN/VALIDATION
# ============================================================================
from sklearn.model_selection import train_test_split

X_train, X_val, y_train, y_val = train_test_split(
    augmented_images, augmented_masks,
    test_size=0.2,
    random_state=42
)

print(f"\n📊 Split dataset:")
print(f"   Train: {X_train.shape[0]} imagini")
print(f"   Validation: {X_val.shape[0]} imagini")

# ============================================================================
# MODEL UNet SIMPLIFICAT
# ============================================================================
print("\n🏗️  Construire model UNet...")

def build_unet(input_size=(IMAGE_SIZE, IMAGE_SIZE, 3)):
    """Construiește un model UNet simplificat pentru segmentare"""
    
    inputs = keras.Input(input_size)
    
    # Encoder
    c1 = layers.Conv2D(32, 3, activation='relu', padding='same')(inputs)
    c1 = layers.Conv2D(32, 3, activation='relu', padding='same')(c1)
    p1 = layers.MaxPooling2D((2, 2))(c1)
    
    c2 = layers.Conv2D(64, 3, activation='relu', padding='same')(p1)
    c2 = layers.Conv2D(64, 3, activation='relu', padding='same')(c2)
    p2 = layers.MaxPooling2D((2, 2))(c2)
    
    c3 = layers.Conv2D(128, 3, activation='relu', padding='same')(p2)
    c3 = layers.Conv2D(128, 3, activation='relu', padding='same')(c3)
    p3 = layers.MaxPooling2D((2, 2))(c3)
    
    # Bottleneck
    c4 = layers.Conv2D(256, 3, activation='relu', padding='same')(p3)
    c4 = layers.Conv2D(256, 3, activation='relu', padding='same')(c4)
    
    # Decoder
    u5 = layers.UpSampling2D((2, 2))(c4)
    u5 = layers.concatenate([u5, c3])
    c5 = layers.Conv2D(128, 3, activation='relu', padding='same')(u5)
    c5 = layers.Conv2D(128, 3, activation='relu', padding='same')(c5)
    
    u6 = layers.UpSampling2D((2, 2))(c5)
    u6 = layers.concatenate([u6, c2])
    c6 = layers.Conv2D(64, 3, activation='relu', padding='same')(u6)
    c6 = layers.Conv2D(64, 3, activation='relu', padding='same')(c6)
    
    u7 = layers.UpSampling2D((2, 2))(c6)
    u7 = layers.concatenate([u7, c1])
    c7 = layers.Conv2D(32, 3, activation='relu', padding='same')(u7)
    c7 = layers.Conv2D(32, 3, activation='relu', padding='same')(c7)
    
    # Output
    outputs = layers.Conv2D(1, 1, activation='sigmoid')(c7)
    
    model = keras.Model(inputs, outputs)
    return model

model = build_unet()
model.compile(
    optimizer=keras.optimizers.Adam(learning_rate=0.001),
    loss='binary_crossentropy',
    metrics=['accuracy', 'binary_accuracy']
)

print(f"✅ Model construit:")
model.summary()

# ============================================================================
# ANTRENARE
# ============================================================================
print("\n🎯 Antrenare model...")
print(f"   Epochs: {EPOCHS}")
print(f"   Batch size: {BATCH_SIZE}")

callbacks = [
    EarlyStopping(monitor='val_loss', patience=10, restore_best_weights=True),
    ModelCheckpoint('best_model.h5', monitor='val_loss', save_best_only=True)
]

history = model.fit(
    X_train, y_train,
    batch_size=BATCH_SIZE,
    epochs=EPOCHS,
    validation_data=(X_val, y_val),
    callbacks=callbacks,
    verbose=1
)

print("\n✅ Antrenare completă!")

# ============================================================================
# CONVERSIE LA TFLite
# ============================================================================
print("\n🔄 Conversie la TFLite...")

# Încarcă cel mai bun model
model.load_weights('best_model.h5')

# Convertește la TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# Optimizări pentru mărime și viteză
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# Convertește
tflite_model = converter.convert()

# Salvează
with open(OUTPUT_MODEL, 'wb') as f:
    f.write(tflite_model)

file_size = os.path.getsize(OUTPUT_MODEL) / (1024 * 1024)  # MB
print(f"✅ Model TFLite salvat: {OUTPUT_MODEL}")
print(f"   Mărime: {file_size:.2f} MB")

# ============================================================================
# TESTARE MODEL
# ============================================================================
print("\n🧪 Testare model...")

# Testează pe o imagine de validare
test_img = X_val[0:1]
test_mask = y_val[0:1]

prediction = model.predict(test_img, verbose=0)
prediction_binary = (prediction > 0.5).astype(np.float32)

# Calculează IoU (Intersection over Union)
intersection = np.logical_and(test_mask, prediction_binary).sum()
union = np.logical_or(test_mask, prediction_binary).sum()
iou = intersection / union if union > 0 else 0

print(f"   IoU (Intersection over Union): {iou:.3f}")
print(f"   (1.0 = perfect, >0.7 = bun, >0.5 = acceptabil)")

# ============================================================================
# FINAL
# ============================================================================
print("\n" + "=" * 60)
print("✅ GATA! Modelul TFLite este gata!")
print("=" * 60)
print(f"\n📦 Model salvat: {OUTPUT_MODEL}")
print(f"   Copiază-l în: app/src/main/assets/models/card_segmentation.tflite")
print("\n📝 Următorii pași:")
print("   1. Copiază card_segmentation.tflite în folderul models/ din Android")
print("   2. Rebuild aplicația")
print("   3. Testează - TFLite va fi folosit automat!")
print("\n" + "=" * 60)

