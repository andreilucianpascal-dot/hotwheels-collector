# Primul Pas: Anotarea Imaginilor cu LabelMe

## 📋 Ce Ai Nevoie
- ✅ Python instalat
- ✅ 50 poze (10 per categorie × 5 categorii)
- ✅ LabelMe (tool pentru anotare)

---

## 🚀 Pasul 1: Instalează LabelMe (GRATUIT - Open Source)

⚠️ **IMPORTANT**: Există două versiuni:
- ✅ **LabelMe open-source** (GRATUIT) - de pe GitHub, instalat cu pip
- ❌ **Labelme.io** (COMERCIAL) - versiunea comercială (NU folosi asta!)

**Folosește versiunea GRATUITĂ:**

Deschide terminalul/command prompt și rulează:

```bash
pip install labelme
```

**Verifică instalarea:**
```bash
labelme --version
```

Dacă vezi o versiune (ex: `5.2.0`), e instalat corect!

**Dacă întâmpini probleme:**
```bash
# Reinstalează complet
pip uninstall labelme
pip install labelme
```

**Sursa oficială gratuită:**
- GitHub: https://github.com/wkentaro/labelme
- Este 100% gratuit și open-source!

---

## 📁 Pasul 2: Organizează Pozele

Creează structura de foldere:

```
dataset/
├── images/
│   ├── 1.jpg
│   ├── 2.jpg
│   ├── 3.jpg
│   └── ... (toate cele 50 de poze)
└── masks/
    (aici vor apărea măștile după anotare)
```

**Important:**
- Toate pozele în folderul `images/`
- Numele fișierelor: `1.jpg`, `2.jpg`, `3.jpg`, etc. (numerotare consecutivă)
- Folderul `masks/` va fi creat automat de LabelMe

---

## 🎨 Pasul 3: Deschide LabelMe

În terminal, navighează la folderul cu pozele:

```bash
cd C:\calea\către\dataset\images
labelme
```

SAU deschide direct folderul:

```bash
labelme C:\calea\către\dataset\images
```

**LabelMe se va deschide cu interfața grafică!**

---

## ✏️ Pasul 4: Anotează Prima Imagine

### Cum să Anotezi:

1. **Deschide prima imagine** (click pe `1.jpg` în listă)

2. **Selectează "Create Polygons"** (butonul cu poligon în toolbar)

3. **Desenează conturul cartonașului:**
   - Click pe fiecare colț/punct important al cartonașului
   - Urmează **exact** marginea cartonașului (nu mai mult, nu mai puțin!)
   - Pentru colțuri tăiate sau decupaje, fă click-uri mai dese
   - Când ai terminat conturul, apasă **Enter** sau click dreapta → "Finish"

4. **Salvează anotarea:**
   - Apasă **Ctrl+S** sau File → Save
   - Se va crea automat `1.json` în același folder

5. **Export ca PNG mask:**
   - File → Export as PNG mask
   - Sau folosește comanda: `labelme_json_to_dataset 1.json -o masks/`
   - Masca va fi salvată în `masks/1.png`

### ⚠️ Reguli Importante:

- ✅ **Precizie maximă**: Urmează exact marginea cartonașului
- ✅ **Forme complexe**: Folosește poligoane (nu dreptunghiuri) pentru colțuri tăiate
- ✅ **Fundal negru**: Zonele care NU sunt cartonaș = negru (0,0,0)
- ✅ **Cartonaș alb**: Zona care ESTE cartonaș = alb (255,255,255)
- ❌ **NU** include fundalul în mască
- ❌ **NU** lasă zone goale în interiorul cartonașului

---

## 🔄 Pasul 5: Anotează Toate Pozele

Repetă procesul pentru toate cele 50 de poze:

1. Deschide imaginea
2. Desenează conturul cu poligon
3. Salvează anotarea (Ctrl+S)
4. Export ca PNG mask
5. Treci la următoarea

**Tips pentru viteză:**
- Folosește **scurtăturile tastaturii** (W pentru polygon tool)
- După ce salvezi, LabelMe trece automat la următoarea imagine
- Poți folosi **batch export** după ce ai terminat toate anotările

---

## 📦 Pasul 6: Batch Export (Opțional - Mai Rapid)

După ce ai anotat toate pozele, poți exporta toate măștile dintr-o dată:

```bash
# Navighează în folderul cu JSON-urile
cd C:\calea\către\dataset\images

# Export toate JSON-urile ca PNG masks
for %f in (*.json) do labelme_json_to_dataset "%f" -o masks/
```

**Sau folosește Python script:**

Creează `export_masks.py`:
```python
import os
import subprocess
import glob

# Folder cu JSON-urile
json_folder = r"C:\calea\către\dataset\images"
masks_folder = r"C:\calea\către\dataset\masks"

# Creează folderul masks dacă nu există
os.makedirs(masks_folder, exist_ok=True)

# Export toate JSON-urile
json_files = glob.glob(os.path.join(json_folder, "*.json"))
for json_file in json_files:
    print(f"Exporting {json_file}...")
    subprocess.run([
        "labelme_json_to_dataset",
        json_file,
        "-o", masks_folder
    ])
    
print(f"✅ Exported {len(json_files)} masks!")
```

Rulează:
```bash
python export_masks.py
```

---

## ✅ Verificare Finală

După anotare, verifică că ai:

1. ✅ **50 de imagini** în `dataset/images/` (1.jpg ... 50.jpg)
2. ✅ **50 de măști** în `dataset/masks/` (1.png ... 50.png)
3. ✅ **Numele se potrivesc**: `1.jpg` → `1.png`, `2.jpg` → `2.png`, etc.
4. ✅ **Măștile sunt corecte**: 
   - Fundal negru
   - Cartonaș alb
   - Contur precis

### Test Rapid:

Deschide o mască în Paint/Photoshop și verifică:
- Fundalul este negru?
- Cartonașul este alb?
- Conturul este precis (nu are "bucăți" lipsă)?

---

## 🎯 Rezumat - Ce Ai Făcut:

1. ✅ Instalat LabelMe
2. ✅ Organizat pozele în `dataset/images/`
3. ✅ Anotat toate cele 50 de poze (desenat conturul cu poligon)
4. ✅ Exportat măștile ca PNG în `dataset/masks/`
5. ✅ Verificat că numele se potrivesc (1.jpg → 1.png)

---

## 📝 Notă Importantă

**Pentru testare cu 50 de poze:**
- Va funcționa, dar precizia va fi limitată
- Pentru producție, ai nevoie de 200-500+ poze
- Dar pentru testare și a vedea dacă procesul funcționează, 50 de poze sunt suficiente!

**Următorul pas** (după anotare) va fi antrenarea modelului în Python.

---

## ❓ Probleme Comune

### LabelMe nu se deschide:
```bash
# Reinstalează
pip uninstall labelme
pip install labelme
```

### Eroare la export:
- Verifică că ai instalat `labelme` complet
- Încearcă: `pip install labelme[all]`

### Măștile nu se potrivesc:
- Verifică că numele fișierelor se potrivesc exact
- JSON-ul trebuie să aibă același nume ca imaginea (ex: `1.jpg` → `1.json`)

---

**Succes cu anotarea! 🚀**

