# Cum Să Ieși Din Python și Să Instalezi LabelMe

## 🔍 Situația Ta

Ai deschis **Python interactiv** (promptul `>>>`). Asta înseamnă că Python este instalat corect! ✅

Dar pentru a instala LabelMe, trebuie să ieși din Python și să folosești **Command Prompt** normal.

---

## 🚪 Pasul 1: Ieși Din Python

În fereastra unde vezi `>>>`, scrie:

```python
exit()
```

Sau apasă:
- **Ctrl + Z** apoi **Enter** (Windows)
- **Ctrl + D** (alternativă)

**După ce ieși**, vei vedea din nou prompt-ul normal: `C:\Users\Andrei>`

---

## ✅ Pasul 2: Verifică Că Python Funcționează

În Command Prompt (NU în Python!), scrie:

```bash
python --version
```

Ar trebui să vezi: `Python 3.14.2` ✅

---

## 📦 Pasul 3: Verifică pip

```bash
pip --version
```

Ar trebui să vezi ceva de genul: `pip 24.x.x from ...` ✅

**Dacă vezi eroare** `'pip' is not recognized`:
- Încearcă: `python -m pip --version`
- SAU: `py -m pip --version`

---

## 🎨 Pasul 4: Instalează LabelMe

Dacă pip funcționează, scrie:

```bash
pip install labelme
```

**Sau dacă pip nu funcționează direct:**

```bash
python -m pip install labelme
```

**Sau:**

```bash
py -m pip install labelme
```

**Așteaptă** 1-2 minute până se termină instalarea.

---

## ✅ Pasul 5: Verifică LabelMe

```bash
labelme --version
```

**Sau:**

```bash
python -m labelme --version
```

Dacă vezi o versiune (ex: `5.2.0`) → ✅ Succes!

---

## 🚀 Pasul 6: Deschide LabelMe

```bash
labelme
```

**Sau dacă vrei să deschizi direct folderul cu poze:**

```bash
labelme C:\calea\către\dataset\images
```

**LabelMe se va deschide cu interfața grafică!**

---

## 📝 Rezumat - Pașii Compleți

1. **Ieși din Python:**
   ```python
   exit()
   ```

2. **Verifică Python:**
   ```bash
   python --version
   ```

3. **Verifică pip:**
   ```bash
   pip --version
   # SAU
   python -m pip --version
   ```

4. **Instalează LabelMe:**
   ```bash
   pip install labelme
   # SAU
   python -m pip install labelme
   ```

5. **Deschide LabelMe:**
   ```bash
   labelme
   ```

---

## ⚠️ Diferența Importantă

- **Python interactiv** (`>>>`): Pentru a scrie cod Python direct
- **Command Prompt** (`C:\Users\Andrei>`): Pentru a rula comenzi (pip, labelme, etc.)

**Pentru instalare, ai nevoie de Command Prompt, NU Python interactiv!**

---

**Succes! 🚀**
















