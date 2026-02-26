## Restore guide (safe rollback)

You have **two layers of backup**:

1) **Git restore point (code + small assets)**  
- Use the `backup/2026-02-26` branch or the restore tag we push.

2) **Full ZIP snapshot (includes big folders like `opencv/`, ML models, etc.)**  
- Generated locally via `scripts/create_full_backup.ps1` and stored in `backup_files/`.

### Restore from Git (fast)

```powershell
git fetch --all --tags
git checkout backup/2026-02-26
```

If you want to force your local `master` back to that state:

```powershell
git checkout master
git reset --hard backup/2026-02-26
```

### Create a full ZIP backup (recommended before risky changes)

From repo root:

```powershell
.\scripts\create_full_backup.ps1
```

It will create a zip in `backup_files/` excluding only caches (`.git`, `.gradle`, `.idea`, build outputs).

### Restore from ZIP (full project)

- Unzip the archive into a new folder (recommended).
- Open that folder in Android Studio / Cursor.

