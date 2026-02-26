"""
Script pentru conversia JSON COCO (din MakeSense.ai) la PNG masks
Pentru segmentarea cartonașelor Hot Wheels cu TFLite
"""

import json
import os
import numpy as np
from PIL import Image, ImageDraw

def create_mask_from_polygon(image_width, image_height, polygon_points):
    """
    Creează o mască PNG din coordonatele poligonului
    
    Args:
        image_width: Lățimea imaginii originale
        image_height: Înălțimea imaginii originale
        polygon_points: Listă de coordonate [x1, y1, x2, y2, ...] sau [[x1,y1], [x2,y2], ...]
    
    Returns:
        PIL Image cu masca (alb pe negru)
    """
    # Creează imagine neagră (fundal)
    mask = Image.new('L', (image_width, image_height), 0)
    draw = ImageDraw.Draw(mask)
    
    # Convertește coordonatele la formatul corect
    points = []
    
    if len(polygon_points) == 0:
        print(f"   ⚠️ Poligon gol!")
        return mask
    
    # Verifică dacă este format COCO: [[x1, y1, x2, y2, ...]] (listă cu un singur element)
    if isinstance(polygon_points[0], (list, tuple)) and len(polygon_points) == 1:
        # Extrage primul element (lista de coordonate plate)
        flat_coords = polygon_points[0]
        # Procesează ca listă plată: [x1, y1, x2, y2, ...]
        for i in range(0, len(flat_coords), 2):
            if i + 1 < len(flat_coords):
                x = float(flat_coords[i])
                y = float(flat_coords[i + 1])
                points.append((int(x), int(y)))
    elif isinstance(polygon_points[0], (int, float)):
        # Format: [x1, y1, x2, y2, ...] - array plat
        for i in range(0, len(polygon_points), 2):
            if i + 1 < len(polygon_points):
                x = float(polygon_points[i])
                y = float(polygon_points[i + 1])
                points.append((int(x), int(y)))
    elif isinstance(polygon_points[0], (list, tuple)):
        # Format: [[x1,y1], [x2,y2], ...]
        for p in polygon_points:
            if isinstance(p, (list, tuple)) and len(p) >= 2:
                x = float(p[0])
                y = float(p[1])
                points.append((int(x), int(y)))
    elif isinstance(polygon_points[0], dict):
        # Format: [{"x": 1, "y": 2}, ...]
        for p in polygon_points:
            if isinstance(p, dict):
                if 'x' in p and 'y' in p:
                    points.append((int(p['x']), int(p['y'])))
                elif 0 in p and 1 in p:
                    points.append((int(p[0]), int(p[1])))
    else:
        print(f"   ⚠️ Format necunoscut pentru polygon_points: {type(polygon_points[0])}")
        print(f"   📝 Primul element: {polygon_points[0]}")
        return mask
    
    # Desenează poligonul alb (cartonașul)
    if len(points) >= 3:  # Minim 3 puncte pentru un poligon
        draw.polygon(points, fill=255)  # 255 = alb (cartonașul)
        print(f"   ✅ Poligon desenat cu {len(points)} puncte")
    else:
        print(f"   ⚠️ Poligon cu mai puțin de 3 puncte ({len(points)}), ignorat")
    
    return mask

