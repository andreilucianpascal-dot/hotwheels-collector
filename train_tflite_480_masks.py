"""
Script de antrenare TFLite cu 480 imagini (augmentate)
Optimizat pentru RTX 5070
"""

import os
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import cv2
from sklearn.model_selection import train_test_split

# Configurare seed pentru reproducibilitate
np.random.seed(42)
tf.random.set_seed(42)

# Configurare GPU (RTX 5070)
gpus = tf.config.list_physical_devices('GPU')
if gpus:
    try:
        for gpu in gpus:
            tf.config.experimental.set_memory_growth(gpu, True)
        print(f"GPU detectat: {len(gpus)} dispozitiv(e)")
        print(f"GPU disponibil pentru antrenare!")
    except RuntimeError as e:
        print(f"Eroare configurare GPU: {e}")
else:
    print("ATENTIE: GPU nu a fost detectat, se va folosi CPU (mai lent)")

# Parametri model
IMG_SIZE = 256
BATCH_SIZE = 16  # RTX 5070 poate procesa batch-uri mari
EPOCHS = 100
LEARNING_RATE = 0.001

def load_dataset(images_dir, masks_dir):
    """
    Incarca dataset-ul de imagini si masti
    """
    print(f"\n=== INCARCARE DATASET ===")
    print(f"Imagini: {images_dir}")
    print(f"Masti: {masks_dir}")
    
    # Lista fisiere
    image_files = sorted([f for f in os.listdir(images_dir) if f.lower().endswith(('.jpg', '.jpeg', '.png'))])
    
    images = []
    masks = []
    
    for img_file in image_files:
        base_name = os.path.splitext(img_file)[0]
        
        # Cai complete
        img_path = os.path.join(images_dir, img_file)
        mask_path = os.path.join(masks_dir, f"{base_name}.png")
        
        # Verifica ca exista masca
        if not os.path.exists(mask_path):
            print(f"ATENTIE: Masca lipseste pentru {img_file}, skip...")
            continue
        
        # Citeste imaginea
        img = cv2.imread(img_path)
        img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        img = cv2.resize(img, (IMG_SIZE, IMG_SIZE))
        img = img.astype(np.float32) / 255.0
        
        # Citeste masca
        mask = cv2.imread(mask_path, cv2.IMREAD_GRAYSCALE)
        mask = cv2.resize(mask, (IMG_SIZE, IMG_SIZE))
        mask = (mask > 127).astype(np.float32)  # Binarizare
        mask = np.expand_dims(mask, axis=-1)
        
        images.append(img)
        masks.append(mask)
    
    images = np.array(images)
    masks = np.array(masks)
    
    print(f"Dataset incarcat:")
    print(f"  - Imagini: {images.shape}")
    print(f"  - Masti: {masks.shape}")
    print(f"  - Imagini valide: {len(images)}")
    
    return images, masks

def create_unet_model(input_shape=(IMG_SIZE, IMG_SIZE, 3)):
    """
    Creeaza model UNet pentru segmentare
    """
    inputs = keras.Input(shape=input_shape)
    
    # Encoder (downsampling)
    c1 = layers.Conv2D(32, (3, 3), activation='relu', padding='same')(inputs)
    c1 = layers.Conv2D(32, (3, 3), activation='relu', padding='same')(c1)
    p1 = layers.MaxPooling2D((2, 2))(c1)
    
    c2 = layers.Conv2D(64, (3, 3), activation='relu', padding='same')(p1)
    c2 = layers.Conv2D(64, (3, 3), activation='relu', padding='same')(c2)
    p2 = layers.MaxPooling2D((2, 2))(c2)
    
    c3 = layers.Conv2D(128, (3, 3), activation='relu', padding='same')(p2)
    c3 = layers.Conv2D(128, (3, 3), activation='relu', padding='same')(c3)
    p3 = layers.MaxPooling2D((2, 2))(c3)
    
    c4 = layers.Conv2D(256, (3, 3), activation='relu', padding='same')(p3)
    c4 = layers.Conv2D(256, (3, 3), activation='relu', padding='same')(c4)
    p4 = layers.MaxPooling2D((2, 2))(c4)
    
    # Bottleneck
    c5 = layers.Conv2D(512, (3, 3), activation='relu', padding='same')(p4)
    c5 = layers.Conv2D(512, (3, 3), activation='relu', padding='same')(c5)
    
    # Decoder (upsampling)
    u6 = layers.UpSampling2D((2, 2))(c5)
    u6 = layers.concatenate([u6, c4])
    c6 = layers.Conv2D(256, (3, 3), activation='relu', padding='same')(u6)
    c6 = layers.Conv2D(256, (3, 3), activation='relu', padding='same')(c6)
    
    u7 = layers.UpSampling2D((2, 2))(c6)
    u7 = layers.concatenate([u7, c3])
    c7 = layers.Conv2D(128, (3, 3), activation='relu', padding='same')(u7)
    c7 = layers.Conv2D(128, (3, 3), activation='relu', padding='same')(c7)
    
    u8 = layers.UpSampling2D((2, 2))(c7)
    u8 = layers.concatenate([u8, c2])
    c8 = layers.Conv2D(64, (3, 3), activation='relu', padding='same')(u8)
    c8 = layers.Conv2D(64, (3, 3), activation='relu', padding='same')(c8)
    
    u9 = layers.UpSampling2D((2, 2))(c8)
    u9 = layers.concatenate([u9, c1])
    c9 = layers.Conv2D(32, (3, 3), activation='relu', padding='same')(u9)
    c9 = layers.Conv2D(32, (3, 3), activation='relu', padding='same')(c9)
    
    # Output
    outputs = layers.Conv2D(1, (1, 1), activation='sigmoid')(c9)
    
    model = keras.Model(inputs=[inputs], outputs=[outputs])
    return model

