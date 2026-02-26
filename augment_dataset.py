"""
Data Augmentation Script pentru Dataset TFLite
Inmulteste cele 48 imagini la 480 (x10) prin aplicarea de transformari automate
"""

import os
import cv2
import numpy as np
from PIL import Image
import albumentations as A

def create_augmentation_pipeline():
    """
    Creeaza pipeline-ul de augmentation cu transformari variate
    """
    return A.Compose([
        # Rotatie usoara
        A.Rotate(limit=15, p=0.7),
        
        # Flip orizontal (50% sansa)
        A.HorizontalFlip(p=0.5),
        
        # Ajustare luminozitate si contrast
        A.RandomBrightnessContrast(
            brightness_limit=0.3,
            contrast_limit=0.3,
            p=0.8
        ),
        
        # Zoom in/out usor
        A.RandomScale(scale_limit=0.2, p=0.5),
        
        # Translatii (shift)
        A.ShiftScaleRotate(
            shift_limit=0.1,
            scale_limit=0,
            rotate_limit=0,
            p=0.5
        ),
        
        # Ajustare culoare (Hue, Saturation)
        A.HueSaturationValue(
            hue_shift_limit=10,
            sat_shift_limit=20,
            val_shift_limit=10,
            p=0.5
        ),
        
        # Blur usor (simuleaza poze neclare)
        A.Blur(blur_limit=3, p=0.3),
        
        # Zgomot (simuleaza poze cu calitate mai slaba)
        A.GaussNoise(var_limit=(5.0, 15.0), p=0.3),
    ])

def augment_dataset(input_images_dir, input_masks_dir, output_images_dir, output_masks_dir, num_augmentations=10):
    """
    Aplica augmentation pe dataset
    
    Args:
        input_images_dir: Director cu imaginile originale (48)
        input_masks_dir: Director cu mastile originale (48)
        output_images_dir: Director unde se salveaza imaginile augmentate
        output_masks_dir: Director unde se salveaza mastile augmentate
        num_augmentations: Cate variante sa genereze pentru fiecare imagine (default: 10)
    """
    
    # Creaza directoarele de output daca nu exista
    os.makedirs(output_images_dir, exist_ok=True)
    os.makedirs(output_masks_dir, exist_ok=True)
    
    # Obtine lista de imagini
    image_files = sorted([f for f in os.listdir(input_images_dir) if f.lower().endswith(('.jpg', '.jpeg', '.png'))])
    
    if not image_files:
        print(f"ERROR: Nu s-au gasit imagini in {input_images_dir}")
        return
    
    print(f"========================================")
    print(f"DATA AUGMENTATION")
    print(f"========================================")
    print(f"Imagini originale: {len(image_files)}")
    print(f"Variante per imagine: {num_augmentations}")
    print(f"Total imagini dupa augmentation: {len(image_files) * (num_augmentations + 1)}")
    print(f"========================================\n")
    
    # Creeaza pipeline-ul de augmentation
    transform = create_augmentation_pipeline()
    
    total_generated = 0
    
    for idx, image_file in enumerate(image_files):
        # Extrage numele de baza (fara extensie)
        base_name = os.path.splitext(image_file)[0]
        
        # Calea completa catre imagine si masca
        image_path = os.path.join(input_images_dir, image_file)
        mask_path = os.path.join(input_masks_dir, f"{base_name}.png")
        
        # Verifica daca exista masca corespunzatoare
        if not os.path.exists(mask_path):
            print(f"ATENTIE: Masca lipseste pentru {image_file}, skip...")
            continue
        
        # Citeste imaginea si masca
        image = cv2.imread(image_path)
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        mask = cv2.imread(mask_path, cv2.IMREAD_GRAYSCALE)
        
        if image is None or mask is None:
            print(f"EROARE: Nu s-a putut citi {image_file} sau masca sa, skip...")
            continue
        
        print(f"[{idx+1}/{len(image_files)}] Procesare: {image_file}")
        
        # Salveaza imaginea si masca originala
        original_image_out = os.path.join(output_images_dir, f"{base_name}_orig.jpg")
        original_mask_out = os.path.join(output_masks_dir, f"{base_name}_orig.png")
        cv2.imwrite(original_image_out, cv2.cvtColor(image, cv2.COLOR_RGB2BGR))
        cv2.imwrite(original_mask_out, mask)
        total_generated += 1
        
        # Genereaza variante augmentate
        for aug_idx in range(num_augmentations):
            # Aplica transformarile (aceeasi transformare pe imagine SI masca)
            augmented = transform(image=image, mask=mask)
            aug_image = augmented['image']
            aug_mask = augmented['mask']
            
            # Salveaza variantele augmentate
            aug_image_out = os.path.join(output_images_dir, f"{base_name}_aug{aug_idx+1}.jpg")
            aug_mask_out = os.path.join(output_masks_dir, f"{base_name}_aug{aug_idx+1}.png")
            
            cv2.imwrite(aug_image_out, cv2.cvtColor(aug_image, cv2.COLOR_RGB2BGR))
            cv2.imwrite(aug_mask_out, aug_mask)
            total_generated += 1
        
        print(f"  -> Generat {num_augmentations + 1} variante (1 orig + {num_augmentations} aug)")
    
    print(f"\n========================================")
    print(f"AUGMENTATION COMPLETAT!")
    print(f"========================================")
    print(f"Total imagini generate: {total_generated}")
    print(f"  - Imagini: {output_images_dir}")
    print(f"  - Masti: {output_masks_dir}")
    print(f"========================================")