def process_single_json(json_path, images_dir, output_masks_dir):
    """
    Procesează un singur fișier JSON și creează masca corespunzătoare
    """
    print(f"\n📖 Procesare: {os.path.basename(json_path)}")
    
    try:
        # Încarcă JSON-ul
        with open(json_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # Găsește numele imaginii din JSON (format COCO)
        image_filename = None
        image_id = None
        json_name = os.path.splitext(os.path.basename(json_path))[0]
        
        # Format COCO: caută în 'images'
        if 'images' in data and len(data['images']) > 0:
            print(f"   🔍 Număr de imagini în JSON: {len(data['images'])}")
            # Caută imaginea care se potrivește cu numele JSON-ului
            for img_info in data['images']:
                if 'file_name' in img_info:
                    file_name_base = os.path.splitext(img_info['file_name'])[0]
                    if file_name_base == json_name:
                        image_filename = img_info['file_name']
                        image_id = img_info.get('id')
                        print(f"   ✅ Găsită imagine în JSON: {image_filename} (id: {image_id})")
                        break
            
            # Dacă nu găsește, folosește prima imagine
            if not image_filename and len(data['images']) > 0:
                image_info = data['images'][0]
                if 'file_name' in image_info:
                    image_filename = image_info['file_name']
                    image_id = image_info.get('id')
                    print(f"   ⚠️ Folosită prima imagine din JSON: {image_filename} (id: {image_id})")
        
        # Dacă nu găsește, încearcă să găsească după numele JSON-ului
        if not image_filename:
            if os.path.exists(images_dir):
                for img_file in os.listdir(images_dir):
                    if os.path.splitext(img_file)[0] == json_name:
                        image_filename = img_file
                        print(f"   ✅ Găsită imagine după nume: {image_filename}")
                        break
        
        if not image_filename:
            print(f"   ⚠️ Nu s-a găsit numele imaginii în JSON")
            print(f"   📝 Structura JSON: {list(data.keys())}")
            return False
        
        # Calea către imagine
        image_path = os.path.join(images_dir, image_filename)
        
        if not os.path.exists(image_path):
            # Încearcă să găsească orice imagine cu nume similar
            for ext in ['.jpg', '.jpeg', '.png', '.JPG', '.JPEG', '.PNG']:
                possible_path = os.path.join(images_dir, json_name + ext)
                if os.path.exists(possible_path):
                    image_path = possible_path
                    image_filename = json_name + ext
                    print(f"   ✅ Găsită imagine: {image_filename}")
                    break
            else:
                print(f"   ⚠️ Imaginea nu există: {image_path}")
                return False
        
        # Obține dimensiunile imaginii
        img = Image.open(image_path)
        image_width, image_height = img.size
        print(f"   📐 Dimensiuni imagine: {image_width}x{image_height}")
        
        # Găsește poligonul în JSON (format COCO)
        polygon = None
        
        # Format COCO: caută în 'annotations' annotation-ul care corespunde cu imaginea
        if 'annotations' in data and len(data['annotations']) > 0:
            print(f"   🔍 Număr de anotări: {len(data['annotations'])}")
            
            # Caută annotation-ul care corespunde cu image_id
            found_annotation = None
            if image_id is not None:
                for ann in data['annotations']:
                    if ann.get('image_id') == image_id:
                        found_annotation = ann
                        print(f"   ✅ Găsită anotare pentru image_id: {image_id}")
                        break
            
            # Dacă nu găsește după image_id, folosește prima anotare
            if not found_annotation:
                found_annotation = data['annotations'][0]
                print(f"   ⚠️ Folosită prima anotare (image_id: {found_annotation.get('image_id', 'N/A')})")
            
            annotation = found_annotation
            print(f"   🔍 Chei în annotation: {list(annotation.keys())}")
            
            if 'segmentation' in annotation:
                segmentation = annotation['segmentation']
                print(f"   🔍 Tip segmentation: {type(segmentation)}, lungime: {len(segmentation) if isinstance(segmentation, list) else 'N/A'}")
                
                # COCO format: segmentation este o listă de poligoane
                # Primul poligon este lista de coordonate plate
                if isinstance(segmentation, list) and len(segmentation) > 0:
                    polygon = segmentation[0]  # Primul poligon
                    print(f"   ✅ Poligon găsit în 'annotations[].segmentation[0]'")
                    # DEBUG: Afișează primele coordonate pentru a verifica dacă sunt diferite
                    if len(polygon) >= 4:
                        print(f"   🔍 Primele coordonate poligon: [{polygon[0]:.1f}, {polygon[1]:.1f}, {polygon[2]:.1f}, {polygon[3]:.1f}]")
                else:
                    print(f"   ⚠️ segmentation nu este o listă sau este goală")
            else:
                print(f"   ⚠️ Nu există 'segmentation' în annotation")
        
        if not polygon:
            print(f"   ⚠️ Nu s-a găsit poligon în JSON")
            print(f"   📝 Chei disponibile: {list(data.keys())}")
            if 'annotations' in data and len(data['annotations']) > 0:
                print(f"   📝 Structura annotations[0]: {list(data['annotations'][0].keys())}")
            return False
        
        # DEBUG: Verifică formatul poligonului
        print(f"   🔍 Tip poligon: {type(polygon)}")
        if polygon and len(polygon) > 0:
            print(f"   🔍 Primul element: {polygon[0] if len(polygon) > 0 else 'N/A'}, tip: {type(polygon[0]) if len(polygon) > 0 else 'N/A'}")
            print(f"   🔍 Număr elemente: {len(polygon)}")
            if isinstance(polygon[0], (list, tuple)):
                print(f"   🔍 Elemente în primul element: {len(polygon[0]) if len(polygon) > 0 else 0}")
        
        # Creează masca
        mask = create_mask_from_polygon(image_width, image_height, polygon)
        
        # Salvează masca (folosește numele JSON-ului ca bază pentru a evita conflicte)
        mask_filename = json_name + '.png'
        mask_path = os.path.join(output_masks_dir, mask_filename)
        mask.save(mask_path)
        
        print(f"   ✅ Mască creată: {mask_filename}")
        return True
        
    except Exception as e:
        print(f"   ❌ Eroare la procesare: {e}")
        import traceback
        traceback.print_exc()
        return False

def verify_masks(images_dir, masks_dir):
    """
    Verifică că măștile sunt corecte
    """
    print("\n🔍 Verificare măști...")
    
    image_files = [f for f in os.listdir(images_dir) if f.lower().endswith(('.jpg', '.jpeg', '.png'))]
    mask_files = [f for f in os.listdir(masks_dir) if f.lower().endswith('.png')]
    
    print(f"   Imagini: {len(image_files)}")
    print(f"   Măști: {len(mask_files)}")
    
    # Verifică că fiecare imagine are mască
    missing_masks = []
    for img_file in image_files:
        mask_file = os.path.splitext(img_file)[0] + '.png'
        if mask_file not in mask_files:
            missing_masks.append(img_file)
    
    if missing_masks:
        print(f"⚠️ Măști lipsă pentru: {missing_masks}")
    else:
        print("✅ Toate imaginile au măști!")
    
    # Verifică că măștile sunt corecte (alb pe negru)
    for mask_file in mask_files[:5]:  # Verifică primele 5
        mask_path = os.path.join(masks_dir, mask_file)
        mask = Image.open(mask_path)
        mask_array = np.array(mask)
        
        unique_values = np.unique(mask_array)
        if len(unique_values) == 2 and 0 in unique_values and 255 in unique_values:
            white_pixels = np.sum(mask_array == 255)
            total_pixels = mask_array.size
            percentage = (white_pixels / total_pixels) * 100
            print(f"✅ {mask_file}: Mască binară corectă (negru + alb, {percentage:.1f}% alb)")
        else:
            print(f"⚠️ {mask_file}: Mască poate avea probleme (valori: {unique_values})")

def main():
    """
    Funcția principală
    """
    print("=" * 60)
    print("🔄 Conversie JSON MakeSense.ai → PNG Masks")
    print("=" * 60)
    
    # Configurare căi (modifică aici!)
    JSON_DIR = "."  # Folderul cu JSON-urile (același cu scriptul)
    IMAGES_DIR = "images"  # Directorul cu imaginile originale
    OUTPUT_MASKS_DIR = "masks"  # Directorul unde se salvează măștile
    
    # Verifică că directorul cu imagini există
    if not os.path.exists(IMAGES_DIR):
        print(f"❌ Eroare: Directorul cu imagini nu există: {IMAGES_DIR}")
        print("\n📝 Creează directorul 'images' și pune pozele acolo")
        return
    
    # Creează directorul pentru măști
    os.makedirs(OUTPUT_MASKS_DIR, exist_ok=True)
    
    # Găsește toate JSON-urile
    json_files = [f for f in os.listdir(JSON_DIR) if f.lower().endswith('.json')]
    
    if not json_files:
        print(f"❌ Eroare: Nu s-au găsit fișiere JSON în: {JSON_DIR}")
        print("\n📝 Asigură-te că JSON-urile sunt în același folder cu scriptul")
        return
    
    print(f"\n📁 Găsite {len(json_files)} fișiere JSON:")
    for json_file in json_files:
        print(f"   - {json_file}")
    
    # Procesează fiecare JSON
    masks_created = 0
    for json_file in json_files:
        json_path = os.path.join(JSON_DIR, json_file)
        if process_single_json(json_path, IMAGES_DIR, OUTPUT_MASKS_DIR):
            masks_created += 1
    
    # Verifică rezultatele
    if masks_created > 0:
        verify_masks(IMAGES_DIR, OUTPUT_MASKS_DIR)
        
        print("\n" + "=" * 60)
        print("✅ Conversie completă!")
        print(f"   Măști create: {masks_created} din {len(json_files)} JSON-uri")
        print(f"   Salvate în: {OUTPUT_MASKS_DIR}")
        print("=" * 60)
    else:
        print("\n⚠️ Nu s-au creat măști. Verifică formatul JSON-urilor.")
        print("\n💡 Tips:")
        print("   - Asigură-te că ai folosit Polygon tool în MakeSense.ai")
        print("   - Verifică că numele JSON-urilor se potrivesc cu numele pozelor")
        print("   - Deschide un JSON în Notepad și verifică structura")
            
if __name__ == "__main__":
    main()
