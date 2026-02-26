"""
Script de test pentru a aplica măștile pe pozele originale
și a vedea rezultatul (cartonașul extras pe fundal alb)
"""

import os
from PIL import Image
import numpy as np

def apply_mask_to_image(image_path, mask_path, output_path):
    """
    Aplică masca pe imagine și extrage cartonașul pe fundal alb
    """
    # Încarcă imaginea originală
    image = Image.open(image_path).convert('RGB')
    image_array = np.array(image)
    
    # Încarcă masca
    mask = Image.open(mask_path).convert('L')
    mask_array = np.array(mask)
    
    # Redimensionează masca dacă e necesar
    if mask_array.shape != image_array.shape[:2]:
        mask = mask.resize((image_array.shape[1], image_array.shape[0]), Image.LANCZOS)
        mask_array = np.array(mask)
    
    # Creează imaginea rezultat (fundal alb)
    result_array = np.ones_like(image_array) * 255  # Fundal alb
    
    # Aplică masca: unde masca este alb (255), pune pixelul din imagine
    # Unde masca este negru (0), păstrează fundalul alb
    mask_binary = (mask_array > 127).astype(np.uint8)  # Binarizează masca
    
    for c in range(3):  # Pentru fiecare canal RGB
        result_array[:, :, c] = (
            image_array[:, :, c] * mask_binary +
            result_array[:, :, c] * (1 - mask_binary)
        ).astype(np.uint8)
    
    # Salvează rezultatul
    result_image = Image.fromarray(result_array)
    result_image.save(output_path)
    print(f"✅ Rezultat salvat: {output_path}")

def main():
    """
    Funcția principală
    """
    print("=" * 60)
    print("🧪 Test Măști - Aplicare pe Poze Originale")
    print("=" * 60)
    
    # Configurare căi
    IMAGES_DIR = "images"
    MASKS_DIR = "masks"
    OUTPUT_DIR = "test_results"
    
    # Creează folderul pentru rezultate
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    # Verifică că folderele există
    if not os.path.exists(IMAGES_DIR):
        print(f"❌ Eroare: Folderul cu imagini nu există: {IMAGES_DIR}")
        return
    
    if not os.path.exists(MASKS_DIR):
        print(f"❌ Eroare: Folderul cu măști nu există: {MASKS_DIR}")
        return
    
    # Găsește toate imaginile
    image_files = [f for f in os.listdir(IMAGES_DIR) if f.lower().endswith(('.jpg', '.jpeg', '.png'))]
    
    if not image_files:
        print(f"❌ Eroare: Nu s-au găsit imagini în: {IMAGES_DIR}")
        return
    
    print(f"\n📁 Găsite {len(image_files)} imagini")
    
    # Procesează fiecare imagine
    processed = 0
    for img_file in image_files:
        image_path = os.path.join(IMAGES_DIR, img_file)
        
        # Găsește masca corespunzătoare
        mask_name = os.path.splitext(img_file)[0] + '.png'
        mask_path = os.path.join(MASKS_DIR, mask_name)
        
        if not os.path.exists(mask_path):
            print(f"⚠️ Mască lipsă pentru {img_file}, ignorat")
            continue
        
        # Creează numele fișierului de output
        output_name = f"result_{os.path.splitext(img_file)[0]}.png"
        output_path = os.path.join(OUTPUT_DIR, output_name)
        
        # Aplică masca
        try:
            apply_mask_to_image(image_path, mask_path, output_path)
            processed += 1
        except Exception as e:
            print(f"❌ Eroare la procesarea {img_file}: {e}")
    
    print("\n" + "=" * 60)
    print(f"✅ Test complet!")
    print(f"   Imagini procesate: {processed} din {len(image_files)}")
    print(f"   Rezultate salvate în: {OUTPUT_DIR}")
    print("=" * 60)
    print("\n💡 Deschide folderul 'test_results' pentru a vedea rezultatele!")
    print("   Ar trebui să vezi cartonașele extrase pe fundal alb.")

if __name__ == "__main__":
    main()














