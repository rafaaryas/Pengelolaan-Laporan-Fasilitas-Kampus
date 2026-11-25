$ErrorActionPreference = 'Stop'
$envFile = Join-Path (Split-Path $PSScriptRoot -Parent) '.env'
if (-not (Test-Path -LiteralPath $envFile)) { throw 'File .env belum ada. Salin .env.example menjadi .env dan isi konfigurasi.' }
$requiredKeys = @('DB_NAME', 'DB_USERNAME', 'DB_PASSWORD')
$optionalKeys = @('DB_HOST', 'DB_PORT', 'SERVER_PORT', 'FLYWAY_LOCATIONS', 'DB_MIGRATION_USERNAME', 'DB_MIGRATION_PASSWORD', 'MYSQL_ROOT_PASSWORD')
$legacyKeys = @('DEMO_MAHASISWA_PASSWORD', 'DEMO_ADMIN_PASSWORD', 'DEMO_PETUGAS_PASSWORD')
foreach ($line in Get-Content -LiteralPath $envFile -Encoding UTF8) {
    if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) { continue }
    if ($line -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') { throw 'Format .env tidak valid. Gunakan KEY=value.' }
    $key = $Matches[1]
    $value = $Matches[2]
    if ($key -in $legacyKeys) { continue }
    if ($key -notin ($requiredKeys + $optionalKeys)) { throw "Variabel .env tidak didukung: $key" }
    if ($key -eq 'MYSQL_ROOT_PASSWORD' -and ($value -eq '' -or $value.StartsWith('CHANGE_ME'))) { continue }
    if ($value.StartsWith('CHANGE_ME')) { throw "Isi nilai lokal untuk $key di .env." }
    [Environment]::SetEnvironmentVariable($key, $value, 'Process')
}
foreach ($key in $requiredKeys) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key, 'Process'))) { throw "Variabel $key wajib diisi." }
}
