# Verificare: Python Este Instalat Corect?

## 🔍 Pasul 1: Verifică Dacă Python Este Instalat

### Caută Folderul Python

1. **Deschide File Explorer**
2. **Navighează la:**
   - `C:\Users\Andrei\AppData\Local\Programs\Python\`
   - SAU `C:\Python3xx\`
   - SAU `C:\Program Files\Python3xx\`

3. **Dacă găsești un folder Python** (ex: `Python314`):
   - ✅ Python este instalat
   - Problema este că nu este în PATH

4. **Dacă NU găsești folderul:**
   - ❌ Python NU este instalat corect
   - Trebuie să-l instalezi din nou

---

## ✅ Pasul 2: Dacă Python Este Instalat (Găsești Folderul)

### Folosește Calea Completă

Dacă ai găsit folderul Python (ex: `C:\Users\Andrei\AppData\Local\Programs\Python\Python314`):

```bash
# Verifică Python
"C:\Users\Andrei\AppData\Local\Programs\Python\Python314\python.exe" --version

# Instalează LabelMe
"C:\Users\Andrei\AppData\Local\Programs\Python\Python314\python.exe" -m pip install labelme

# Deschide LabelMe
"C:\Users\Andrei\AppData\Local\Programs\Python\Python314\python.exe" -m labelme
```

**⚠️ Înlocuiește calea cu calea ta reală!**

---

## 🔧 Pasul 3: Adaugă Python la PATH

### Găsește Calea Python

1. **Deschide File Explorer**
2. **Navighează la folderul Python** (ex: `C:\Users\Andrei\AppData\Local\Programs\Python\Python314`)
3. **Copiază calea completă** (click dreapta pe bara de adresă → Copy address as text)

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

8. **Închide și redeschide Command Prompt** (foarte important!)

9. **Verifică:**
```bash
python --version
pip --version
```

---

## 🚀 Pasul 4: Încearcă `py` Launcher

Înainte de a adăuga la PATH, încearcă:

```bash
py --version
```

**Dacă funcționează**, folosește `py` pentru tot:

```bash
py -m pip install labelme
py -m labelme
```

---

## 📥 Pasul 5: Dacă Python NU Este Instalat

### Reinstalează Python Corect

1. **Download Python:**
   - https://www.python.org/downloads/
   - **NU** instala din Microsoft Store!

2. **Rulează instalatorul** descărcat

3. **⚠️ FOARTE IMPORTANT:**
   - **Bifează** ✅ **"Add Python to PATH"**
   - **Bifează** ✅ **"Install for all users"** (opțional, dar recomandat)

4. **Click "Install Now"**

5. **Așteaptă** să se termine instalarea

6. **Închide și redeschide Command Prompt**

7. **Verifică:**
```bash
python --version
pip --version
```

---

## 🎯 Rezumat - Ce Să Faci Acum

### 1. Verifică Dacă Python Este Instalat

Caută în:
- `C:\Users\Andrei\AppData\Local\Programs\Python\`
- `C:\Python3xx\`
- `C:\Program Files\Python3xx\`

### 2. Dacă Găsești Python:

**Opțiunea A:** Folosește calea completă
```bash
"C:\calea\completă\python.exe" -m pip install labelme
```

**Opțiunea B:** Adaugă Python la PATH (vezi Pasul 3)

**Opțiunea C:** Folosește `py` launcher
```bash
py -m pip install labelme
```

### 3. Dacă NU Găsești Python:

Reinstalează Python cu "Add to PATH" bifat (vezi Pasul 5)

---

## ✅ Verificare Finală

După ce ai rezolvat, verifică:

```bash
# Verifică Python
python --version
# SAU
py --version

# Verifică pip
pip --version
# SAU
py -m pip --version

# Instalează LabelMe
pip install labelme
# SAU
py -m pip install labelme
# SAU
"C:\calea\completă\python.exe" -m pip install labelme
```

---

**Spune-mi ce găsești când cauți folderul Python! 🚀**
















