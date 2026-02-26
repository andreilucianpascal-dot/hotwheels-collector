# Cum Să Convertești JSON COCO la PNG Masks

## 📋 Ce Ai Nevoie

1. ✅ **JSON-ul exportat** din MakeSense.ai (COCO format)
2. ✅ **Imaginile originale** (cele 4 poze pe care le-ai anotat)
3. ✅ **Python instalat** (ai deja Python 3.14.2)

---

## 🚀 Pașii

### Pasul 1: Organizează Fișierele

Creează următoarea structură:

```
proiect_tflite/
├── convert_coco_to_masks.py  (scriptul de conversie)
├── annotations.json          (JSON-ul exportat din MakeSense.ai)
├── images/                   (director cu pozele originale)
│   ├── 1.jpg
│   ├── 2.jpg
│   ├── 3.jpg
│   └── 4.jpg
└── masks/                    (va fi creat automat - măștile PNG)
```

### Pasul 2: Instalează Librăriile Necesare

Deschide PowerShell și rulează:

```bash
py -m pip install pillow numpy opencv-python
```

**Explicație:**
- `pillow` = pentru procesarea imaginilor (PIL)
- `numpy` = pentru operații matematice
- `opencv-python` = pentru procesare avansată (opțional, dar util)

### Pasul 3: Rulează Scriptul

```bash
# Navighează la directorul proiectului
cd C:\calea\către\proiect_tflite

# Rulează scriptul
py convert_coco_to_masks.py
```

### Pasul 4: Verifică Rezultatele

După rulare, ar trebui să vezi:

```
✅ Mască creată: 1.png
✅ Mască creată: 2.png
✅ Mască creată: 3.png
✅ Mască creată: 4.png

🎉 Total măști create: 4
```

**Verifică manual:**
- Deschide `masks/1.png` - ar trebui să fie **alb pe negru**
- Fundalul = negru (0,0,0)
- Cartonașul = alb (255,255,255)

---

## 🔧 Dacă Ai Probleme

### Problema 1: "JSON-ul nu pare să fie în format COCO standard"

**Soluție:** MakeSense.ai poate exporta în format ușor diferit. Scriptul încearcă să detecteze automat formatul, dar dacă nu merge:

1. Deschide `annotations.json` în Notepad
2. Verifică structura (ar trebui să vezi `"images"`, `"annotations"`, etc.)
3. Trimite-mi un sample din JSON ca să pot adapta scriptul

### Problema 2: "Imaginea nu există"

**Soluție:** 
- Verifică că numele fișierelor din JSON se potrivesc cu numele din directorul `images/`
- MakeSense.ai poate salva numele diferit - verifică manual

### Problema 3: "Nu s-a găsit poligon"

**Soluție:**
- Verifică că ai folosit **Polygon tool** (nu Rectangle) în MakeSense.ai
- Verifică că ai salvat anotările corect

---

## 📝 Ce Urmează După Conversie

După ce ai măștile PNG:

1. ✅ **Verifică calitatea** - deschide măștile și verifică că sunt corecte
2. ✅ **Continuă anotarea** - anotează restul pozelor (până la 200-500)
3. ✅ **Antrenare TFLite** - vezi `TFLITE_SEGMENTATION_GUIDE.md`

---

## 🎯 Structura Finală

După conversie, ar trebui să ai:

```
proiect_tflite/
├── images/
│   ├── 1.jpg
│   ├── 2.jpg
│   ├── 3.jpg
│   └── 4.jpg
└── masks/
    ├── 1.png  (mască albă pe negru)
    ├── 2.png
    ├── 3.png
    └── 4.png
```

**Important:** Numele trebuie să se potrivească! (`1.jpg` → `1.png`)

---

## ✅ Checklist

- [ ] JSON exportat din MakeSense.ai
- [ ] Pozele originale în directorul `images/`
- [ ] Scriptul `convert_coco_to_masks.py` în același director
- [ ] Librăriile instalate (`pillow`, `numpy`)
- [ ] Scriptul rulat cu succes
- [ ] Măștile verificate (alb pe negru)

---

**Succes! 🚀**