def dice_coefficient(y_true, y_pred, smooth=1e-6):
    """
    Calculeaza Dice Coefficient (metrica pentru segmentare)
    """
    y_true_f = tf.keras.backend.flatten(y_true)
    y_pred_f = tf.keras.backend.flatten(y_pred)
    intersection = tf.keras.backend.sum(y_true_f * y_pred_f)
    return (2. * intersection + smooth) / (tf.keras.backend.sum(y_true_f) + tf.keras.backend.sum(y_pred_f) + smooth)

def dice_loss(y_true, y_pred):
    """
    Dice Loss (loss function pentru segmentare)
    """
    return 1 - dice_coefficient(y_true, y_pred)

if __name__ == "__main__":
    # Cai catre date
    script_dir = os.path.dirname(os.path.abspath(__file__))
    images_dir = os.path.join(script_dir, "training_480", "images")
    masks_dir = os.path.join(script_dir, "training_480", "masks")
    
    # Verifica ca directoarele exista
    if not os.path.exists(images_dir) or not os.path.exists(masks_dir):
        print(f"EROARE: Directoarele training_480/images sau training_480/masks nu exista!")
        print(f"Ruleaza mai intai: py augment_dataset.py")
        exit(1)
    
    # Incarca dataset
    images, masks = load_dataset(images_dir, masks_dir)
    
    if len(images) == 0:
        print(f"EROARE: Nu s-au incarcat imagini!")
        exit(1)
    
    # Split dataset: 80% antrenare, 20% validare
    X_train, X_val, y_train, y_val = train_test_split(
        images, masks, test_size=0.2, random_state=42
    )
    
    print(f"\n=== SPLIT DATASET ===")
    print(f"Antrenare: {X_train.shape[0]} imagini")
    print(f"Validare: {X_val.shape[0]} imagini")
    
    # Creeaza model
    print(f"\n=== CREARE MODEL UNET ===")
    model = create_unet_model()
    
    # Compileaza model
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=LEARNING_RATE),
        loss=dice_loss,
        metrics=[dice_coefficient, 'binary_accuracy']
    )
    
    print(f"Model creat:")
    model.summary()
    
    # Callbacks
    callbacks = [
        keras.callbacks.ModelCheckpoint(
            filepath='best_model_480.h5',
            monitor='val_dice_coefficient',
            mode='max',
            save_best_only=True,
            verbose=1
        ),
        keras.callbacks.EarlyStopping(
            monitor='val_dice_coefficient',
            mode='max',
            patience=15,
            verbose=1,
            restore_best_weights=True
        ),
        keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=5,
            verbose=1,
            min_lr=1e-7
        )
    ]
    
    # Antrenare
    print(f"\n=== ANTRENARE MODEL ===")
    print(f"Epochs: {EPOCHS}")
    print(f"Batch size: {BATCH_SIZE}")
    print(f"Learning rate: {LEARNING_RATE}")
    print(f"\nINFO: Pe RTX 5070, antrenarea ar trebui sa dureze ~30-40 minute")
    print(f"Poti monitoriza progresul in timp real...\n")
    
    history = model.fit(
        X_train, y_train,
        validation_data=(X_val, y_val),
        batch_size=BATCH_SIZE,
        epochs=EPOCHS,
        callbacks=callbacks,
        verbose=1
    )
    
    # Evaluare finala
    print(f"\n=== EVALUARE FINALA ===")
    val_loss, val_dice, val_acc = model.evaluate(X_val, y_val, verbose=0)
    print(f"Validation Loss: {val_loss:.4f}")
    print(f"Validation Dice Coefficient: {val_dice:.4f}")
    print(f"Validation Accuracy: {val_acc:.4f}")
    
    # Conversie la TFLite
    print(f"\n=== CONVERSIE LA TFLITE ===")
    
    # Incarca cel mai bun model salvat
    best_model = keras.models.load_model(
        'best_model_480.h5',
        custom_objects={
            'dice_loss': dice_loss,
            'dice_coefficient': dice_coefficient
        }
    )
    
    # Converter TFLite cu optimizari
    converter = tf.lite.TFLiteConverter.from_keras_model(best_model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float32]
    
    tflite_model = converter.convert()
    
    # Salveaza modelul TFLite
    tflite_path = os.path.join(script_dir, "card_segmentation_480.tflite")
    with open(tflite_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"Model TFLite salvat: {tflite_path}")
    print(f"Marime: {len(tflite_model) / 1024:.2f} KB")
    
    print(f"\n========================================")
    print(f"ANTRENARE COMPLETATA CU SUCCES!")
    print(f"========================================")
    print(f"Rezultate:")
    print(f"  - Dice Coefficient: {val_dice:.4f}")
    print(f"  - Accuracy: {val_acc:.4f}")
    print(f"\nUrmatori pasi:")
    print(f"1. Copiaza 'card_segmentation_480.tflite' in:")
    print(f"   app/src/main/assets/models/card_segmentation.tflite")
    print(f"2. Rebuildeaza aplicatia Android")
    print(f"3. Testeaza cu poze noi!")
    print(f"========================================")














