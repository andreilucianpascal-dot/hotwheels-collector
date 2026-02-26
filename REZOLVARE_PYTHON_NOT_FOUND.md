# Rezolvare: Python was not found

## 🔍 Problema

Windows nu găsește Python în PATH, deși probabil este instalat (ai văzut promptul `>>>`).

---

## ✅ Soluție 1: Folosește `py` Launcher (Cel Mai Simplu!)

Pe Windows, există un launcher `py` care funcționează de obicei:

```bash
py --version
```

**Dacă funcționează**, folosește `py` în loc de `python`:

```bash
# Verifică pip
py -m pip --version

# Instalează LabelMe
py -m pip install labelme

# Deschide LabelMe
py -m labelme
```

---

## 🔍 Soluție 2: Găsește Unde Este Python

### Verifică Dacă Python Este Instalat

În Command Prompt, scrie:

```bash
where py
```

**Sau:**

```bash
where python
```

**Sau caută manual:**
- `C:\Users\Andrei\AppData\Local\Programs\Python\Python3xx\`
- `C:\Python3xx\`
- `C:\Program Files\Python3xx\`

### Dacă Găsești Calea

Folosește calea completă:

```bash
# Înlocuiește cu calea ta reală
"C:\Users\Andrei\AppData\Local\Programs\Python\Python314\python.exe" -m pip --version
"C:\Users\Andrei\AppData\Local\Programs\Python\Python314\python.exe" -m pip install labelme
```

---

## 🔧 Soluție 3: Reinstalează Python cu PATH

**Cel mai sigur:**

1. **Dezinstalează Python** (dacă este instalat):
   - Settings → Apps → Caută "Python" → Uninstall

2. **Descarcă Python din nou:**
   - https://www.python.org/downloads/
   - Download Python 3.14.2 (sau cea mai nouă versiune)

3. **Instalează Python:**
   - Rulează instalatorul
   - **⚠️ FOARTE IMPORTANT:** Bifează ✅ **"Add Python to PATH"**
   - Click "Install Now"
   - Așteaptă să se termine

4. **Închide și redeschide Command Prompt**

5. **Verifică:**
```bash
python --version
pip --version
```

---

## 🎯 Soluție 4: Adaugă Python la PATH Manual

### Găsește Calea Python

1. **Deschide File Explorer**
2. **Navighează la:**
   - `C:\Users\Andrei\AppData\Local\Programs\Python\`
   - SAU `C:\Python3xx\`
   - SAU `C:\Program Files\Python3xx\`

3. **Intră în folderul Python** (ex: `Python314`)
4. **Copiază calea completă** (ex: `C:\Users\Andrei\AppData\Local\Programs\Python\Python314`)

### Adaugă la PATH

1. **Apasă `Win + R`**
2. **Scrie:** `sysdm.cpl` → Enter
3. **Tab "Advanced"** → **"Environment Variables"**
4. **În "System variables"**, găsește **"Path"** → **Edit**
5. **Click "New"** → adaugă calea Python:
   - `C:\Users\Andrei\AppData\Local\Programs\Python\Python314`
6. **Click "New"** → adaugă calea Scripts:
   - `C:\Users\Andrei\AppData\Local\Programs\Python\Python314\Scripts`
7. **Click OK** pe toate ferestrele

8. **Închide și redeschide Command Prompt**

9. **Verifică:**
```bash
python --version
pip --version
```

---

## 🚀 Soluție 5: Folosește Python Direct din Folder

Dacă găsești folderul Python, poți folosi calea completă:

```bash
# Exemplu (înlocuiește cu calea ta reală)
cd C:\Users\Andrei\AppData\Local\Programs\Python\Python314
python.exe -m pip install labelme
python.exe -m labelme
```

---

## ✅ Verificare Rapidă

Încearcă în ordine:

1. **`py --version`** (cel mai probabil va funcționa)
2. **`where py`** (găsește calea)
3. **Caută manual în File Explorer** (AppData\Local\Programs\Python)

---

## 🎯 Rezumat - Ce Să Faci Acum

**Încearcă mai întâi:**

```bash
py --version
py -m pip --version
py -m pip install labelme
py -m labelme
```

**Dacă `py` funcționează** → ✅ Gata! Folosește `py` în loc de `python`.

**Dacă `py` NU funcționează** → Reinstalează Python cu "Add to PATH" bifat.

---

**Spune-mi ce funcționează! 🚀**
















