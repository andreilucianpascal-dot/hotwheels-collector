# Alternative Gratuite pentru Anotare (dacă LabelMe nu funcționează)

## 🆓 Toate Acestea Sunt 100% Gratuite!

---

## 1. **CVAT** (Computer Vision Annotation Tool)

### Instalare:
```bash
# Opțiunea 1: Docker (recomandat)
docker run -d -p 8080:8080 openvino/cvat

# Opțiunea 2: Instalare locală
git clone https://github.com/opencv/cvat
cd cvat
docker-compose up -d
```

### Acces:
- Deschide browser: `http://localhost:8080`
- Creează cont (gratuit)
- Upload poze și anotează

### Avantaje:
- ✅ 100% gratuit
- ✅ Interfață web (nu necesită instalare locală)
- ✅ Export direct PNG masks
- ✅ Suport pentru poligoane complexe

---

## 2. **Roboflow** (Cloud-based, cu plan gratuit)

### Acces:
- Website: https://roboflow.com/
- Creează cont gratuit
- Upload poze și anotează online

### Plan Gratuit:
- ✅ 1,000 de imagini
- ✅ Export PNG masks
- ✅ Suport pentru poligoane

### Limitări:
- 1,000 imagini (suficient pentru testul tău cu 50!)
- Necesită cont (gratuit)

---

## 3. **VGG Image Annotator (VIA)** - Simplu și Rapid

### Instalare:
- Website: https://www.robots.ox.ac.uk/~vgg/software/via/
- Download: https://www.robots.ox.ac.uk/~vgg/software/via/downloads/via.html
- **NU necesită instalare** - rulează direct în browser!

### Utilizare:
1. Deschide `via.html` în browser
2. Upload poze
3. Anotează cu poligoane
4. Export JSON → convertește la PNG (vezi script mai jos)

### Avantaje:
- ✅ 100% gratuit
- ✅ Nu necesită instalare
- ✅ Rulează în browser
- ✅ Foarte simplu de folosit

---

## 4. **Photoshop/GIMP** (Manual, dar precis)

### Dacă ai deja Photoshop sau GIMP:

**Proces:**
1. Deschide imaginea
2. Folosește **Pen Tool** pentru a desena conturul
3. Creează selecție din path
4. Fill selecția cu alb
5. Inversă selecția → fill cu negru
6. Export ca PNG

**Script Python pentru batch processing:**
```python
# Dacă ai exportat măștile ca PNG manual
# Verifică că sunt corecte (alb pe negru)
```

---

## 5. **MakeSense.ai** - Cel Mai Simplu!

### Acces:
- Website: https://www.makesense.ai/
- **NU necesită instalare** - rulează direct în browser!
- **NU necesită cont** - 100% gratuit!

### Utilizare:
1. Deschide https://www.makesense.ai/
2. Click "Get Started"
3. Upload poze
4. Anotează cu poligoane
5. Export ca PNG masks

### Avantaje:
- ✅ 100% gratuit
- ✅ Nu necesită instalare
- ✅ Nu necesită cont
- ✅ Foarte simplu
- ✅ Export direct PNG

---

## 🎯 Recomandare pentru Tine

### Dacă LabelMe nu funcționează:

**Opțiunea 1: MakeSense.ai** (Cel mai simplu!)
- Deschide https://www.makesense.ai/
- Upload poze
- Anotează
- Export PNG

**Opțiunea 2: VIA (VGG Image Annotator)**
- Download `via.html`
- Rulează în browser
- Anotează
- Export JSON → convertează la PNG

**Opțiunea 3: Roboflow** (dacă vrei cloud)
- Cont gratuit
- 1,000 imagini (suficient pentru test)
- Export PNG

---

## 📝 Script Python pentru Conversie (dacă ai JSON din VIA)

Dacă folosești VIA și ai JSON-uri, poți converti la PNG:

```python
import json
import numpy as np
from PIL import Image, ImageDraw

def via_json_to_png(json_path, output_png_path, image_width, image_height):
    """Convertește JSON din VIA la PNG mask"""
    with open(json_path, 'r') as f:
        data = json.load(f)
    
    # Creează mască albă pe negru
    mask = Image.new('RGB', (image_width, image_height), (0, 0, 0))
    draw = ImageDraw.Draw(mask)
    
    # Desenează poligoanele ca alb
    for region in data.get('regions', []):
        if 'shape_attributes' in region:
            points = region['shape_attributes'].get('all_points_x', [])
            y_points = region['shape_attributes'].get('all_points_y', [])
            polygon = list(zip(points, y_points))
            draw.polygon(polygon, fill=(255, 255, 255))
    
    mask.save(output_png_path)
    print(f"✅ Converted {json_path} → {output_png_path}")

# Folosește:
# via_json_to_png('1.json', 'masks/1.png', 1920, 1080)
```

---

## ✅ Verificare Finală

Indiferent de tool-ul folosit, verifică că:
- ✅ Măștile sunt PNG
- ✅ Fundal negru (0,0,0)
- ✅ Cartonaș alb (255,255,255)
- ✅ Numele se potrivesc: `1.jpg` → `1.png`

---

**Concluzie:** Există multe alternative gratuite! MakeSense.ai este probabil cea mai simplă dacă LabelMe nu funcționează. 🚀
















