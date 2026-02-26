# Tipuri de Cartonașe și Machine Learning

## 📐 Tipurile de Cartonașe Hot Wheels

### 1. **Mainline**
- **Scurt**: ~108x108mm (pătrat)
- **Lung**: ~108x165mm (dreptunghi)

### 2. **Premium**
- Similar cu Mainline, dar dimensiuni ușor diferite
- **Team Transport**: Mai mare decât restul (~120x180mm)

### 3. **Silver Series**
- Similar cu Mainline Long (~108x165mm)

### 4. **TH/STH**
- Similar cu Mainline (scurt sau lung, depinde de serie)

---

## 🤖 Cum Funcționează Modelul TFLite?

### ✅ **Modelul Învață AUTOMAT Toate Formele**

**NU trebuie să faci cod special pentru fiecare tip!**

Modelul TFLite (UNet) va:
1. **Învăța** forma fiecărui tip de cartonaș din dataset
2. **Generaliza** și recunoaște forme similare (chiar dacă nu le-a văzut exact)
3. **Detecta** automat tipul corect bazat pe formă

### Exemplu:
- Dacă antrenezi cu 10 Mainline scurt + 10 Mainline lung + 10 Premium + 10 Silver Series
- Modelul va învăța **toate** formele
- Când vei face o poză nouă, modelul va detecta automat forma corectă (chiar dacă e ușor diferită)

---

## 📊 Strategie de Antrenare

### Opțiunea 1: **Un Singur Model pentru Toate** (Recomandat)

**Avantaje:**
- ✅ Un singur model (mai simplu)
- ✅ Modelul învață toate formele automat
- ✅ Generalizează bine pentru forme similare
- ✅ Nu trebuie să detectezi tipul înainte

**Cum funcționează:**
- Antrenezi modelul cu **toate tipurile** amestecate
- Modelul învață că "cartonaș scurt" = Mainline scurt
- Modelul învață că "cartonaș lung" = Mainline long / Silver Series
- Modelul învață că "cartonaș mare" = Team Transport

**Dataset:**
```
images/
├── 1.jpg (Mainline scurt)
├── 2.jpg (Mainline lung)
├── 3.jpg (Premium)
├── 4.jpg (Silver Series)
├── 5.jpg (Team Transport)
└── ...
```

**Rezultat:** Modelul va detecta automat forma corectă pentru orice tip!

---

### Opțiunea 2: **Modele Separate** (Opțional, Avansat)

**Când să folosești:**
- Dacă formele sunt **foarte diferite** (ex: cartonaș vs cutie)
- Dacă vrei precizie maximă pentru fiecare tip

**Dezavantaje:**
- ❌ Mai complex (trebuie să detectezi tipul înainte)
- ❌ Mai multe modele de gestionat
- ❌ Nu e necesar pentru Hot Wheels (formele sunt similare)

**NU recomand** pentru Hot Wheels - formele sunt suficient de similare!

---

## 🎯 Pentru Testul Tău (50 de Poze)

### Distribuție Recomandată:

**5 categorii × 10 poze = 50 poze total**

```
Mainline Scurt:     10 poze
Mainline Lung:      10 poze
Premium:            10 poze
Silver Series:      10 poze
Team Transport:     10 poze (sau alt tip mare)
```

**Sau dacă nu ai Team Transport:**
```
Mainline Scurt:     10 poze
Mainline Lung:      10 poze
Premium:            10 poze
Silver Series:      10 poze
TH/STH:             10 poze
```

### Important:
- ✅ **Varietate în fiecare categorie**: unghiuri diferite, iluminări diferite
- ✅ **Toate tipurile** în același dataset
- ✅ **Modelul va învăța** diferențele automat

---

## 💻 Cum Funcționează în Aplicație?

### După Antrenare:

**NU trebuie să detectezi tipul înainte!**

```kotlin
// ❌ NU face asta:
if (isMainline) {
    useMainlineModel()
} else if (isPremium) {
    usePremiumModel()
}

// ✅ FAI ASTA (un singur model pentru toate):
val mask = tfliteManager.segmentCard(photo)
// Modelul detectează automat forma corectă!
```

**Modelul va:**
1. Analiza poza
2. Detecta automat forma cartonașului (scurt/lung/mare)
3. Returna masca corectă pentru orice tip

---

## 🔬 De Ce Funcționează?

### UNet (Modelul de Segmentare)

UNet este un model de **segmentare semantică** care:
- **Învață** forme complexe din exemple
- **Generalizează** pentru forme similare (chiar dacă nu le-a văzut exact)
- **Nu are nevoie** de reguli hardcodate

**Exemplu:**
- Dacă antrenezi cu 10 Mainline scurt în unghiuri diferite
- Modelul va recunoaște **orice** Mainline scurt, chiar dacă:
  - E într-un unghi nou
  - E la o distanță diferită
  - Are iluminare diferită

---

## 📝 Rezumat

### ✅ Ce TREBUIE să Faci:

1. **Anotează toate tipurile** în același dataset
2. **Varietate în fiecare tip** (unghiuri, iluminări, distanțe)
3. **Antrenează un singur model** cu toate tipurile
4. **Modelul va învăța automat** toate formele

### ❌ Ce NU Trebuie să Faci:

1. ❌ **NU** creea modele separate pentru fiecare tip
2. ❌ **NU** detecta tipul înainte de segmentare
3. ❌ **NU** faci cod special pentru fiecare tip

### 🎯 Rezultat Final:

**Un singur model TFLite care detectează automat orice tip de cartonaș Hot Wheels!**

---

## 🚀 Următorii Pași

1. **Anotează toate cele 50 de poze** (toate tipurile amestecate)
2. **Antrenează modelul** cu toate tipurile
3. **Testează** pe poze noi (diferite tipuri)
4. **Modelul va funcționa** pentru toate tipurile automat!

---

**Concluzie:** Nu trebuie să te preocupi de tipuri diferite - modelul va învăța toate formele automat dacă le antrenezi corect! 🎯
