def verify_dataset(images_dir, masks_dir):
    """
    Verifica ca fiecare imagine are o masca corespunzatoare
    """
    image_files = sorted([f for f in os.listdir(images_dir) if f.lower().endswith(('.jpg', '.jpeg', '.png'))])
    mask_files = sorted([f for f in os.listdir(masks_dir) if f.lower().endswith('.png')])
    
    print(f"\nVerificare dataset:")
    print(f"  - Imagini gasite: {len(image_files)}")
    print(f"  - Masti gasite: {len(mask_files)}")
    
    if len(image_files) != len(mask_files):
        print(f"  ATENTIE: Numar diferit de imagini si masti!")
        return False
    
    # Verifica ca fiecare imagine are o masca
    missing_masks = []
    for image_file in image_files:
        base_name = os.path.splitext(image_file)[0]
        mask_file = f"{base_name}.png"
        if mask_file not in mask_files:
            missing_masks.append(image_file)
    
    if missing_masks:
        print(f"  EROARE: {len(missing_masks)} imagini nu au masti:")
        for img in missing_masks[:5]:  # Arata primele 5
            print(f"    - {img}")
        return False
    
    print(f"  Verificare OK: Toate imaginile au masti!")
    return True

if __name__ == "__main__":
    # Configurare cai
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # Director input (cele 48 imagini originale)
    input_images_dir = os.path.join(script_dir, "training_48", "images")
    input_masks_dir = os.path.join(script_dir, "training_48", "masks")
    
    # Director output (480 imagini augmentate)
    output_base_dir = os.path.join(script_dir, "training_480")
    output_images_dir = os.path.join(output_base_dir, "images")
    output_masks_dir = os.path.join(output_base_dir, "masks")
    
    # Verifica ca directoarele de input exista
    if not os.path.exists(input_images_dir):
        print(f"EROARE: Directorul {input_images_dir} nu exista!")
        print(f"Asigura-te ca ai structura:")
        print(f"  training_48/")
        print(f"    images/  (48 imagini)")
        print(f"    masks/   (48 masti)")
        exit(1)
    
    if not os.path.exists(input_masks_dir):
        print(f"EROARE: Directorul {input_masks_dir} nu exista!")
        exit(1)
    
    # Ruleaza augmentation
    augment_dataset(
        input_images_dir=input_images_dir,
        input_masks_dir=input_masks_dir,
        output_images_dir=output_images_dir,
        output_masks_dir=output_masks_dir,
        num_augmentations=9  # 9 variante + 1 originala = 10 total per imagine
    )
    
    # Verifica dataset-ul generat
    verify_dataset(output_images_dir, output_masks_dir)
    
    print(f"\nGATA! Acum poti antrena modelul cu:")
    print(f"  py train_tflite_480_masks.py")














