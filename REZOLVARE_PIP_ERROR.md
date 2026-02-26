# Rezolvare: 'pip' is not recognized

## 🔍 Problema

Eroarea `'pip' is not recognized` înseamnă că:
- ❌ Python nu este instalat SAU
- ❌ Python nu este în PATH (sistemul nu știe unde este Python)

---

## ✅ Soluție 1: Verifică Dacă Python Este Instalat

În Command Prompt, scrie:

```bash
python --version
```

**Dacă vezi o versiune** (ex: `Python 3.11.0`):
- ✅ Python este instalat
- Problema este că nu este în PATH

**Dacă vezi eroare** `'python' is not recognized`:
- ❌ Python NU este instalat
- Trebuie să instalezi Python mai întâi

---

## 📥 Soluție 2: Instalează Python (Dacă Nu Este Instalat)

### Pasul 1: Download Python

1. Deschide: https://www.python.org/downloads/
2. Click pe butonul mare **"Download Python 3.x.x"** (cea mai nouă versiune)
3. Descarcă instalatorul (ex: `python-3.12.0-amd64.exe`)

### Pasul 2: Instalează Python

**⚠️ FOARTE IMPORTANT:**

1. Rulează instalatorul descărcat
2. **Bifează** ✅ **"Add Python to PATH"** (foarte important!)
3. Click "Install Now"
4. Așteaptă să se termine instalarea

### Pasul 3: Verifică Instalarea

**Închide și redeschide Command Prompt** (important!)

Apoi scrie:
```bash
python --version
pip --version
```

Dacă vezi versiuni pentru ambele → ✅ Succes!

---

## 🔧 Soluție 3: Python Este Instalat Dar Nu Este în PATH

### Verifică Unde Este Python

În Command Prompt, scrie:
```bash
where python
```

Sau:
```bash
where py
```

**Dacă vezi o cale** (ex: `C:\Users\Andrei\AppData\Local\Programs\Python\Python312\python.exe`):
- Python este instalat, dar nu este în PATH

### Adaugă Python la PATH Manual

1. **Caută calea Python:**
   - De obicei: `C:\Users\[NUME]\AppData\Local\Programs\Python\Python3xx\`
   - Sau: `C:\Python3xx\`

2. **Adaugă la PATH:**
   - Apasă `Win + R`
   - Scrie `sysdm.cpl` → Enter
   - Tab "Advanced" → "Environment Variables"
   - În "System variables", găsește "Path" → Edit
   - Click "New" → adaugă calea Python (ex: `C:\Python312\`)
   - Click "New" → adaugă calea Scripts (ex: `C:\Python312\Scripts\`)
   - Click OK pe toate ferestrele

3. **Închide și redeschide Command Prompt**

4. **Verifică:**
```bash
python --version
pip --version
```

---

## 🚀 Soluție 4: Folosește `py` în Loc de `python`

Pe Windows, poți folosi `py` launcher:

```bash
py --version
py -m pip --version
py -m pip install labelme
```

**Dacă funcționează**, folosește `py -m pip` în loc de `pip`!

---

## 🎯 Soluție 5: Reinstalează Python cu PATH

**Cel mai simplu:**

1. **Dezinstalează Python** (dacă este instalat):
   - Settings → Apps → Python → Uninstall

2. **Reinstalează Python:**
   - Download de pe https://www.python.org/downloads/
   - **Bifează** ✅ **"Add Python to PATH"** (foarte important!)
   - Install Now

3. **Închide și redeschide Command Prompt**

4. **Verifică:**
```bash
python --version
pip --version
```

---

## ✅ Verificare Finală

După ce ai rezolvat, verifică:

```bash
# Verifică Python
python --version

# Verifică pip
pip --version

# Instalează LabelMe
pip install labelme

# Verifică LabelMe
labelme --version
```

**Dacă toate funcționează** → ✅ Gata!

---

## 🆘 Dacă Tot Nu Funcționează

### Alternative:

1. **Folosește MakeSense.ai** (nu necesită instalare):
   - https://www.makesense.ai/
   - Rulează direct în browser
   - 100% gratuit

2. **Folosește Anaconda** (include Python + pip):
   - Download: https://www.anaconda.com/download
   - Instalează Anaconda
   - Deschide "Anaconda Prompt"
   - Rulează: `pip install labelme`

---

## 📝 Rezumat

**Cel mai simplu:**
1. Download Python de pe https://www.python.org/downloads/
2. **Bifează** ✅ **"Add Python to PATH"**
3. Install Now
4. Închide și redeschide Command Prompt
5. `pip install labelme`

**Succes! 🚀**
















