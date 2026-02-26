# Cum Să Instalezi LabelMe (Open Source - Gratuit)

## 🚀 Metoda Simplă (Recomandat)

**NU trebuie să descarci manual de pe GitHub!**

LabelMe se instalează direct cu pip (package manager pentru Python).

### Pasul 1: Deschide Terminal/Command Prompt

**Windows:**
- Apasă `Win + R`
- Scrie `cmd` sau `powershell`
- Apasă Enter

**Sau:**
- Caută "Command Prompt" sau "PowerShell" în Start Menu

### Pasul 2: Instalează LabelMe

În terminal, scrie:

```bash
pip install labelme
```

**Așteaptă** până se termină instalarea (poate dura 1-2 minute).

### Pasul 3: Verifică Instalarea

```bash
labelme --version
```

Dacă vezi o versiune (ex: `5.2.0` sau `5.3.0`), e instalat corect! ✅

### Pasul 4: Deschide LabelMe

```bash
labelme
```

**LabelMe se va deschide cu interfața grafică!**

---

## 🔧 Dacă Ai Probleme

### Eroare: "pip nu este recunoscut"

**Soluție:** Python nu este în PATH.

1. Reinstalează Python și bifează "Add Python to PATH"
2. SAU adaugă manual Python la PATH

### Eroare: "Permission denied"

**Windows:**
```bash
# Încearcă cu --user
pip install --user labelme
```

### Eroare: "pip outdated"

**Actualizează pip:**
```bash
python -m pip install --upgrade pip
pip install labelme
```

---

## 📥 Dacă Vrei Să Descărci Manual de pe GitHub (Opțional)

**NU este necesar**, dar dacă vrei să vezi codul sursă:

### Opțiunea 1: Download ZIP

1. Deschide: https://github.com/wkentaro/labelme
2. Click pe butonul verde **"Code"**
3. Click pe **"Download ZIP"**
4. Extrage ZIP-ul
5. Deschide terminal în folderul extras
6. Rulează:
```bash
pip install -e .
```

### Opțiunea 2: Git Clone (dacă ai Git instalat)

```bash
git clone https://github.com/wkentaro/labelme.git
cd labelme
pip install -e .
```

**⚠️ Notă:** Metoda manuală este mai complexă și nu este necesară! Folosește `pip install labelme` - este mult mai simplu!

---

## ✅ Verificare Finală

După instalare, verifică că funcționează:

```bash
# Deschide LabelMe
labelme

# SAU deschide direct un folder cu poze
labelme C:\calea\către\dataset\images
```

**Dacă se deschide interfața grafică** → ✅ Succes!

---

## 🎯 Rezumat

**Metoda Simplă (Recomandat):**
```bash
pip install labelme
labelme
```

**NU trebuie să descarci manual de pe GitHub!** Pip face totul automat.

---

## 📚 Link-uri Utile

- **GitHub LabelMe**: https://github.com/wkentaro/labelme
- **Documentație**: https://github.com/wkentaro/labelme#installation

---

**Succes cu instalarea! 🚀**
















