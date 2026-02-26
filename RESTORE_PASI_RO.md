## Backup / Restore – pași simpli (Română)

### 1) Unde îl vezi pe GitHub

1. Intră în repo: `https://github.com/andreilucianpascal-dot/hotwheels-collector`
2. Sus-stânga la **branch dropdown** (unde vezi `master`), acum proiectul este **la zi în `master`**.
3. Pentru “punct fix de restore” (checkpoint): intră la **Tags** și caută:
   - `restorepoint-2026-02-26`

### 2) Varianta recomandată: “restore point” + backup ZIP complet

Ai 2 niveluri:
- **GitHub (cod + fișiere normale)**: `master` + tag-ul `restorepoint-2026-02-26`
- **ZIP complet local** (include și foldere mari ignorate de git, ex: `opencv/`, modele): îl creezi cu scriptul.

### 3) Cum faci backup ZIP complet (înainte de schimbări riscante)

Din folderul proiectului:

```powershell
cd "C:\Users\Andrei\StudioProjects\hotwheels-collector"
.\scripts\create_full_backup.ps1
```

Îți va crea un fișier `.zip` în `backup_files\`.

### 4) Cum revii la “checkpoint” (restore din Git)

```powershell
cd "C:\Users\Andrei\StudioProjects\hotwheels-collector"
git fetch --all --tags
git checkout restorepoint-2026-02-26
```

Dacă vrei să forțezi `master` local să fie exact ca checkpoint-ul:

```powershell
git checkout master
git reset --hard restorepoint-2026-02-26
```

### 5) Cum refaci TOT proiectul (restore din ZIP)

1. Fă un folder nou, ex: `C:\Users\Andrei\StudioProjects\hotwheels-collector_restore`
2. Dezarchivează ZIP-ul în acel folder.
3. Deschide acel folder în Android Studio.

### 6) Cum îl deschizi în Android Studio

1. Android Studio → **File** → **Open**
2. Selectezi folderul proiectului (ex: `hotwheels-collector` sau folderul restaurat din ZIP)
3. Aștepți Gradle Sync (dacă îți cere)
4. Rulezi `app` (Run ▶)

