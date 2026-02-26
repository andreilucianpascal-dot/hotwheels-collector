# 🐍 Instalare Python 3.12.12 - Ghid Pas cu Pas

## 📋 Pasul 1: Descarcă Python 3.12.12

1. **Deschide browser-ul** și mergi la:
   ```
   https://www.python.org/downloads/release/python-31212/
   ```

2. **Scroll down** până la "Files" section

3. **Găsește și click pe:**
   ```
   Windows installer (64-bit)
   ```
   (Fișierul se numește: `python-3.12.12-amd64.exe`)

4. **Descarcă** fișierul (va fi ~25-30 MB)

## 📥 Pasul 2: Instalează Python 3.12.12

1. **Deschide fișierul descărcat** (`python-3.12.12-amd64.exe`)

2. **IMPORTANT - Bifează:**
   ✅ **"Add Python 3.12 to PATH"** (foarte important!)
   
   ⚠️ **NU bifa "Install for all users"** (dacă nu ești admin)

3. **Click pe "Install Now"**

4. **Așteaptă** instalarea (1-2 minute)

5. **Când apare "Setup was successful"** → Click "Close"

## ✅ Pasul 3: Verifică Instalarea

Deschide **PowerShell** sau **Command Prompt** și rulează:

```powershell
py -3.12 --version
```

**Ar trebui să vezi:**
```
Python 3.12.12
```

Dacă vezi eroarea "Python 3.12 was not found", înseamnă că nu ai bifat "Add Python 3.12 to PATH". 
**Soluție:** Reinstalează Python 3.12.12 și bifează "Add Python 3.12 to PATH".

## 🔍 Pasul 4: Verifică Că Ambele Versiuni Funcționează

```powershell
# Verifică Python 3.14 (vechiul)
py --version
# Ar trebui să vezi: Python 3.14.2

# Verifică Python 3.12 (nou)
py -3.12 --version
# Ar trebui să vezi: Python 3.12.12
```

## 📦 Pasul 5: Instalează TensorFlow cu Python 3.12

```powershell
py -3.12 -m pip install --upgrade pip
py -3.12 -m pip install tensorflow numpy pillow scikit-learn
```

**Timp estimat:** 5-10 minute (depinde de internet)

## ✅ Pasul 6: Verifică Instalarea TensorFlow

```powershell
py -3.12 -c "import tensorflow as tf; print('TensorFlow version:', tf.__version__)"
```

**Ar trebui să vezi:**
```
TensorFlow version: 2.15.0
```
(sau o versiune similară)

## 🎯 Pasul 7: Rulează Scriptul de Antrenare

```powershell
cd C:\Users\Andrei\Desktop\test_tflite
py -3.12 train_tflite_4_masks.py
```

## ⚠️ Probleme Comune

### Problema 1: "Python 3.12 was not found"
**Soluție:** Reinstalează Python 3.12.12 și bifează "Add Python 3.12 to PATH"

### Problema 2: "pip is not recognized"
**Soluție:** Folosește `py -3.12 -m pip` în loc de `pip`

### Problema 3: "Permission denied"
**Soluție:** Rulează PowerShell ca Administrator

## 📝 Rezumat

✅ **Python 3.14** rămâne instalat (pentru alte lucruri)
✅ **Python 3.12.12** este instalat (pentru TensorFlow)
✅ Folosești `py -3.12` pentru TensorFlow
✅ Folosești `py` (sau `py -3.14`) pentru alte lucruri

## 🎉 Gata!

Acum ai ambele versiuni și poți folosi Python 3.12 pentru TensorFlow!














