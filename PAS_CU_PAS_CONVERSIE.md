# 📋 Pași Detaliați: Conversie JSON → PNG Masks

## 🎯 Ce Vrem Să Facem

Transformăm JSON-ul exportat din MakeSense.ai în **măști PNG** (imagini alb-negru) care arată exact unde este cartonașul în fiecare poză.

---

## 📁 PASUL 1: Organizează Fișierele

### 1.1. Creează un Folder Nou

1. Deschide **File Explorer** (Exploratorul de fișiere)
2. Mergi pe **Desktop** (sau oriunde vrei)
3. Click dreapta → **New** → **Folder**
4. Numește-l: `test_tflite` (sau orice nume vrei)

### 1.2. Copiază Pozele Originale

1. Găsește pozele tale originale (cele 4 poze: mainline scurt, lung, premium, silver series)
2. **Copiază** pozele (Ctrl+C)
3. Mergi în folderul `test_tflite` pe care l-ai creat
4. **Creează un folder nou** înăuntru numit `images`
5. **Intră** în folderul `images`
6. **Lipește** pozele acolo (Ctrl+V)

**Rezultat:** Ar trebui să ai:
```
test_tflite/
└── images/
    ├── 1.jpg  (sau cum se numesc pozele tale)
    ├── 2.jpg
    ├── 3.jpg
    └── 4.jpg
```

**⚠️ IMPORTANT:** Notează **exact** cum se numesc pozele tale! (ex: `IMG_001.jpg`, `poza1.jpg`, etc.)

### 1.3. Copiază JSON-urile

1. Găsește **toate** fișierele JSON exportate din MakeSense.ai (ai 4 JSON-uri, unul pentru fiecare poză)
2. **Copiază** toate JSON-urile
3. Mergi în folderul `test_tflite` (nu în `images`, ci în `test_tflite`)
4. **Lipește** toate JSON-urile acolo

**⚠️ IMPORTANT:** Nu trebuie să redenumești JSON-urile! Lasă-le cu numele original.

**Rezultat:** Ar trebui să ai:
```
test_tflite/
├── images/
│   ├── 1.jpg
│   ├── 2.jpg
│   ├── 3.jpg
│   └── 4.jpg
├── 1.json  (sau cum se numesc JSON-urile tale)
├── 2.json
├── 3.json
└── 4.json
```

**💡 Tips:** Dacă JSON-urile au nume diferite (ex: `IMG_001.json`), e OK! Scriptul le va găsi automat.

---

## 🐍 PASUL 2: Instalează Python Libraries

### 2.1. Deschide PowerShell

1. Apasă **Windows Key** (tasta Windows)
2. Scrie: `powershell`
3. Click pe **Windows PowerShell** (sau **Terminal**)

### 2.2. Instalează Librăriile

**Scrie exact** (și apasă Enter după fiecare linie):

```bash
py -m pip install pillow
```

Așteaptă să se instaleze (va scrie "Successfully installed...").

Apoi:

```bash
py -m pip install numpy
```

Așteaptă din nou.

**✅ Dacă vezi "Successfully installed"** → totul e bine!

**❌ Dacă vezi erori** → trimite-mi mesajul de eroare.

---

## 📝 PASUL 3: Copiază Scriptul

### 3.1. Găsește Scriptul

1. Mergi în folderul proiectului: `C:\Users\Andrei\StudioProjects\hotwheels-collector`
2. Găsește fișierul: `convert_coco_to_masks.py`
3. **Copiază** fișierul (Ctrl+C)

### 3.2. Lipește Scriptul în Folderul Test

1. Mergi în folderul `test_tflite` pe care l-ai creat
2. **Lipește** scriptul acolo (Ctrl+V)

**Rezultat:** Ar trebui să ai:
```
test_tflite/
├── images/
│   ├── 1.jpg
│   ├── 2.jpg
│   ├── 3.jpg
│   └── 4.jpg
├── annotations.json
└── convert_coco_to_masks.py
```

---

## 🔧 PASUL 4: Modifică Scriptul (Dacă E Necesar)

### 4.1. Deschide Scriptul

1. Click dreapta pe `convert_coco_to_masks.py`
2. **Open with** → **Notepad** (sau orice editor de text)

### 4.2. Verifică Căile

Caută în script (aproape de final, în funcția `main()`) aceste linii:

```python
JSON_DIR = "."  # Folderul cu JSON-urile
IMAGES_DIR = "images"
OUTPUT_MASKS_DIR = "masks"
```

**✅ Dacă sunt exact așa** → nu trebuie să modifici nimic!

**💡 Scriptul va găsi automat toate JSON-urile** din folder, nu trebuie să le redenumești!

---

## ▶️ PASUL 5: Rulează Scriptul

### 5.1. Deschide PowerShell în Folderul Corect

**Opțiunea 1 (Ușor):**
1. Mergi în folderul `test_tflite` în File Explorer
2. Click în bara de adresă (unde scrie calea)
3. Șterge tot și scrie: `powershell`
4. Apasă Enter

**Opțiunea 2 (Manual):**
1. Deschide PowerShell
2. Scrie: `cd C:\Users\Andrei\Desktop\test_tflite` (sau calea reală către folderul tău)
3. Apasă Enter

### 5.2. Rulează Scriptul

**Scrie exact:**

```bash
py convert_coco_to_masks.py
```

Apasă Enter.

### 5.3. Ce Ar Trebui Să Vezi

**✅ Dacă merge bine:**
```
============================================================
🔄 Conversie JSON MakeSense.ai → PNG Masks
============================================================

📁 Găsite 4 fișiere JSON:
   - 1.json
   - 2.json
   - 3.json
   - 4.json

📖 Procesare: 1.json
   ✅ Mască creată: 1.png
📖 Procesare: 2.json
   ✅ Mască creată: 2.png
📖 Procesare: 3.json
   ✅ Mască creată: 3.png
📖 Procesare: 4.json
   ✅ Mască creată: 4.png

🎉 Total măști create: 4
```

**❌ Dacă vezi erori:**
- Copiază **tot** mesajul de eroare
- Trimite-mi-l și te ajut să rezolv

---

## ✅ PASUL 6: Verifică Rezultatele

### 6.1. Verifică Că S-a Creat Folderul `masks`

1. Mergi în folderul `test_tflite`
2. Ar trebui să vezi un folder nou: `masks`
3. **Intră** în folderul `masks`

### 6.2. Verifică Măștile

Ar trebui să vezi 4 fișiere PNG:
- `1.png`
- `2.png`
- `3.png`
- `4.png`

### 6.3. Deschide o Mască

1. **Click dublu** pe `1.png`
2. Ar trebui să vezi:
   - **Fundal NEGRU** (sau gri închis)
   - **Cartonaș ALB** (sau gri deschis)

**✅ Dacă vezi alb pe negru** → PERFECT! Măștile sunt corecte!

**❌ Dacă vezi altceva** (ex: toate negru, toate alb, culori) → trimite-mi o poză cu masca și rezolvăm.

---

## 🎯 Rezultat Final

După toți pașii, ar trebui să ai:

```
test_tflite/
├── images/
│   ├── 1.jpg
│   ├── 2.jpg
│   ├── 3.jpg
│   └── 4.jpg
├── masks/
│   ├── 1.png  (mască albă pe negru)
│   ├── 2.png
│   ├── 3.png
│   └── 4.png
├── 1.json
├── 2.json
├── 3.json
├── 4.json
└── convert_coco_to_masks.py
```

---

## ❓ Dacă Ai Probleme

### Problema: "Nu s-au găsit fișiere JSON"
**Soluție:** Verifică că:
- JSON-urile sunt în același folder cu scriptul (nu în `images/`)
- JSON-urile au extensia `.json` (nu `.txt` sau altceva)

### Problema: "Imaginea nu există"
**Soluție:** Verifică că:
- Pozele sunt în folderul `images/`
- Numele din JSON se potrivesc cu numele pozelor

### Problema: "Nu s-a găsit poligon"
**Soluție:** Verifică că ai folosit **Polygon tool** (nu Rectangle) în MakeSense.ai.

---

## 📞 Următorul Pas

După ce ai măștile corecte:
1. ✅ **Testează** că funcționează (ai 4 măști)
2. ✅ **Continuă anotarea** restului pozelor (până la 200-500)
3. ✅ **Antrenăm modelul TFLite** (când ai suficiente poze)

---

**Succes! 🚀**

**Dacă te blochezi la orice pas, spune-mi exact la ce pas ești și ce vezi!**

