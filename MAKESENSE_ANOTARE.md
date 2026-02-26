# Cum Să Anotezi în MakeSense.ai

## 🎯 Ce Să Selectezi

### ✅ DA: Object Detection (cu poligoane!)

**MakeSense.ai NU are "Instance Segmentation"**, dar Object Detection suportă **poligoane**!

**Selectează: "Object Detection"**

**Important:** După ce selectezi Object Detection, vei putea alege între:
- **Rectangle** (dreptunghi) ❌ - NU folosi asta
- **Polygon** (poligon) ✅ - **FOLOSEȘTE ASTA!**

---

## 📝 Pașii în MakeSense.ai

### Pasul 1: Selectează Tipul de Anotare

După ce ai uploadat pozele, vei vedea:
- **Object Detection** ✅ (Selectează asta!)
- **Image Recognition** ❌ (NU asta)

**Click pe "Object Detection"**

### Pasul 1.5: Selectează Tool-ul de Desenare

După ce ai selectat Object Detection, vei vedea opțiuni pentru tool:
- **Rectangle** ❌ (NU - doar dreptunghiuri)
- **Polygon** ✅ (DA - poligoane precise!)

**Selectează "Polygon"** (sau "Draw Polygon")

---

### Pasul 2: Creează Clasa

1. **Click pe "Add Class"** sau "Create Class"
2. **Numele clasei:** `card` sau `hotwheels_card` sau `cartonas`
3. **Click "Save"**

---

### Pasul 3: Anotează Prima Imagine

1. **Click pe prima poză** din listă

2. **Asigură-te că ai selectat "Polygon"** (nu Rectangle!)

3. **Click pe "Add Polygon"** sau "Draw Polygon" sau butonul cu poligon

4. **Desenează conturul cartonașului:**
   - Click pe fiecare colț/punct important
   - Urmează **exact** marginea cartonașului
   - Pentru colțuri tăiate, fă click-uri mai dese
   - Când ai terminat, click dreapta sau **Enter** pentru a închide poligonul

5. **Selectează clasa:**
   - După ce ai desenat poligonul, selectează clasa `card` (sau cum ai numit-o)

6. **Click "Save"** sau "Next" pentru a trece la următoarea poză

---

### Pasul 4: Repetă pentru Toate Pozele

Repetă procesul pentru toate cele 48 de poze:
- Click pe poză
- Add Polygon
- Desenează conturul
- Selectează clasa
- Save
- Next

---

### Pasul 5: Export Măști

După ce ai anotat toate pozele:

1. **Click pe "Export"** (buton în partea de sus)

2. **Selectează format:**
   - **"COCO JSON"** ✅ (RECOMANDAT!)
   - **"VGG JSON"** (alternativă, dar mai puțin standardizat)
   
   **👉 Selectează "COCO JSON"** pentru că:
   - Este format standard pentru ML
   - Are mai multe tool-uri de conversie disponibile
   - Este mai ușor de convertit la PNG masks
   - Este mai bine documentat

3. **Download** fișierul JSON

4. **Convert JSON la PNG masks:**
   - MakeSense.ai exportă JSON cu poligoane, nu direct PNG masks
   - Va trebui să convertești JSON-ul la PNG (vezi script în `TFLITE_SEGMENTATION_GUIDE.md`)

**⚠️ Notă:** MakeSense.ai exportă JSON, nu PNG direct. Va trebui să convertești mai târziu folosind un script Python.

---

## ⚠️ Tips Importante

### Precizie
- ✅ Urmează **exact** marginea cartonașului
- ✅ Pentru colțuri tăiate, fă click-uri mai dese
- ✅ Nu include fundalul în poligon
- ❌ NU face poligonul prea mare sau prea mic

### Viteză
- După ce salvezi, MakeSense trece automat la următoarea poză
- Poți folosi săgeți pentru navigare între poze
- Poți zoom (scroll mouse) pentru precizie mai bună

---

## ✅ Verificare Finală

După export, verifică că:
- ✅ Ai 48 de măști PNG
- ✅ Numele se potrivesc cu pozele (1.jpg → 1.png)
- ✅ Măștile sunt corecte (fundal negru, cartonaș alb)

---

## 🎯 Rezumat

1. **Selectează: "Object Detection"** cu **"Polygon"** tool ✅
2. **Creează clasa:** `card`
3. **Anotează:** Desenează poligon pe fiecare poză
4. **Export:** **COCO JSON format** ✅
5. **Convert:** JSON → PNG masks (cu script Python)
6. **Verifică:** Măștile sunt corecte

---

**Succes cu anotarea! 🚀**

