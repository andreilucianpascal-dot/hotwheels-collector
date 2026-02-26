param(
  [string]$ProjectRoot = (Resolve-Path ".").Path,
  [string]$OutDir = (Join-Path (Resolve-Path ".").Path "backup_files"),
  [string]$Name = ("full_backup_" + (Get-Date -Format "yyyy-MM-dd_HH-mm-ss") + ".zip")
)

$ErrorActionPreference = "Stop"

if (!(Test-Path $OutDir)) {
  New-Item -ItemType Directory -Path $OutDir | Out-Null
}

$zipPath = Join-Path $OutDir $Name
if (Test-Path $zipPath) {
  Remove-Item -Force $zipPath
}

Write-Host "Creating backup zip..."
Write-Host "  ProjectRoot: $ProjectRoot"
Write-Host "  OutDir     : $OutDir"
Write-Host "  Zip        : $zipPath"

# Exclude folders that are either caches or regenerated (keep your real assets like opencv/, models, etc.)
$excludeDirs = @(
  ".git",
  ".gradle",
  ".idea",
  "build",
  "app\build",
  ".cxx",
  ".externalNativeBuild"
)

$excludeFullPaths = $excludeDirs | ForEach-Object { Join-Path $ProjectRoot $_ }

$allFiles = Get-ChildItem -Path $ProjectRoot -Recurse -File -Force | Where-Object {
  $full = $_.FullName
  foreach ($ex in $excludeFullPaths) {
    if ($full.StartsWith($ex, [System.StringComparison]::OrdinalIgnoreCase)) { return $false }
  }
  return $true
}

if ($allFiles.Count -eq 0) {
  throw "No files found to backup (after excludes)."
}

Write-Host ("Files to include: " + $allFiles.Count)

# Compress-Archive can be slow for huge projects; still the simplest reliable local backup.
Compress-Archive -Path $allFiles.FullName -DestinationPath $zipPath -CompressionLevel Optimal

Write-Host "Done."
Write-Host "Backup created at: $zipPath"

